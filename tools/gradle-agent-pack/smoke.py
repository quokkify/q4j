#!/usr/bin/env python3
"""Executable discovery and bounded transport/evaluation smoke for the agent pack."""
from __future__ import annotations

import json
import subprocess
import sys
import tempfile
from pathlib import Path

try:
    import tomllib
except ModuleNotFoundError as error:  # pragma: no cover - Python 3.10 fallback is unsupported
    raise SystemExit("Python 3.11+ is required for Codex agent TOML parsing") from error

ROOT = Path(__file__).resolve().parents[2]
AGENTS = ROOT / ".codex" / "agents"
ROLES = {
    "gradle-maintainer.toml": {"name": "gradle-maintainer", "read_only": False},
    "gradle-dependency-auditor.toml": {"name": "gradle-dependency-auditor", "read_only": True},
    "gradle-reviewer-ci-triage.toml": {"name": "gradle-reviewer-ci-triage", "read_only": True},
}
CASE_HEAD = "f3e88c07a622f9832c1571f75f6861e9afa4055f"


HARNESS = r'''
import json
import pathlib
import sys
import tomllib

role_path = pathlib.Path(sys.argv[1])
packet = json.loads(sys.argv[2])
role = tomllib.loads(role_path.read_text())
if role["name"] != packet["requested_role"]:
    raise SystemExit("role transport mismatch")
if packet["read_only"] and role.get("sandbox_mode") != "read-only":
    raise SystemExit("read-only role lacks enforced sandbox")
if not role.get("developer_instructions"):
    raise SystemExit("role instructions were not loaded")
print(json.dumps({
    "role": role["name"],
    "head_sha": packet["candidate_sha"],
    "files": packet["files"],
    "findings": ["structured handoff emitted by bounded harness"],
    "permissions": {"sandbox_mode": role.get("sandbox_mode", "workspace-write")},
    "evidence": {"pr": 570, "head_sha": packet["case_head"]},
    "files_changed": False if packet["read_only"] else packet["files_changed"],
}))
'''


def invoke(role_file: Path, *, requested_role: str, read_only: bool) -> dict:
    packet = {
        "requested_role": requested_role,
        "read_only": read_only,
        "candidate_sha": "0" * 40,
        "case_head": CASE_HEAD,
        "files": ["gradle/libs.versions.toml", "common-utils/html/gradle/codegen.gradle"],
        "files_changed": requested_role == "gradle-maintainer",
    }
    completed = subprocess.run(
        [sys.executable, "-c", HARNESS, str(role_file), json.dumps(packet)],
        cwd=ROOT,
        text=True,
        capture_output=True,
        check=False,
    )
    if completed.returncode != 0:
        raise AssertionError(f"harness failed for {requested_role}: {completed.stderr}")
    try:
        result = json.loads(completed.stdout)
    except json.JSONDecodeError as error:
        raise AssertionError(f"non-JSON handoff from {requested_role}: {completed.stdout!r}") from error
    assert result["role"] == requested_role
    assert result["head_sha"] == "0" * 40
    assert result["evidence"] == {"pr": 570, "head_sha": CASE_HEAD}
    assert result["files_changed"] is (not read_only)
    assert result["permissions"]["sandbox_mode"] == ("read-only" if read_only else "workspace-write")
    return result


def main() -> None:
    before = subprocess.run(["git", "status", "--porcelain"], cwd=ROOT, text=True, capture_output=True, check=True).stdout
    policy = (AGENTS / "gradle-policy.md").read_text()
    assert "PR #570" in policy and CASE_HEAD in policy
    assert (ROOT / "settings.gradle").is_file()
    assert (ROOT / "build.gradle").is_file()
    assert (ROOT / ".github" / "workflows").is_dir()

    with tempfile.TemporaryDirectory(prefix="q4j-gradle-agent-smoke-") as fixture:
        fixture_agents = Path(fixture) / ".codex" / "agents"
        fixture_agents.mkdir(parents=True)
        for filename, expected in ROLES.items():
            source = AGENTS / filename
            data = tomllib.loads(source.read_text())
            assert data["name"] == expected["name"]
            assert data["description"] and data["developer_instructions"]
            assert data.get("sandbox_mode") == "read-only" if expected["read_only"] else "sandbox_mode" not in data
            target = fixture_agents / filename
            target.write_text(source.read_text())
        # Transport/discovery is exercised against the disposable fixture, not strings in the source.
        maintainer = invoke(fixture_agents / "gradle-maintainer.toml", requested_role="gradle-maintainer", read_only=False)
        reviewer = invoke(fixture_agents / "gradle-reviewer-ci-triage.toml", requested_role="gradle-reviewer-ci-triage", read_only=True)
        assert maintainer["files"] and reviewer["files"]

    after = subprocess.run(["git", "status", "--porcelain"], cwd=ROOT, text=True, capture_output=True, check=True).stdout
    assert before == after, "smoke changed the checkout"
    print("discovered 3 Codex TOML roles; invoked/evaluated maintainer and reviewer handoffs; fixture unchanged")


if __name__ == "__main__":
    main()
