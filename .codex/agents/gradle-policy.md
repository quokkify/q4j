# Shared Gradle policy

Use this policy with every Q4J Gradle specialist. It is based on the repository layout and the version-catalog migration represented by PR #570 (validated head `f3e88c07a622f9832c1571f75f6861e9afa4055f`). The current checkout is authoritative when it differs from that historical case study.

## Repository facts

- Wrapper: `./gradlew`; inspect `./gradlew --version` before relying on Gradle behavior.
- Modules are declared in `settings.gradle`; use the exact project paths printed by `./gradlew projects`.
- Shared build policy is applied from `build.gradle` and `gradle/{compilation,code-analysis,dependencies,tests,aspectj,module-metadata,publishing}.gradle`.
- The version catalog, when present, is `gradle/libs.versions.toml`; generated Groovy accessors are consumed as `libs.foo.bar` and `rootProject.libs.foo.bar`.
- HTML model generation is wired by `common-utils/html/gradle/codegen.gradle`; a catalog migration must not remove that application or its source/task wiring.
- CI workflow commands and external-service assumptions are in `.github/workflows/*.yml`. Kafka, RabbitMQ, Redis, MongoDB, browser/grid, ReportPortal, Jira, and TestRail are integration concerns, not proof that a local build defect exists.

## Guardrails

1. Read `git status --short --branch`, relevant build files, `settings.gradle`, and the applicable workflow before editing or judging.
2. Keep changes minimal. Do not alter library behavior for an agent demonstration.
3. For catalog work, parse TOML, confirm every `libs.*` accessor maps to an existing alias, and compare every migrated dependency coordinate and version. Check bundles, constraints, annotation processors, and non-default configurations.
4. Search for stale implicit property lookups in the touched Gradle surface. Do not remove a property still used elsewhere.
5. Preserve generated code semantics: verify code-generation scripts, source sets, task dependencies, and generated-source inclusion.
6. Validate in stages: `./gradlew projects`; the smallest affected compile/check; then relevant test/check tasks. Use `--no-daemon --console=plain --stacktrace` for reproducible evidence.
7. Report skipped tests explicitly; never call a service outage a passing check.
8. Always run `git diff --check`. Record local and remote/exact-head identity separately.
9. Stop on missing credentials, destructive requests, unexplained version drift, or an external service failure that prevents a meaningful conclusion.

## Failure classification

- Configuration/accessor/TOML/parser error: code defect.
- Compilation/static-analysis/test assertion failure: code defect unless independently shown environmental.
- Connection refusal, authentication failure, timeout, or unavailable Kafka/Redis/etc.: external prerequisite.
- CI status for a different commit: stale evidence.

## Handoff schema

Report `role`, `scope`, `head_sha`, `files`, `commands` (including exit codes), `findings`, `external_prerequisites`, and `next_action`. Never include tokens, secret values, private configuration, or full environment dumps.
