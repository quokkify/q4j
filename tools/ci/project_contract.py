import json
import re
from pathlib import Path

import yaml

answers = {}
for answers_file in sorted(Path(".").glob(".copier-answers.*.yml")):
    answers.update(yaml.safe_load(answers_file.read_text()) or {})

if Path(".copier-answers.allure.yml").is_file():
    allure_document = yaml.safe_load(Path(".github/workflows/allure-report.yml").read_text())
    jobs = allure_document["jobs"]
    report = jobs.get("report")
    pages = jobs.get("pages")
    expected_core = "quokkify/ci-kit/.github/workflows/allure-publisher-core.yml@" + answers["toolkit_version"]
    expected_pages = "quokkify/ci-kit/.github/workflows/allure-pages.yml@" + answers["toolkit_version"]
    if not report or report.get("uses") != expected_core:
        raise SystemExit("Allure report must use the versioned shared publisher workflow")
    report_inputs = report.get("with", {})
    if answers.get("allure_source") != "external" or report_inputs.get("artifact-mode") != "external":
        raise SystemExit("q4j Allure reporting must use the external Run tests source")
    if report_inputs.get("source-workflow") != answers.get("allure_external_workflow_name") or report_inputs.get("source-workflow-path") != answers.get("allure_external_workflow_path"):
        raise SystemExit("External Allure reporting must match its configured source workflow")
    if report_inputs.get("minimum-artifacts") != answers.get("allure_external_artifact_min_count") or report_inputs.get("maximum-artifacts") != answers.get("allure_external_artifact_max_count") or report_inputs.get("skip-empty-artifacts") is not True:
        raise SystemExit("External Allure artifact contract must preserve its configured bounds and empty-run behavior")
    if report_inputs.get("history-path") != "allure-history/history.jsonl":
        raise SystemExit("Allure history must be carried between report runs")
    if set(jobs) != {"report", "pages"}:
        raise SystemExit("Allure reporting must use only the generated report and Pages jobs")
    if not pages or pages.get("uses") != expected_pages:
        raise SystemExit("Allure Pages must use the versioned shared publisher workflow")
    pages_inputs = pages.get("with", {})
    if pages_inputs.get("managed-pages-layout") is not True or "allure/pr-" not in str(pages_inputs.get("pages-destination-directory", "")):
        raise SystemExit("Allure Pages must publish to the managed per-PR report directory")
    test_workflow = Path(".github/workflows/test.yml").read_text()
    if "path: \"**/build/allure-results\"" in test_workflow:
        raise SystemExit("Test jobs must upload only their own Allure results directory")
    if "allure-artifact" not in test_workflow:
        raise SystemExit("Test jobs must stage Allure results with their module path preserved")
    if "allure-artifact/stability-tests/build" not in test_workflow:
        raise SystemExit("Stability Allure results must use a distinct source path")
    if "results_path" not in test_workflow or "results_path" not in Path(".github/workflows/reusable-module-matrices.yml").read_text():
        raise SystemExit("Test matrix must provide a dedicated Allure results path")

publish_workflow = Path(".github/workflows/publish-maven-central.yml")
if publish_workflow.is_file():
    publish_text = publish_workflow.read_text()
    if "uses: actions/setup-java@" in publish_text:
        raise SystemExit("Maven Central publishing must use ci-kit/actions/setup-java-gradle")
    setup_java_gradle = re.search(
        r"(?m)^\s+uses: quokkify/ci-kit/actions/setup-java-gradle@(?P<reference>[^ #]+)(?:\s+#\s*(?P<comment>\S+))?$",
        publish_text,
    )
    if not setup_java_gradle:
        raise SystemExit("Maven Central publishing must use ci-kit/actions/setup-java-gradle")
    reference = setup_java_gradle.group("reference")
    comment = setup_java_gradle.group("comment")
    pinned_release = len(reference) == 40 and all(char in "0123456789abcdef" for char in reference)
    if (reference != answers["toolkit_version"] and not pinned_release) or (pinned_release and comment != answers["toolkit_version"]):
        raise SystemExit("Maven Central publishing toolkit action must match toolkit_version")

selenium_grid = Path("tools/environment/assets/selenium-grid/config.toml").read_text()
marker = re.search(
    r'(?m)^# renovate: depName=selenium/standalone-chromium currentValue=(?P<currentValue>\d+\.\d+(?:\.\d+)?-\d+) currentDigest=(?P<currentDigest>sha256:[a-f0-9]{64})$',
    selenium_grid,
)
if not marker:
    raise SystemExit("selenium grid config must keep a valid Renovate marker comment for the standalone-chromium image")
configs_line = re.search(
    r'(?m)^configs = \["selenium/standalone-chromium:(?P<currentValue>\d+\.\d+(?:\.\d+)?-\d+)"',
    selenium_grid,
)
if not configs_line or configs_line.group("currentValue") != marker.group("currentValue"):
    raise SystemExit("selenium grid config must pin the standalone-chromium image by tag and keep marker/config values aligned")
if '@sha256:' in selenium_grid:
    raise SystemExit("selenium grid config must not use a digest reference in configs")
renovate = json.loads(Path(".github/renovate.json").read_text())
selenium_rules = [
    rule for rule in renovate["customManagers"]
    if "Selenium" in str(rule.get("description", ""))
]
if len(selenium_rules) != 1:
    raise SystemExit("selenium Renovate extraction must stay consolidated into a single custom manager")
selenium_rule = selenium_rules[0]
pattern = selenium_rule["matchStrings"][0]
if "depName=(?<depName>selenium/standalone-chromium)" not in pattern or "currentDigest=(?<currentDigest>sha256:[a-f0-9]{64})" not in pattern or "configs\\s*=\\s*\\[\\s*\"selenium/standalone-chromium:(?<currentValue>" not in pattern:
    raise SystemExit("selenium Renovate matcher must capture both the digest marker and config tag in one rule")
if selenium_rule.get("datasourceTemplate") != "docker":
    raise SystemExit("selenium Renovate matcher must stay on the docker datasource")
if selenium_rule.get("packageNameTemplate") != "selenium/standalone-chromium":
    raise SystemExit("selenium Renovate matcher must explicitly pin the standalone-chromium package name")
if selenium_rule.get("versioningTemplate") != "regex:^(?<major>\\d+)\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?-(?<build>\\d+)$":
    raise SystemExit("selenium Renovate matcher must keep the dated tag versioning")
