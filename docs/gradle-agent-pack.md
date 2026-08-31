# Using the Q4J Gradle agent pack

## Pick a role

Use the maintainer for Gradle edits, the dependency auditor for a read-only migration audit, and the reviewer/CI-triage role for exact-head review. Auditor and reviewer TOMLs enforce Codex `sandbox_mode = "read-only"`; do not combine implementation and review.

## Self-contained handoff

Include task, base SHA, candidate SHA, exact modules, expected dependency/property mapping, known constraints, requested role, and verification budget. Include prior failure output without secrets. A reviewer independently verifies the candidate SHA and receives the maintainer's changed-file list and command evidence.

## PR #570 implementation/migration case study

The bounded scenario references the historical PR head `f3e88c07a622f9832c1571f75f6861e9afa4055f` without mutating PR #570. It uses a disposable fixture/worktree and records the exact evidence packet:

```bash
gh pr view 570 --repo quokkify/q4j --json headRefOid,files
gh pr diff 570 --repo quokkify/q4j --patch
./gradlew projects --no-daemon --console=plain
./gradlew :common-utils:html:compileJava :data-utils:sql:compileJava --no-daemon --console=plain --stacktrace
```

The maintainer scenario evaluates catalog alias mapping, retained `common-utils/html/gradle/codegen.gradle`, and SQL annotation processors. The smoke harness verifies that this evidence is carried in a structured handoff.

## Read-only review/CI-triage scenario

The reviewer scenario independently records `git rev-parse HEAD`, `./gradlew --version`, `./gradlew projects`, the relevant check, `git diff --check`, and remote check/thread state. It must not report green when SHA, checks, or review state is unavailable or stale. Service failures such as Kafka metadata timeouts remain external prerequisites.

Run `python3 tools/gradle-agent-pack/smoke.py` for transport/discovery plus both bounded role invocation/evaluation scenarios. The harness is disposable and leaves the checkout unchanged.
