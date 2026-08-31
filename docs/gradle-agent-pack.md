# Using the Q4J Gradle agent pack

## Pick a role

Use the maintainer when the task changes Gradle files. Use the dependency auditor when the desired output is an audit and no mutation is authorized. Use the reviewer/CI triage role after implementation or when a check fails. A maintainer may write a patch; the two audit roles are read-only and must never be combined with implementation in one invocation.

## Self-contained handoff

Include:

```text
Task: migrate dependency X from implicit Gradle property to catalog alias
Base SHA: <40-character SHA>
Candidate SHA: <40-character SHA or “working tree”>
Modules: :common-utils:html, :data-utils:sql
Expected mapping: htmlcleaner -> net.sourceforge.htmlcleaner:htmlcleaner:2.29
Known constraints: preserve HTML code generation and querydsl annotation processing
Requested role: maintainer | auditor | reviewer
Verification budget: commands/time limit
```

Also include any prior failure output without secrets. A reviewer receives the maintainer's changed-file list and command evidence but independently verifies the exact head.

## Example: PR #570 implementation/migration scenario

This is a bounded, non-mutating scenario. It uses the historical PR head as a case-study reference and the current checkout for repository facts:

```bash
gh pr view 570 --repo quokkify/q4j --json headRefOid,files
gh pr diff 570 --repo quokkify/q4j --patch >/dev/null
./gradlew projects --no-daemon --console=plain
./gradlew :common-utils:html:compileJava :data-utils:sql:compileJava \
  --no-daemon --console=plain --stacktrace
```

The implementation role should report whether `gradle/libs.versions.toml` aliases map to the migrated usages, whether `common-utils/html/gradle/codegen.gradle` remains applied, and whether SQL annotation processors remain configured. The coordinator applies any accepted patch in a separate PR; this scenario does not modify PR #570.

## Example: read-only review/CI-triage scenario

```bash
git rev-parse HEAD
./gradlew --version
./gradlew projects --no-daemon --console=plain
./gradlew :integrations:kafka:check --no-daemon --console=plain --stacktrace

git diff --check
```

The reviewer records command exit codes and labels connection/authentication/timeouts for Kafka (or other services) as external prerequisites. It must not report a green verdict when the candidate SHA, CI status, or review-thread state is unavailable or stale. GitHub checks/comments are inspected separately from local Gradle results.

Observed bounded run in this checkout: `./gradlew :integrations:kafka:check --no-daemon --console=plain --stacktrace` compiled the module but failed its single Kafka publish test with `Topic messages not present in metadata after 60000 ms` (exit 1). This is the expected external Kafka prerequisite classification, not a catalog/accessor failure; no repository files were changed by the reviewer scenario.

## Evidence and completion

Use the shared handoff fields: role, scope, head SHA, files, commands with exit codes, findings, external prerequisites, and next action. Completion means the role's boundary was respected, the evidence is reproducible, and no secret values were disclosed. “No tests run” is evidence only when the reason and the next safe check are stated.
