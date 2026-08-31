#!/usr/bin/env python3
"""Bounded parser/contract smoke test for the repository-local agent pack."""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
AGENTS = ROOT / ".codex" / "agents"
ROLE_FILES = {
    "gradle-maintainer.md": "write",
    "gradle-dependency-auditor.md": "read-only",
    "gradle-reviewer-ci-triage.md": "read-only",
}
REQUIRED = (
    "## Trigger",
    "## Input handoff",
    "## Allowed actions",
    "## Prohibited actions",
    "## Investigation",
    "## Verification",
    "## Stop/escalate",
    "## Evidence handoff",
    "## Secret hygiene",
    "## Definition of done",
)

def front_matter(text: str) -> dict[str, str]:
    lines = text.splitlines()
    if lines[:1] != ["---"]:
        raise AssertionError("missing front matter opener")
    try:
        end = lines.index("---", 1)
    except ValueError as error:
        raise AssertionError("missing front matter closer") from error
    values: dict[str, str] = {}
    for line in lines[1:end]:
        key, separator, value = line.partition(":")
        if not separator or not key or not value.strip():
            raise AssertionError(f"invalid front matter line: {line!r}")
        values[key.strip()] = value.strip()
    return values

def main() -> None:
    policy = (AGENTS / "gradle-policy.md").read_text()
    assert "PR #570" in policy and "./gradlew projects" in policy
    assert (ROOT / "settings.gradle").is_file()
    assert (ROOT / "build.gradle").is_file()
    assert (ROOT / ".github" / "workflows").is_dir()
    for filename, expected_mode in ROLE_FILES.items():
        path = AGENTS / filename
        text = path.read_text()
        metadata = front_matter(text)
        assert metadata["mode"] == expected_mode, (filename, metadata)
        for section in REQUIRED:
            assert section in text, (filename, section)
        if expected_mode == "read-only":
            assert "Do not edit" in text or "Never edit" in text
            assert "unchanged" in text or "no files changed" in text
    docs = (ROOT / "docs" / "gradle-agent-pack.md").read_text()
    assert "implementation/migration scenario" in docs
    assert "read-only review/CI-triage scenario" in docs
    print(f"validated policy, {len(ROLE_FILES)} role files, and 2 bounded scenarios")

if __name__ == "__main__":
    main()
