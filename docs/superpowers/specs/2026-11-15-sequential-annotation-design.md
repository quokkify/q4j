# `@Sequential` annotation — design

## Intent

`SuiteListener` splits a run into two generated TestNG tests named `Concurrency` and `Sequential`. The
annotation that sends a method to the second one is called `@SingleThread`, so the name in the code differs
from the name in the report. It is also easy to confuse with TestNG's own `@Test(singleThreaded = true)`,
which means something else (all methods of one class on one thread).

Rename the annotation to `@Sequential` so the code and the generated suite use the same word.

## Goals

- A new `dev.quokkify.annotation.Sequential` annotation with the same retention and target as `@SingleThread`.
- `@SingleThread` keeps working and is marked `@Deprecated(forRemoval = true)` with a pointer to `@Sequential`.
- `SuiteListener` puts a method into the `Sequential` test when it carries either annotation.
- The repository's own tests and the `testng-extensions` README use `@Sequential`.

## Non-goals

- Renaming the `SINGLE_THREAD_TESTS_IN_PARALLEL` option or `TestNGExtension.singleThreadTestsInParallel()`.
  It is a separate public contract (environment variable); renaming it needs its own alias and migration.
- Renaming the protected `SuiteListener.ThreadGroup` enum or its constants. Subclasses can reference them.
- Changing the generated test names `Concurrency` and `Sequential`.
- Editing `CHANGELOG.md` history or older documents under `docs/superpowers/`.
- Removing `@SingleThread`. That happens in a later breaking release.

## Anti-goal

Consumer code that uses `@SingleThread`, or a `SuiteListener` subclass that overrides
`isMethodSingleThread`, stops compiling or changes behaviour after this release.

## Approaches

1. **Add `@Sequential`, deprecate `@SingleThread` (chosen).** Both annotations work for a transition period.
   Consumers get a deprecation warning instead of a compile error; the release is a plain `feat`.
2. **Hard rename.** Less code, but every consumer breaks until they change the import, and the release
   becomes `feat!`. Rejected by the owner in favour of approach 1.
3. **Keep `@SingleThread` and rename the generated test to match.** Rejected: it changes report output and
   keeps the name that collides with TestNG's `singleThreaded`.

## Design

- `Sequential.java`: `@Retention(RUNTIME)`, `@Target(METHOD)`, Javadoc describing that the test runs in the
  `Sequential` block, one test at a time.
- `SingleThread.java`: add `@Deprecated(forRemoval = true)` and a Javadoc `@deprecated` tag linking to
  `Sequential`.
- `SuiteListener`:
  - new `protected boolean isMethodSequential(Method)` — true when the method has `@Sequential` or when
    `isMethodSingleThread(method)` is true;
  - `isMethodSingleThread(Method)` keeps its current body (checks `@SingleThread` only) and is deprecated,
    so an existing override in a subclass is still honoured;
  - `getXmlIncludeTests` calls `isMethodSequential`.
- Tests: replace `@SingleThread` with `@Sequential` in the repository's test classes. Add a regression
  test in `testng-extensions` proving that a method annotated with the deprecated `@SingleThread` and a
  method annotated with `@Sequential` both land in the `Sequential` generated test, and that a method
  with neither lands in `Concurrency`.
- README (`testng-extensions`): the example, the section about running alongside parallel tests and the
  Key API table use `@Sequential`; one line states that `@SingleThread` is deprecated. The option name
  stays as is.
- Release: `feat(testng-extensions): add @Sequential and deprecate @SingleThread`. The PR description
  fills the Highlight, Usage example and Migration sections (migration: replace the import and the
  annotation name; no behaviour change).

## Left to the builder

- Whether `@Deprecated` gets a `since` value: follow the existing convention in the repository.
- How to silence the deprecation warning for the listener's own use of `SingleThread`, in line with the
  build's compiler settings.
- Names of test classes and fixtures that mention "single thread": leave them unless they read wrongly
  after the annotation swap.
