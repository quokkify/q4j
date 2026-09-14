#!/usr/bin/env python3
"""Validate Q4J publication and Release Please naming contracts."""
from __future__ import annotations

import json
import re
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONFIG = ROOT / ".github/release-please/config.json"
MANIFEST = ROOT / ".github/release-please/manifest.json"
METADATA = ROOT / "gradle/module-metadata.gradle"
MATRICES = ROOT / ".github/workflows/reusable-module-matrices.yml"


def main() -> None:
    config = json.loads(CONFIG.read_text())
    manifest = json.loads(MANIFEST.read_text())
    package = config["packages"]["."]
    assert package["package-name"] == "q4j"
    assert config["include-component-in-tag"] is False
    assert manifest["."]
    sections = {entry["type"]: entry for entry in package["changelog-sections"]}
    for hidden in ("build", "ci", "test", "chore", "style"):
        assert sections[hidden]["hidden"] is True, hidden
    assert sections["deps"]["section"] == "📦 Dependencies"

    metadata = METADATA.read_text()
    artifact_ids = re.findall(r"artifactId:\s*'([^']+)'", metadata)
    assert artifact_ids and all(not value.startswith("q4j-") for value in artifact_ids)
    assert len(artifact_ids) == len(set(artifact_ids))

    matrix_names = re.findall(r"\{ name: '([^']+)'", MATRICES.read_text())
    assert matrix_names and all(not value.startswith("q4j-") for value in matrix_names)

    tracked_paths = subprocess.check_output(["git", "ls-files"], text=True).splitlines()
    tracked = "\n".join(
        (ROOT / relative).read_text(errors="replace")
        for relative in tracked_paths
        if relative != "tools/release_contract.py"
    )
    assert "dev.quokkify:q4j-" not in tracked
    print(f"release contract OK: {len(artifact_ids)} unique artifact IDs, {len(matrix_names)} matrix entries")


if __name__ == "__main__":
    main()
