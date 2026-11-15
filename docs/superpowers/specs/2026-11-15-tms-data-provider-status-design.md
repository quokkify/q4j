# TMS: one result per case for repeated invocations

## Intent

A test that runs several times in one execution (data provider rows, `invocationCount`) reports every
invocation to the TMS as a separate result. The run case shows the status of the last result received, so
a passing last row hides earlier failures.

Reproduced on TestPlanIt 1.1.22: a data provider with rows `failed, failed, passed` linked to one case
left the run case `passed` with three attempts.

## Goals

- A case linked to a repeatedly invoked test gets exactly one result per execution, with the worst status
  of its invocations.
- The result says which invocations failed and with which parameters.
- Works for every TMS provider without provider changes.

## Non-goals

- Mapping a data provider row to its own case.
- Changing how retries are reported: only the final attempt of a retried invocation counts, as today.
- Merging results of different test methods linked to the same case.

## Anti-goals

- A failure that is reported late or not at all because the run was interrupted before the flush.

## Approaches

1. **Aggregate in `TmsListener`, worst status wins (chosen).** One result per case, provider-agnostic,
   deterministic under parallel rows.
2. Keep per-row attempts and make the TestPlanIt provider refuse to improve the case status. Fixes only
   one provider and depends on the order in which parallel rows arrive.
3. One case per row. Needs a row-to-case mapping in the test code; a separate feature.

## Design

Scope: `integrations/tms/testng` (`TmsListener`), its tests, and the two READMEs that document the
current behaviour.

- A method is _repeated_ when it has a data provider or `invocationCount > 1`. Results of any other
  method are reported immediately, exactly as today.
- Results of a repeated method are collected per test method and case id instead of being reported.
- The collected results are reported when the test class finishes (`IClassListener.onAfterClass`), one
  `TmsResult` per case id, so results are not held until the end of a single-`<test>` run. When the enclosing
  `<test>` finishes (`ITestListener.onFinish(ITestContext)`), and at `onExecutionFinish` before `finishRun`,
  anything still collected is reported, so an aborted run does not lose results. A failure while reporting is
  logged and never prevents `finishRun`.
- A method is repeated also when it runs with `invocationCount > 1` on a thread pool: TestNG runs those
  invocations on clones with an invocation count of 1, so the count is read from the original method of the
  test context.
- Invocations are not collected while the run is unavailable.
- Status precedence: `FAILED` > `SKIPPED` > `PASSED`.
- `elapsed` is the sum of the invocations.
- Comment: a summary line followed by one line per non-passed invocation, and per invocation that carries a
  comment (a failure tolerated by `successPercentage`, reported as `PASSED`), in invocation order:

  ```text
  3 invocations: 2 failed, 1 passed
  [1] FAILED (row-1, false): row-1 failed
  [2] FAILED (row-2, false): row-2 failed
  ```

  Parameters are rendered with `String.valueOf` when the invocation is collected; a parameter whose
  `toString()` throws gets a fallback text. Line breaks are collapsed and the parameter list is truncated to a
  fixed length without cutting a surrogate pair. The summary reads `1 invocation` or `N invocations`. When
  every invocation passed without a comment, the comment is the summary line only.

- `README.md` of `tms-testng` and of `testplanit`: replace "separate attempts, last one wins" with the
  aggregated behaviour. Shards that share `TESTPLANIT_RUN_ID` still add one attempt each; that stays
  documented.

## Tests

Regression guard in `TmsListenerTestNgRunTest`, run through a real nested TestNG with the recording
provider:

- data provider `failed, failed, passed` → one `FAILED` result with both failures in the comment;
- data provider with all rows passed → one `PASSED` result;
- `invocationCount = 3` with one failure → one `FAILED` result;
- a plain method is still reported once, immediately.

## Left to the builder

- Internal structure of the collector and the exact truncation length.
- Thread-safety details for parallel data providers.
