from __future__ import annotations

import os

import stat
import subprocess
import tempfile
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
SCRIPT = ROOT / "tools/scripts/ci/gradle-retry.sh"
ACTION_SCRIPT = ROOT / ".github/actions/gradle-retry/gradle-retry.sh"


class GradleRetryTests(unittest.TestCase):
    def run_case(
        self,
        mode: str,
        *,
        max_attempts: str = "3",
        persistent: bool = False,
        script: Path = SCRIPT,
        action_runtime: bool = False,
    ) -> tuple[subprocess.CompletedProcess[str], int]:
        with tempfile.TemporaryDirectory(prefix="q4j-gradle-retry-") as temporary:
            root = Path(temporary)
            tmpdir = root / "tmp"
            tmpdir.mkdir()
            fake_gradle = root / "gradlew"
            count = root / "count"
            fake_gradle.write_text(
                "#!/usr/bin/env bash\n"
                "[[ \"$1\" == --task && \"$2\" == test ]] || { echo \"unexpected args: $*\" >&2; exit 99; }\n"
                "n=0; [[ -f \"$COUNT_FILE\" ]] && n=$(<\"$COUNT_FILE\")\n"
                "n=$((n + 1)); printf '%s' \"$n\" > \"$COUNT_FILE\"\n"
                "case \"$MODE\" in\n"
                "  ordinary) echo 'compilation failed'; exit 9;;\n"
                "  not-found) if [[ \"$*\" == *--refresh-dependencies* ]]; then [[ \"${PERSISTENT:-0}\" == 1 ]] && { echo 'Could not find org.example:missing:1.0.'; echo 'Searched in the following locations:'; exit 7; }; echo success; exit 0; fi; echo 'Could not find org.example:missing:1.0.'; echo 'Searched in the following locations:'; exit 7;;\n"
                "  rate-limit) [[ $n -lt 2 ]] && { echo 'Could not GET https://repo.maven.apache.org/maven2/example.pom'; echo 'Received status code 429 from server: Too Many Requests'; exit 8; }; echo success; exit 0;;\n"
                "  forbidden) [[ $n -lt 2 ]] && { echo \"Could not GET 'https://repo.maven.apache.org/maven2/example.pom'. Received status code 403 from server: Forbidden\"; exit 13; }; echo success; exit 0;;\n"
                "  persistent-forbidden) echo \"Could not GET 'https://repo.maven.apache.org/maven2/example.pom'. Received status code 403 from server: Forbidden\"; exit 13;;\n"
                "  unrelated-forbidden) echo 'Could not resolve org.example:missing:1.0'; echo 'Task failed: Forbidden API operation'; exit 7;;\n"
                "  server-error) echo 'Could not GET https://repo.maven.apache.org/maven2/example.pom'; echo 'Received status code 500 from server'; exit 14;;\n"
                "esac\n"
            )
            fake_gradle.chmod(fake_gradle.stat().st_mode | stat.S_IXUSR)
            env = {
                **os.environ,
                "COUNT_FILE": str(count),
                "GRADLE_RETRY_COMMAND": f"{fake_gradle} --task test",
                "GRADLE_RETRY_INITIAL_DELAY_SECONDS": "1",
                "GRADLE_RETRY_MAX_ATTEMPTS": max_attempts,
                "TMPDIR": str(tmpdir),
                "MODE": mode,
            }
            if mode == "not-found" and persistent:
                env["PERSISTENT"] = "1"
            command = ["bash", str(script)]
            if action_runtime:
                env["GITHUB_ACTION_PATH"] = str(ACTION_SCRIPT.parent)
                command = ["bash", "-c", 'bash "$GITHUB_ACTION_PATH/gradle-retry.sh"']
            result = subprocess.run(
                command,
                cwd=root,
                env=env,
                text=True,
                capture_output=True,
                check=False,
            )
            self.assertEqual(list(tmpdir.iterdir()), [], "retry temporary output was not cleaned up")
            return result, int(count.read_text())

    def test_existing_429_retry_is_preserved(self) -> None:
        result, calls = self.run_case("rate-limit", max_attempts="2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)

    def test_500_and_ordinary_failures_are_not_retried(self) -> None:
        for mode, status in (("server-error", 14), ("ordinary", 9)):
            with self.subTest(mode=mode):
                result, calls = self.run_case(mode)
                self.assertEqual(result.returncode, status)
                self.assertEqual(calls, 1)

    def test_transient_403_retries_and_recovers(self) -> None:
        result, calls = self.run_case("forbidden", max_attempts="2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)
        self.assertIn("repository 403", result.stdout)

    def test_composite_action_ships_and_executes_its_declared_script(self) -> None:
        result, calls = self.run_case("forbidden", max_attempts="2", action_runtime=True)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)

    def test_persistent_403_fails_after_limit(self) -> None:
        result, calls = self.run_case("persistent-forbidden", max_attempts="2")
        self.assertEqual(result.returncode, 13)
        self.assertEqual(calls, 2)
        self.assertIn("repository 403", result.stdout)
        self.assertIn("retry limit exhausted", result.stderr)

    def test_unrelated_forbidden_text_is_not_retried(self) -> None:
        result, calls = self.run_case("unrelated-forbidden")
        self.assertEqual(result.returncode, 7)
        self.assertEqual(calls, 1)
        self.assertNotIn("repository 403", result.stdout)

    def test_not_found_gets_exactly_one_refresh_retry(self) -> None:
        result, calls = self.run_case("not-found", max_attempts="2")
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual(calls, 2)
        self.assertIn("--refresh-dependencies", result.stdout)

    def test_persistent_not_found_fails_after_refresh(self) -> None:
        result, calls = self.run_case("not-found", max_attempts="2", persistent=True)
        self.assertEqual(result.returncode, 7)
        self.assertEqual(calls, 2)

    def test_500_is_not_retried(self) -> None:
        result, calls = self.run_case("server-error")
        self.assertEqual(result.returncode, 14)
        self.assertEqual(calls, 1)


if __name__ == "__main__":
    unittest.main()
