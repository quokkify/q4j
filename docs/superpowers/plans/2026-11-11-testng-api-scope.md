# Publish `dev.quokkify:testng` as API in Steps Modules Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Consumers of the nine steps modules compile against the inherited `AbstractSteps` API with only the module coordinate, and a `consumerSmoke` check in `<module>:check` keeps it that way.

**Architecture:** A shared script `gradle/consumer-smoke.gradle` adds, to every published module, a `consumerSmoke` repository, a source generator that finds public `AbstractSteps` subclasses with ClassGraph, and an `Exec` task that compiles the generated consumer in the standalone build `tools/consumer-smoke` (subprojects `pom` and `gmm`). The guard lands first and is red on `main`'s dependencies; the dependency fix turns it green.

**Tech Stack:** Gradle 9.9 (Groovy DSL), vanniktech maven-publish 0.37.0, ClassGraph 5.0.2 (`libs.classgraph`), Java 21 toolchain.

**Spec:** `docs/superpowers/specs/2026-11-11-testng-api-scope-design.md`

## Global Constraints

- Work only in the worktree `<worktree>`, branch `fix/testng-api-scope`.
- Steps base type: `dev.quokkify.step.AbstractSteps`. Steps modules: `:data-utils:sql`, `:data-utils:nosql:mongodb`, `:data-utils:nosql:morphia-legacy`, `:data-utils:nosql:redis`, `:integrations:kafka`, `:integrations:rabbitmq`, `:integrations:rest-assured`, `:integrations:selenide`, `:integrations:tyrus`.
- Smoke repository: `<root>/build/consumer-smoke-repo`; repository name `consumerSmoke` (publish task `publishAllPublicationsToConsumerSmokeRepository`).
- `tools/consumer-smoke` is not included in the root `settings.gradle`.
- The consumer build resolves `dev.quokkify` only from the smoke repository (`exclusiveContent`), everything else from `mavenCentral()`.
- No new third-party dependency besides ClassGraph on the root buildscript classpath.
- The public q4j repo never names the downstream consumer project; scratch files go to the session scratchpad, not the repo.
- Markdown passes `npx prettier@3 --check .` (CI runs it over `docs/` and READMEs).
- Commits: Conventional Commits, ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **False green from discovery.** If ClassGraph cannot resolve `AbstractSteps`, it finds no subclasses and the task is skipped. Discovery must scan the module's output together with its `compileClasspath` and log the classes found. Task 2 asserts that none of the nine modules is skipped.
2. **Release version without signing keys.** `check` with `-Pversion=0.0.0-probe` and no `signingInMemory*` properties must not fail in a `Sign` task. Covered in Task 1, Step 6.
3. **Stale artifacts after a dependency change.** When the module goes `api` → `implementation` → `api` in one workspace, without `clean`, the result must go green → red → green. Gradle must not reuse the previous SNAPSHOT metadata. Covered in Task 2, Step 4.
4. **Recursive generics.** `PageSteps<S extends PageSteps<S, V, P>, V extends Verification<S, V, P>, P extends Page>` must compile with wildcards. Covered by `:integrations:selenide` in Task 2, Step 3.
5. **A missing transitive project publication.** If a project dependency (for example `:common-utils:core`) is not published to the smoke repository, the consumer build must fail loudly with "Could not find dev.quokkify:core". It must not compile against something else. Covered by the `exclusiveContent` constraint and Task 1, Step 4.

---

### Task 1: `consumerSmoke` guard (red on current dependencies)

**Files:**

- Create: `gradle/consumer-smoke.gradle`
- Create: `tools/consumer-smoke/settings.gradle`, `tools/consumer-smoke/build.gradle`
- Modify: `build.gradle` (a `buildscript` block before `plugins` with `classpath libs.classgraph`; `apply from: 'gradle/consumer-smoke.gradle'` after `publishing.gradle`)

**Interfaces:**

- Produces, per published module (`rootProject.modules[path]` present and `publish != false`):
  - repository `consumerSmoke` in `publishing.repositories`, URL `rootProject.layout.buildDirectory.dir('consumer-smoke-repo')`;
  - task `generateConsumerSmokeSources`: output dir `build/generated/consumer-smoke/src`, writes `consumersmoke/ConsumerSmoke.java`, logs `consumerSmoke <path>: N steps class(es): <fqcn, ...>`;
  - task `consumerSmoke` (group `verification`), wired as `check.dependsOn consumerSmoke`; `onlyIf` at least one class was found, with the skip logged.
- Nested build properties: `-PsmokeRepo=<abs path>`, `-PsmokeCoordinate=dev.quokkify:<artifactId>:<version>`, `-PsmokeSources=<abs path>`. Target task: `compileJava` (runs in `:pom` and `:gmm`).

- [ ] **Step 1: Write the nested consumer build.** `tools/consumer-smoke/settings.gradle`: `rootProject.name = 'consumer-smoke'`, `include 'pom', 'gmm'`. `tools/consumer-smoke/build.gradle` configures both subprojects: plugin `java`, toolchain 21, `implementation smokeCoordinate`, `sourceSets.main.java.srcDir smokeSources`. Repositories: `exclusiveContent { forRepository { maven { url smokeRepo; <pom only: metadataSources { mavenPom(); artifact() }> } }; filter { includeGroup 'dev.quokkify' } }`, then `mavenCentral()`. Fail with a clear `GradleException` if a property is missing.

- [ ] **Step 2: Write `gradle/consumer-smoke.gradle`.**
  - **Discovery and generation.** Use `ClassGraph().overrideClasspath(classesDirs + compileClasspath).enableClassInfo().acceptPackages('dev.quokkify')`. Keep the subclasses of `dev.quokkify.step.AbstractSteps` that are public, are not anonymous, and whose classpath element is in this module's `classesDirs`. Generate:

    ```java
    package consumersmoke;

    final class ConsumerSmoke {
      static void use0(dev.quokkify.steps.DatabaseSteps<?> steps) {
        steps.verify();
        steps.verifySoftly();
      }
      // one useN per class; nested classes use getName().replace('$', '.');
      // one '?' per type parameter of the class's type signature
    }
    ```

  - **Publishing.** `consumerSmoke` depends on `publishAllPublicationsToConsumerSmokeRepository` of this module and of every project reachable through `runtimeClasspath` project dependencies. Reference them by task path.
  - **Exec.** Runs `rootProject.file('gradlew')` with `-p <root>/tools/consumer-smoke --console=plain compileJava -PsmokeRepo=… -PsmokeCoordinate=… -PsmokeSources=…`. The task should be compatible with the configuration cache: capture values at configuration time and do not touch `project` in `doLast`.

- [ ] **Step 3: Run the guard on a steps module. It must fail.**
      Run: `./gradlew :data-utils:sql:consumerSmoke --console=plain`
      Expected: the log line `consumerSmoke :data-utils:sql: 1 steps class(es): dev.quokkify.steps.DatabaseSteps`, then FAIL. The nested javac output contains `cannot access dev.quokkify.step.AbstractSteps` (or `class file for dev.quokkify.step.AbstractSteps not found`) in both `:pom:compileJava` and `:gmm:compileJava`. Save the failing excerpt to `<scratchpad>/red-sql.txt` for the PR.

- [ ] **Step 4: Check the skip and the loud-failure paths.**
      Run: `./gradlew :common-utils:core:consumerSmoke --console=plain`. Expected: the task is `SKIPPED` and the log says no steps classes were found.
      Temporarily comment out the dependency on `:common-utils:core`'s publish task and delete `build/consumer-smoke-repo`, then run Step 3 again. Expected: it fails with `Could not find dev.quokkify:core`. Restore the dependency.

- [ ] **Step 5: Run the guard on a module whose dependency is already correct.**
      Run: `./gradlew :integrations:jira:testng:consumerSmoke --console=plain`. Expected: `SKIPPED`, because there are no steps classes. This confirms that listener modules are not affected.

- [ ] **Step 6: Release-version probe without signing keys.**
      Run: `./gradlew :common-utils:core:publishAllPublicationsToConsumerSmokeRepository -Pversion=0.0.0-probe --console=plain`, with no `ORG_GRADLE_PROJECT_signing*` in the environment.
      Expected: PASS. If a `sign*Publication` task fails, make signing skip publications to `consumerSmoke` when no key is configured. Releases to Central must still require signing; check that `./gradlew :common-utils:core:publishToMavenCentral --dry-run -Pversion=0.0.0-probe` still lists the `sign*` tasks. Rerun until it passes.

- [ ] **Step 7: Commit.** Step 3 is red on purpose: CI does not run until the PR, and Task 2 turns it green.

  ```bash
  git add gradle/consumer-smoke.gradle tools/consumer-smoke build.gradle
  git commit -m "build: add consumerSmoke check for steps modules"
  ```

### Task 2: Dependency scopes (green)

**Files:**

- Modify: the `build.gradle` of the nine steps modules (`implementation`/`compileOnly project(":testng-extensions")` → `api project(":testng-extensions")`)
- Modify: `common-utils/signature/build.gradle` (remove `implementation project(":testng-extensions")`)
- Modify: `integrations/selenide-proxy/build.gradle` (remove `compileOnly` and `testImplementation project(":testng-extensions")`)
- Modify: `integrations/selenide/build.gradle` (remove the duplicate `testImplementation project(":testng-extensions")`)

**Interfaces:**

- Consumes: `consumerSmoke` from Task 1.

- [ ] **Step 1: Change the scopes as in the spec table.** In the `pom` and `gmm` consumers this flips the Task 1 red to green.

- [ ] **Step 2: Run the guard on all nine modules.**
      Run: `./gradlew :data-utils:sql:consumerSmoke :data-utils:nosql:mongodb:consumerSmoke :data-utils:nosql:morphia-legacy:consumerSmoke :data-utils:nosql:redis:consumerSmoke :integrations:kafka:consumerSmoke :integrations:rabbitmq:consumerSmoke :integrations:rest-assured:consumerSmoke :integrations:selenide:consumerSmoke :integrations:tyrus:consumerSmoke --console=plain`
      Expected: BUILD SUCCESSFUL. None of the nine tasks is `SKIPPED`, and each one logs at least one steps class.

- [ ] **Step 3: Check the generated POMs.**
      Run: `./gradlew generatePomFileForMavenPublication -q`, then for each module `grep -A3 '<artifactId>testng</artifactId>' <module>/build/publications/maven/pom-default.xml`.
      Expected: in the nine steps modules `dev.quokkify:testng` has `<scope>compile</scope>`. In the POMs of `common-utils/signature` and `integrations/selenide-proxy` there is no `dev.quokkify:testng` (`org.testng:testng`, if it appears transitively, does not count).

- [ ] **Step 4: Red→green proof without `clean`.**
      Temporarily set `:data-utils:sql` back to `implementation` and run `./gradlew :data-utils:sql:consumerSmoke`. Expected: FAIL with the same error as in Task 1, Step 3. Restore `api` and run again. Expected: PASS. Save both excerpts to `<scratchpad>/redgreen-sql.txt`.

- [ ] **Step 5: Full check of the affected modules.**
      Run `./gradlew <module>:check -x <module>:test --console=plain`, the way CI runs it, for the nine steps modules plus `:common-utils:signature`, `:integrations:selenide-proxy` and `:testng-extensions`. Also run `./gradlew :common-utils:signature:test :integrations:selenide-proxy:compileTestJava`.
      Expected: everything is green, and `verifyArchgateRuleList` and checkstyle pass.

- [ ] **Step 6: Commit.**
  ```bash
  git add data-utils integrations common-utils/signature
  git commit -m "fix(build): publish dev.quokkify:testng as api where steps classes expose it"
  ```

### Task 3: Documentation

**Files:**

- Modify: the `README.md` of the nine steps modules: one sentence in the dependency or installation section saying that `dev.quokkify:testng` (`AbstractSteps`) comes transitively and does not need to be declared separately
- Modify: `testng-extensions/README.md`: declare it directly only when listeners or annotations are used without a steps module
- Create: `tools/consumer-smoke/README.md`: what the check verifies (POM and GMM consumers, coordinate only), how to run it (`./gradlew :data-utils:sql:consumerSmoke`), what `cannot access dev.quokkify.step.AbstractSteps` means (the module must declare `api project(":testng-extensions")`), and why the build is standalone

- [ ] **Step 1: Write the text.** Match the style of the neighbouring READMEs (find the dependency section with `grep -n "dev.quokkify" <module>/README.md`).
- [ ] **Step 2: Formatting.** Run: `npx --yes prettier@3 --check .`. Expected: `All matched files use Prettier code style!`. If needed, run `--write` on the changed `.md` files.
- [ ] **Step 3: Commit.** `git commit -m "docs: note transitive dev.quokkify:testng in steps modules"`.

### Task 4: Real-consumer check (local, not part of the repo)

**Files:** only in the scratchpad (`<scratch>`): `init.gradle`, `m2/`, logs. Do not edit anything in the consumer repository.

**Interfaces:**

- `init.gradle` reads the properties `q4jRepo`, `q4jVersion`, `dropTestng` (`true|false`). In `allprojects`, it adds `exclusiveContent` (`dev.quokkify` → `q4jRepo`) as the first repository, sets `resolutionStrategy.eachDependency { if (requested.group == 'dev.quokkify') useVersion(q4jVersion) }`, and, when `dropTestng=true`, uses `configurations.configureEach { withDependencies { removeIf { it.group == 'dev.quokkify' && it.name == 'testng' } } }`. This removes only the direct declaration.

- [ ] **Step 1: Publish both q4j states.** From a detached worktree of `origin/main` (`<baseline-worktree>`), run `./gradlew publishToMavenLocal -Pversion=0.0.0-pre825-SNAPSHOT -Dmaven.repo.local=<scratch>/m2`. From this branch, run the same with `-Pversion=0.0.0-fix825-SNAPSHOT`. Use SNAPSHOT versions so publishing does not need signing keys.
- [ ] **Step 2: Create a detached worktree of the consumer.** Run `git -C <consumer-repo> fetch origin` and `git -C <consumer-repo> worktree add --detach <consumer-worktree> origin/main`, where `<consumer-repo>` is a downstream consumer project.
- [ ] **Step 3: Run the matrix** in `test-automation/` of that worktree: `./gradlew --init-script <scratch>/init.gradle -Pq4jRepo=<scratch>/m2 -Pq4jVersion=<v> -PdropTestng=<b> compileJava compileTestJava --console=plain`, for 4 runs.
      Expected: `pre825/false`: PASS; `pre825/true`: FAIL with javac `cannot access dev.quokkify.step.AbstractSteps` (or `class file for ... not found`); `fix825/true`: PASS; `fix825/false`: PASS.
      If `pre825/false` fails, the consumer does not compile against current `main` for reasons unrelated to #825. In that case stop and report; do not fix the consumer.
- [ ] **Step 4: Record the result** in `<scratch>/consumer-matrix.md`: a 4-row table plus the javac line from the red row, without naming the project. Remove the worktrees with `git worktree remove` for the consumer worktree and the baseline.

### Task 5: Branch verification

- [ ] **Step 1: Run the whole build the way CI does.** Run: `./gradlew check -x test --console=plain` (run in background, it takes long). Expected: BUILD SUCCESSFUL, with `consumerSmoke` `SKIPPED` in modules without steps classes.
- [ ] **Step 2: Scan the changed files for placeholders.** Run: `git diff origin/main...HEAD | grep -nE 'TODO|FIXME|TBD|@Disabled|\.skip\('`. Expected: empty output.
- [ ] **Step 3:** Pass `red-sql.txt`, `redgreen-sql.txt` and `consumer-matrix.md` to `finishing-a-development-branch` for the PR description. The PR sections are Highlight, Usage example and Migration, as listed in the spec's Release section. Push and create the PR only when the user asks.
