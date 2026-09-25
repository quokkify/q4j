#!/usr/bin/env python3
"""Exercise the q4j Copier contract against disposable repositories."""

from __future__ import annotations

import json
import shutil
import subprocess
import tempfile
from pathlib import Path

import yaml

REPOSITORY = Path(__file__).resolve().parents[2]
TOOLKIT_SOURCE = "https://github.com/quokkify/project-toolkit.git"
TOOLKIT_REVISION = "v2.22.0"
TOOLKIT_VERSION = "v2.22.0"


def run(*args: str, cwd: Path) -> subprocess.CompletedProcess[str]:
    result = subprocess.run(
        args,
        cwd=cwd,
        text=True,
        capture_output=True,
        check=False,
    )
    if result.returncode:
        raise AssertionError(
            f"Command failed ({result.returncode}): {' '.join(args)}\n"
            f"stdout:\n{result.stdout}\nstderr:\n{result.stderr}"
        )
    return result


def copy_repository(destination: Path) -> None:
    run("git", "clone", "--quiet", "--no-local", str(REPOSITORY), str(destination), cwd=REPOSITORY)


def assert_no_merge_conflicts(repository: Path) -> None:
    status = run("git", "status", "--porcelain=v1", cwd=repository).stdout.splitlines()
    conflicts = [line for line in status if line.startswith(("UU ", "AA ", "DD ", "AU ", "UA "))]
    assert not conflicts, "Copier update left unmerged paths: " + ", ".join(conflicts)

    merge_artifacts = [
        str(path.relative_to(repository))
        for path in repository.rglob("*")
        if path.is_file() and path.suffix in {".rej", ".orig"}
    ]
    assert not merge_artifacts, "Copier update left merge artifacts: " + ", ".join(merge_artifacts)

    markers = []
    for path in repository.rglob("*"):
        if not path.is_file() or ".git" in path.parts:
            continue
        try:
            text = path.read_text(encoding="utf-8")
        except UnicodeDecodeError:
            continue
        if any(line.startswith(("<<<<<<< ", "=======", ">>>>>>> ")) for line in text.splitlines()):
            markers.append(str(path.relative_to(repository)))
    assert not markers, "Copier update left conflict markers in: " + ", ".join(markers)


def parse_generated_configuration(repository: Path) -> None:
    for path in repository.glob(".github/workflows/*.yml"):
        try:
            yaml.safe_load(path.read_text(encoding="utf-8"))
        except yaml.YAMLError as error:
            raise AssertionError(f"Generated workflow is not valid YAML: {path}: {error}") from error

    for path in repository.rglob("*.json"):
        if ".git" in path.parts:
            continue
        try:
            json.loads(path.read_text(encoding="utf-8"))
        except json.JSONDecodeError as error:
            raise AssertionError(f"Generated JSON is not valid: {path}: {error}") from error


def copier_update_fixture() -> Path:
    fixture = Path(tempfile.mkdtemp(prefix="q4j-copier-update-"))
    repository = fixture / "repository"
    copy_repository(repository)
    run(
        "copier",
        "update",
        "--defaults",
        "--trust",
        "--vcs-ref",
        TOOLKIT_REVISION,
        "--data",
        "components=[]",
        cwd=repository,
    )
    # Clearing components intentionally removes the generated component jobs.
    # Resolve that expected deletion before checking for unrelated Copier
    # conflicts; the component render is exercised separately below.
    run("git", "checkout", "--theirs", ".github/workflows/validate.yml", cwd=repository)
    run("git", "add", ".github/workflows/validate.yml", cwd=repository)
    assert_no_merge_conflicts(repository)
    parse_generated_configuration(repository)
    return fixture


def assert_external_contract(repository: Path) -> None:
    answers = yaml.safe_load((repository / ".copier-answers.yml").read_text(encoding="utf-8"))
    assert answers.get("components") == [], "External fixture must render with components=[]"

    workflow = (repository / ".github/workflows/allure-report.yml").read_text(encoding="utf-8")
    assert "const minimumArtifacts = 24;" in workflow, "External Allure minimum bound changed"
    assert "const maximumArtifacts = 50;" in workflow, "External Allure maximum bound changed"
    assert 'const artifactPrefix = "allure-results-";' in workflow, "External artifact prefix changed"
    assert "No external Allure artifacts found; report generation skipped." in workflow, (
        "External zero-artifact handling is missing"
    )

    template = repository / ".github/pull_request_template.md"
    text = template.read_text(encoding="utf-8")
    for heading in ("Release notes", "Usage example", "Migration", "Breaking change"):
        assert heading in text, f"PR template is missing required section: {heading}"


def component_fixture() -> Path:
    fixture = Path(tempfile.mkdtemp(prefix="q4j-copier-component-"))
    run(
        "copier",
        "copy",
        "--defaults",
        "--trust",
        "--vcs-ref",
        TOOLKIT_REVISION,
        "--data",
        "project_name=Q4J",
        "--data",
        f"toolkit_version={TOOLKIT_VERSION}",
        "--data",
        "allure_report=true",
        "--data",
        "allure_external_workflow_name=Run tests",
        "--data",
        "allure_external_workflow_path=.github/workflows/test.yml",
        "--data",
        'components=[{"type":"java","path":".","id":"app-java","name":"App Java"}]',
        TOOLKIT_SOURCE,
        str(fixture),
        cwd=REPOSITORY,
    )
    parse_generated_configuration(fixture)
    return fixture


def assert_component_contract(repository: Path) -> None:
    answers = yaml.safe_load((repository / ".copier-answers.yml").read_text(encoding="utf-8"))
    assert answers.get("components") == [{"type": "java", "path": ".", "id": "app-java", "name": "App Java"}], (
        "Component fixture did not preserve the java component declaration"
    )

    validate = (repository / ".github/workflows/validate.yml").read_text(encoding="utf-8")
    for expected in (
        "app-java:",
        'working-directory: "."',
        'test-artifact-name: allure-results-app-java',
        'test-artifact-path: "allure-results"',
    ):
        assert expected in validate, f"Component validation contract is missing: {expected}"

    checked_in_validate = (REPOSITORY / ".github/workflows/validate.yml").read_text(encoding="utf-8")
    for expected in (
        'test-command: "mkdir -p allure-results && touch allure-results/placeholder.txt && ./gradlew help --no-daemon --console=plain --stacktrace"',
        'test-artifact-path: "allure-results"',
    ):
        assert expected in checked_in_validate, f"Checked-in App Java remediation is missing: {expected}"

    allure = (repository / ".github/workflows/allure-report.yml").read_text(encoding="utf-8")
    for expected in (
        'const componentMode = true;',
        'const componentWorkflowPath = ".github/workflows/validate.yml";',
        'const componentArtifactPattern = /^allure-results-[A-Za-z_][A-Za-z0-9_-]*$/;',
    ):
        assert expected in allure, f"Component Allure contract is missing: {expected}"


def main() -> None:
    update_fixture = copier_update_fixture()
    component = component_fixture()
    try:
        assert_external_contract(update_fixture / "repository")
        assert_component_contract(component)
    finally:
        shutil.rmtree(update_fixture, ignore_errors=True)
        shutil.rmtree(component, ignore_errors=True)
    print("Copier contract passed: external update and java component render are deterministic")


if __name__ == "__main__":
    main()
