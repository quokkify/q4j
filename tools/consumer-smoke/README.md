# consumer-smoke

Standalone Gradle build behind the `consumerSmoke` check. It verifies that a published module is usable by a consumer
that declares only that module's `dev.quokkify` coordinate.

## What it checks

`consumerSmoke` runs in the `check` of every published module. It finds public subclasses of
`dev.quokkify.step.AbstractSteps` in the module (ClassGraph) and skips with a log line when the module has none.
Publishing is wired only for modules whose compile classpath includes `:testng-extensions`, so other modules skip
without publishing anything. When `:testng-extensions` is present but ClassGraph cannot find `AbstractSteps`, the task
fails instead of skipping. Otherwise it:

1. Publishes the module and its project-dependency closure to `build/consumer-smoke-repo`.
2. Generates a consumer that calls the inherited `verify()` and `verifySoftly()` on each steps class.
3. Compiles that consumer through this build, which resolves `dev.quokkify` only from the repository above.

This build has two subprojects:

- `pom` resolves the Maven POM only.
- `gmm` resolves Gradle Module Metadata.

## Run

```bash
./gradlew :data-utils:sql:consumerSmoke
```

## Failures

javac reports `error: cannot access AbstractSteps` followed by
`class file for dev.quokkify.step.AbstractSteps not found` when the consumer's compile classpath lacks the module that
declares the type. The module with the steps class must expose it as `api`: `api project(":testng-extensions")`, or,
for a steps class that extends one from another q4j module (for example `selenide-proxy` and `selenide`), `api` on that
module.

## Why a standalone build

Repositories are configured per project, so POM-only and Gradle Module Metadata resolution cannot coexist in one
project. Resolving inside the main build could also substitute the project for the published artifact, which hides the
missing-dependency problem the check exists to catch.
