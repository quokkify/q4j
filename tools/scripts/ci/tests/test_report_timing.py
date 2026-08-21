import json
import tempfile
import unittest
from unittest.mock import patch
from pathlib import Path

import report_timing


class TimingReportTest(unittest.TestCase):
    def fixture(self):
        return {
            "run": {
                "id": 42,
                "html_url": "https://github.com/quokkify/q4j/actions/runs/42",
                "head_sha": "abc123",
                "run_started_at": "2026-01-01T00:00:00Z",
                "updated_at": "2026-01-01T00:00:20Z",
            },
            "jobs": [
                {"id": 2, "name": "Tests • beta", "status": "completed", "conclusion": "success", "queued_at": "2026-01-01T00:00:01Z", "started_at": "2026-01-01T00:00:05Z", "completed_at": "2026-01-01T00:00:20Z"},
                {"id": 1, "name": "Build • alpha", "status": "completed", "conclusion": "failure", "queued_at": "2026-01-01T00:00:01Z", "started_at": "2026-01-01T00:00:02Z", "completed_at": "2026-01-01T00:00:12Z"},
                {"id": 3, "name": "Lint", "status": "in_progress", "conclusion": None, "queued_at": "2026-01-01T00:00:01Z", "started_at": "2026-01-01T00:00:03Z", "completed_at": None},
            ],
        }

    def test_aggregates_runtime_queue_and_stable_longest_sort(self):
        result = report_timing.collect(self.fixture())
        self.assertEqual(result["wall_clock"], 20)
        self.assertEqual(result["job_runtime"], 25)
        self.assertEqual(result["longest"]["name"], "beta")
        self.assertEqual(result["jobs"][0]["stage"], "Tests")
        self.assertEqual(result["jobs"][0]["queue"], 4)
        self.assertEqual(result["jobs"][2]["duration"], None)

    def test_markdown_contains_summary_and_unfinished_status(self):
        rendered = report_timing.markdown(report_timing.collect(self.fixture()))
        self.assertIn(report_timing.MARKER, rendered)
        self.assertIn("Total wall-clock: **0m 20s**", rendered)
        self.assertIn("| Lint / Lint | in_progress |", rendered)
        self.assertIn("**Tests / beta**", rendered)

    def test_empty_payload_is_valid_markdown(self):
        rendered = report_timing.markdown(report_timing.collect({"run": {}, "jobs": []}))
        self.assertIn("| — | no jobs returned |", rendered)
        self.assertIn("Jobs: **0**", rendered)

    def test_unstarted_job_keeps_placeholder_without_parsing(self):
        job = report_timing.normalise_job({"id": 9, "name": "Queued", "status": "queued", "started_at": None})
        self.assertEqual(job["started"], "—")
        self.assertEqual(report_timing.format_started(job["started"]), "—")

    @patch.object(report_timing, "urlopen")
    @patch.object(report_timing, "api_json")
    def test_update_paginates_and_patches_owned_marker_endpoint(self, api_json, urlopen):
        api_json.side_effect = [
            [{"id": index, "body": "unrelated", "user": {"login": "someone"}} for index in range(100)],
            [{"id": 123, "body": report_timing.MARKER, "user": {"login": "github-actions[bot]"}}],
        ]
        report_timing.update_comment("quokkify/q4j", 501, "new body", "token")
        self.assertEqual(api_json.call_count, 2)
        self.assertIn("page=1", api_json.call_args_list[0].args[0])
        self.assertIn("page=2", api_json.call_args_list[1].args[0])
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "https://api.github.com/repos/quokkify/q4j/issues/comments/123")
        self.assertEqual(request.method, "PATCH")

    @patch.object(report_timing, "urlopen")
    @patch.object(report_timing, "api_json")
    def test_post_ignores_unowned_marker(self, api_json, urlopen):
        api_json.return_value = [{"id": 123, "body": report_timing.MARKER, "user": {"login": "human"}}]
        report_timing.update_comment("quokkify/q4j", 501, "new body", "token")
        request = urlopen.call_args.args[0]
        self.assertEqual(request.full_url, "https://api.github.com/repos/quokkify/q4j/issues/501/comments")
        self.assertEqual(request.method, "POST")

    @patch.object(report_timing, "api_json")
    def test_duplicate_owned_markers_are_rejected(self, api_json):
        api_json.return_value = [
            {"id": 1, "body": report_timing.MARKER, "user": {"login": "github-actions[bot]"}},
            {"id": 2, "body": report_timing.MARKER, "user": {"login": "github-actions[bot]"}},
        ]
        with self.assertRaisesRegex(RuntimeError, "at most one"):
            report_timing.update_comment("quokkify/q4j", 501, "new body", "token")

    def test_offline_cli_writes_fixture(self):
        with tempfile.TemporaryDirectory() as directory:
            fixture = Path(directory) / "fixture.json"
            output = Path(directory) / "report.md"
            fixture.write_text(json.dumps(self.fixture()))
            self.assertEqual(report_timing.main.__module__, "report_timing")
            args = report_timing.argparse.Namespace(input=fixture, output=output, repo=None, run_id=None, pr=None, comment=False)
            result = report_timing.collect(report_timing.load_payload(args))
            output.write_text(report_timing.markdown(result))
            self.assertIn("abc123", output.read_text())


if __name__ == "__main__":
    unittest.main()
