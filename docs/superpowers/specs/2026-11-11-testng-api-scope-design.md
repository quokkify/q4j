# Publish `dev.quokkify:testng` as API Where Steps Classes Expose It — Design

Date: 2026-11-11. Context: issue #825, found in the final review of #824.

## Goal

A consumer that depends on a single q4j module, for example `dev.quokkify:sql`, can compile code that subclasses or calls a public steps class of that module, including the methods inherited from `dev.quokkify.step.AbstractSteps`. A build check keeps it that way.

## Why

- Nine published modules have public classes that extend `AbstractSteps`, but declare `:testng-extensions` as `implementation` (POM scope `runtime`) or `compileOnly` (absent from the POM). Neither puts `dev.quokkify:testng` on a Maven or Gradle consumer's compile classpath.
- The problem stays hidden in projects that add `dev.quokkify:testng` themselves, such as projects generated from the test-automation template.

## Decisions

| Topic               | Decision                                                                                                                                                                                                     |
| ------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Steps modules       | `api project(":testng-extensions")`.                                                                                                                                                                         |
| Unused dependencies | Removed from `common-utils/signature` (`implementation`) and `integrations/selenide-proxy` (`compileOnly`); neither uses a `testng-extensions` type in `src/main`.                                           |
| Listener modules    | jira/testng, reportportal/testng, testrail/testng keep `implementation`: they use `dev.quokkify.util.TestUtils` only inside listener bodies.                                                                 |
| Guard               | A consumer smoke check, `consumerSmoke`, in every published module, wired into `check`. It compiles a generated consumer against the published POM and Gradle Module Metadata, resolved by coordinates only. |
| Guard scope         | Public subclasses of `AbstractSteps`, discovered automatically. A general API-leak guard over all project dependencies is out of scope.                                                                      |
| CI                  | No workflow change: `build.yml` and `test.yml` already run `<module>:check` per module.                                                                                                                      |

## Dependency changes

| Module                                             | Before           | After     |
| -------------------------------------------------- | ---------------- | --------- |
| `data-utils/sql`                                   | `implementation` | `api`     |
| `data-utils/nosql/mongodb`                         | `implementation` | `api`     |
| `data-utils/nosql/morphia-legacy`                  | `implementation` | `api`     |
| `data-utils/nosql/redis`                           | `implementation` | `api`     |
| `integrations/kafka`                               | `implementation` | `api`     |
| `integrations/rabbitmq`                            | `implementation` | `api`     |
| `integrations/rest-assured`                        | `implementation` | `api`     |
| `integrations/tyrus`                               | `implementation` | `api`     |
| `integrations/selenide`                            | `compileOnly`    | `api`     |
| `integrations/selenide-proxy`                      | `compileOnly`    | removed   |
| `common-utils/signature`                           | `implementation` | removed   |
| `integrations/{jira,reportportal,testrail}/testng` | `implementation` | unchanged |

The explicit `testImplementation project(":testng-extensions")` lines in selenide and selenide-proxy are removed too, because `gradle/dependencies.gradle` already adds that dependency to every module except `:testng-extensions`.

`testng-extensions` itself is unchanged. `AbstractSteps` exposes only `V verify()` and `verifySoftly(Consumer<V>...)`; `SoftAssertions` is used inside a method body, so `implementation libs.assertj.core` stays correct.

Consumers of the nine modules now get the `api` dependencies of `testng-extensions` (`testng`, `allure-testng`, slf4j and the log4j bridge) on their compile classpath. This is expected for a test library.

## `consumerSmoke`

Registered from a shared Gradle script for every module that `gradle/module-metadata.gradle` marks as published; `check` depends on it.

1. **Discover.** After `compileJava`, ClassGraph (`libs.classgraph`) scans the module's main output, with its compile classpath for supertype resolution, for public subclasses of `dev.quokkify.step.AbstractSteps`. With none found, the task is skipped through `onlyIf`, and the skip reason is logged.
2. **Generate.** One Java source with a method per discovered class, with every type parameter as a wildcard:

   ```java
   static void use(DatabaseSteps<?> steps) {
     steps.verify();
     steps.verifySoftly();
   }
   ```

   javac must resolve `AbstractSteps` to compile the inherited calls, so this checks the same thing as the issue's `class MySteps extends DatabaseSteps` without generating constructors or abstract method bodies.

3. **Publish.** The module and every project dependency on its runtime classpath publish to `build/consumer-smoke-repo` in the root project, through a Maven repository named `consumerSmoke` added to the existing vanniktech publishing setup.
4. **Compile the consumer.** The task runs the standalone build in `tools/consumer-smoke` with the project's wrapper, passing the coordinates, the repository path and the generated source. That build is not part of the root `settings.gradle`. It has two subprojects:
   - `pom`: the repository uses `metadataSources { mavenPom(); artifact() }`, as a Maven consumer sees the module;
   - `gmm`: default metadata sources, as a Gradle consumer sees it.

   Both resolve `dev.quokkify` only from the local repository (`exclusiveContent`) and everything else from Maven Central. A javac failure in either subproject fails the task, and its output names the missing type.

A separate build, rather than a detached configuration in the root build, is needed because repositories are declared per project, so one project cannot resolve the same coordinates both POM-only and with module metadata, and because in-build resolution could pick the project instead of the published artifact. The nested build costs about 15–30 s per steps module; CI already runs modules as separate jobs.

### Risks checked first in the plan

- Whether `signAllPublications()` requires signing keys for a local repository. If it does, signing tasks are disabled for publications to `consumerSmoke`.
- The nested build must use the same JDK toolchain and honour the CI's network settings.

### Proof the guard fires

During implementation, one module is set back to `implementation`; `consumerSmoke` must fail with a javac error naming `AbstractSteps`. Set back to `api`, it must pass. Both outputs go into the PR description.

## Check against a real consumer

A one-off local check, not part of CI, on a downstream test project that extends steps classes from `rest-assured`, `selenide` and `sql` and today declares `dev.quokkify:testng` directly, which hides the bug.

- q4j is published twice to a scratch Maven repository (`-Dmaven.repo.local=<scratch>`): `main` as `0.0.0-pre825` and this branch as `0.0.0-fix825`. Comparing against `main`, not the consumer's released q4j version, keeps unrelated API changes out of the result.
- The consumer is not edited. A Gradle init script resolves `dev.quokkify` only from the scratch repository, forces the version, and in the "without" variant removes only the direct `dev.quokkify:testng` declaration (`withDependencies { removeIf … }`); the transitive one stays.
- It runs `compileJava compileTestJava` in a detached worktree of the consumer's default branch. Tests are not run: they need the consumer's backend and database, and the defect shows at compile time.

| q4j      | Direct `testng` | Expected | Shows                            |
| -------- | --------------- | -------- | -------------------------------- |
| `main`   | yes             | green    | baseline                         |
| `main`   | no              | red      | the defect on a real consumer    |
| this fix | no              | green    | the fix works                    |
| this fix | yes             | green    | no regression for the workaround |

The matrix, with the failing javac line of the red row, goes into the PR description without naming the consumer project.

## Documentation

- The README of each of the nine modules says that `dev.quokkify:testng` comes transitively.
- `testng-extensions/README.md` says it is added directly only when listeners or annotations are used without a steps module.
- `tools/consumer-smoke/README.md` says what the check verifies, how to run it (`./gradlew :data-utils:sql:consumerSmoke`) and how to read a failure.

## Release

One PR, `fix(build): publish dev.quokkify:testng as api where steps classes expose it`, with the release-notes sections filled in:

- Highlight: the nine modules work with only their own coordinate;
- Usage example: a consumer with only `dev.quokkify:sql`;
- Migration: none required; a direct `dev.quokkify:testng` dependency added only to work around this can be removed.

## Acceptance

- The generated POMs of the nine modules list `dev.quokkify:testng` in `compile` scope; the POMs of signature and selenide-proxy no longer list it.
- `./gradlew <module>:check` passes for all published modules, and `consumerSmoke` runs (not skipped) for the nine steps modules.
- The red/green proof above is recorded in the PR.
- The real-consumer matrix matches the expected column.

## Out of scope

- A general guard against public signatures that use types from `implementation`-only dependencies.
- The dependency-analysis Gradle plugin.
- Changes to the test-automation template. Its `template/AGENTS.md` tells projects to declare `dev.quokkify:testng` next to `rest-assured`; a separate issue in quokkify/java-test-automation-template tracks updating that paragraph after the q4j release.
