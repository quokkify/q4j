#!/usr/bin/env python3
"""Render GitHub Actions job timing and optionally maintain a PR comment."""
from __future__ import annotations

import argparse
import json
import os
import sys
from datetime import datetime, timezone
from pathlib import Path
from typing import Any
from urllib.request import Request, urlopen

MARKER = "<!-- q4j-ci-timing -->"


def parse_time(value: str | None) -> datetime | None:
    if not value:
        return None
    return datetime.fromisoformat(value.replace("Z", "+00:00"))


def seconds(start: str | None, end: str | None) -> float | None:
    first, last = parse_time(start), parse_time(end)
    if first is None or last is None or last < first:
        return None
    return (last - first).total_seconds()


def normalise_job(job: dict[str, Any]) -> dict[str, Any]:
    name = str(job.get("name") or job.get("display_name") or f"job-{job.get('id', '?')}")
    if " • " in name:
        stage, short_name = name.split(" • ", 1)
    else:
        stage, short_name = name, name
    started = job.get("started_at")
    completed = job.get("completed_at")
    duration = seconds(started, completed)
    queue = seconds(job.get("queued_at"), started)
    return {
        "id": job.get("id", 0),
        "name": short_name,
        "stage": stage,
        "status": job.get("conclusion") or job.get("status") or "unknown",
        "started": started or "—",
        "duration": duration,
        "queue": queue,
    }


def collect(payload: dict[str, Any]) -> dict[str, Any]:
    jobs = [normalise_job(job) for job in payload.get("jobs", [])]
    jobs.sort(key=lambda item: (-(item["duration"] or -1), item["stage"].lower(), item["name"].lower(), item["id"]))
    run = payload.get("run", {})
    starts = [parsed for job in payload.get("jobs", []) if (parsed := parse_time(job.get("started_at"))) is not None]
    ends = [parsed for job in payload.get("jobs", []) if (parsed := parse_time(job.get("completed_at"))) is not None]
    run_start = parse_time(run.get("run_started_at") or run.get("created_at")) or (min(starts) if starts else None)
    run_end = parse_time(run.get("updated_at")) or (max(ends) if ends else None)
    wall = (run_end - run_start).total_seconds() if run_start and run_end and run_end >= run_start else None
    runtime = sum(job["duration"] for job in jobs if job["duration"] is not None)
    longest = jobs[0] if jobs else None
    return {
        "run": run,
        "jobs": jobs,
        "wall_clock": wall,
        "job_runtime": runtime,
        "longest": longest,
    }


def format_duration(value: float | None) -> str:
    if value is None:
        return "—"
    minutes, secs = divmod(round(value), 60)
    hours, minutes = divmod(minutes, 60)
    return f"{hours}h {minutes:02d}m {secs:02d}s" if hours else f"{minutes}m {secs:02d}s"


def format_started(value: str) -> str:
    parsed = parse_time(value)
    return parsed.astimezone(timezone.utc).strftime("%Y-%m-%d %H:%M UTC") if parsed else value


def markdown(result: dict[str, Any]) -> str:
    run = result["run"]
    wall = result["wall_clock"]
    jobs = result["jobs"]
    longest = result["longest"]
    run_url = run.get("html_url") or "—"
    sha = run.get("head_sha") or "—"
    rows = []
    for job in jobs:
        share = f"{job['duration'] / wall:.1%}" if wall and job["duration"] is not None else "—"
        label = f"**{job['stage']} / {job['name']}**" if job is longest else f"{job['stage']} / {job['name']}"
        rows.append(f"| {label} | {job['status']} | {format_started(job['started'])} | {format_duration(job['duration'])} | {share} |")
    if not rows:
        rows.append("| — | no jobs returned | — | — | — |")
    longest_text = f"{longest['stage']} / {longest['name']} ({format_duration(longest['duration'])})" if longest else "—"
    return "\n".join([
        MARKER,
        "## CI timing statistics",
        "",
        f"Run: [{run.get('id', 'unknown')}]({run_url})  ",
        f"Head SHA: `{sha}`  ",
        f"Total wall-clock: **{format_duration(wall)}**  ",
        f"Total job runtime: **{format_duration(result['job_runtime'])}**  ",
        f"Jobs: **{len(jobs)}**  ",
        f"Longest job: **{longest_text}**",
        "",
        "| Job/stage | Status | Started | Duration | Wall-clock share |",
        "| --- | --- | --- | ---: | ---: |",
        *rows,
        "",
        "_Generated from the GitHub Actions jobs API; unfinished jobs are reported with their current status._",
    ])


def api_json(url: str, token: str) -> Any:
    request = Request(url, headers={"Accept": "application/vnd.github+json", "Authorization": f"Bearer {token}", "X-GitHub-Api-Version": "2022-11-28"})
    with urlopen(request) as response:
        return json.load(response)


def load_payload(args: argparse.Namespace) -> dict[str, Any]:
    if args.input:
        return json.loads(Path(args.input).read_text())
    token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
    if not token:
        raise SystemExit("GH_TOKEN or GITHUB_TOKEN is required for live mode")
    base = f"https://api.github.com/repos/{args.repo}"
    run = api_json(f"{base}/actions/runs/{args.run_id}", token)
    jobs = []
    page = 1
    while True:
        response = api_json(f"{base}/actions/runs/{args.run_id}/jobs?per_page=100&page={page}", token)
        jobs.extend(response.get("jobs", []))
        if len(response.get("jobs", [])) < 100:
            break
        page += 1
    # The collector is itself a job in this workflow. It cannot have completed
    # when it reads the API, so exclude it from measured pipeline scope.
    jobs = [job for job in jobs if job.get("name") != "Report CI timing statistics"]
    return {"run": run, "jobs": jobs}


def update_comment(repo: str, pr: int, body: str, token: str) -> None:
    base = f"https://api.github.com/repos/{repo}/issues/{pr}/comments"
    comments = api_json(base + "?per_page=100", token)
    existing = next((comment for comment in comments if MARKER in comment.get("body", "")), None)
    data = json.dumps({"body": body}).encode()
    if existing:
        url, method = f"{base}/{existing['id']}", "PATCH"
    else:
        url, method = base, "POST"
    request = Request(url, data=data, method=method, headers={"Accept": "application/vnd.github+json", "Authorization": f"Bearer {token}", "Content-Type": "application/json", "X-GitHub-Api-Version": "2022-11-28"})
    with urlopen(request):
        pass


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--repo", default=os.environ.get("GITHUB_REPOSITORY"))
    parser.add_argument("--run-id", type=int, default=os.environ.get("GITHUB_RUN_ID"))
    parser.add_argument("--input", type=Path)
    parser.add_argument("--output", type=Path)
    parser.add_argument("--pr", type=int)
    parser.add_argument("--comment", action="store_true")
    args = parser.parse_args()
    if not args.input and (not args.repo or not args.run_id):
        parser.error("--repo and --run-id are required in live mode")
    result = collect(load_payload(args))
    rendered = markdown(result)
    if args.output:
        args.output.write_text(rendered + "\n")
    else:
        print(rendered)
    if args.comment:
        token = os.environ.get("GH_TOKEN") or os.environ.get("GITHUB_TOKEN")
        if not token or not args.repo or not args.pr:
            raise SystemExit("GH_TOKEN/GITHUB_TOKEN, --repo, and --pr are required for comments")
        update_comment(args.repo, args.pr, rendered, token)
    return 0


if __name__ == "__main__":
    sys.exit(main())
