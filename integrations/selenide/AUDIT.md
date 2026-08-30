# Selenide table module — audit notes (work/2026-08-28)

Findings from exercising the table functionality against the local `web` infra (Selenium Grid
4.47 + nginx fixtures) and reading the table implementation. Each item records what was observed,
where, and a recommended change. Severity is relative to the module's stated contracts.

## Verified runtime behavior

- Local `web` infra run: `http://host.docker.internal:80` over HTTP from the containerized browser;
  64 table tests executed, 62 passed and 2 failed on the full run. Both failures were
  **timing-sensitive and passed in isolation** (see F7); this run supports the browser findings.
- Separate verification run: `http://localhost:80` over HTTPS was stopped by
  `ERR_SSL_PROTOCOL_ERROR` before reaching the fixture, so it provides no test evidence and does
  not support the findings. Table tests require `BASE_URL`/`NGINX_BASE_URL` reachable from the
  containerized browser; see RUNBOOK.

## Findings

### N1 — Duplicate horizontal headers remain an uncovered ambiguity edge (Low)

The former eager-validation concern is covered by
`TableQueryContractTest.horizontalTypedLookupWaitsAndRejectsDuplicates`: a delayed horizontal row
is waited for before its typed cell is read, and duplicate displayed headers are rejected. The
remaining narrow gap is a remount or header-duplication change occurring during a single polling
window; no production failure is claimed for that scenario.

The relevant horizontal path (`RowHeaderCellLocator`) still calls

```java
SelenideDomTableModel.this.columnIndex(column, resolver);   // result discarded
```

for validation. `TableModel.columnIndex(...)` (TableModel.java:21-31) throws
`TableColumnNotFoundException` when the column header is absent from `displayedHeaders()`. For a
horizontal adapter, `displayedHeaders()` is the per-row header cells of _currently mounted_ rows,
so it can be empty/partial while rows load asynchronously.

The test above is the current evidence for the supported delayed-row and duplicate-header
contracts. Any broader change to polling exception handling would need a new reproduction first.

### N2 — `TableQueryRow.cell(int)` eagerly reads cell text, breaking the lazy contract (Low-Med)

`TableQueryRow.cell(int)` (TableQueryRow.java:32-35) validates existence by reading the cell text
immediately (`cellText(columnIndex).orElseThrow(...)`) and then `IndexedCellReference.text()`
reads it again → **two DOM reads** per indexed cell access. The typed path `cell(C)` is lazy (no
eager text read). The README explicitly states references "resolve the current DOM when read".
Inconsistent and an avoidable DOM hit.

Recommended: defer the bounds check to read time (lazy), matching the typed path, or drop the
eager read.

### N3 — `SelenideDomTableModel.rows()` is a non-atomic snapshot (Low)

`rows()` (SelenideDomTableModel.java:61-81) returns an `AbstractList` whose `size()` and `get(i)`
each re-query `rowsElements()` independently. Iterating while the DOM mutates (row removed between
`size()` and `get()`) can throw `IndexOutOfBoundsException` or yield inconsistent data.

Recommended: capture a single `rowsElements()` snapshot per `size()`/iteration, or document the
non-atomic contract explicitly (the single-snapshot guarantee already exists for assertions via
`MatchingTableCondition`/`SnapshotTableCondition`).

### N4 — `uniqueRow(condition, timeout)` double scan (Low)

`SelenideTableQuery.uniqueRow(condition, timeout)` first waits via `requiredRow(...)`, then
re-scans with `uniqueRow(condition)`. A mutation between the two passes (matched row gone, or a
duplicate now present) gives surprising behavior (not-found / ambiguous) rather than a stable wait.
Acceptable, but worth a comment or a single-pass wait.

### N5 — Empty-table helpers throw unhelpful exceptions (Low)

- `BaseTable.getAnyRow()` → `random.nextInt(rows.size())` with `size()==0` throws raw
  `IllegalArgumentException`.
- `BaseClassicTable.getFirstRow()` → raw `RuntimeException("No rows found")`.

Recommended: dedicated table-specific exceptions (e.g. `TableRowNotFoundException`) for empty
tables, consistent with the rest of the module.

### N6 — `RowConditions.greaterThan` silently returns false on non-numeric text (Low)

`greaterThan` parses `BigDecimal` and returns `false` on `NumberFormatException`. Cells with
artifacts (`"$1,000"`, `"1 234"`, `"10%"`) never match, silently. Consider a dedicated exception
or documenting the strict-numeric contract.

## Infra / local-run findings

See `RUNBOOK.md` for the working local recipe. Non-obvious points worth product decisions:

### F7 — Timing-sensitive tests flake under bulk/emulation (Medium)

`TableRowWaitTest.testDynamicHorizontalTableRowAppearingWithDelayIsFound` (sub-0.5s bound on
`isRowExist` not waiting) and `TableQueryContractTest.addressesClassicTableByIndexAndTypedKey`
(a `PT2S` row lookup) failed on a full 64-test run and passed in isolation. Root cause is CPU
contention under `selenideBrowserTestLock` plus amd64-on-arm64 Rosetta emulation. Consider wider
`isRowExist` bounds and/or budgeting the 2s row lookup beyond wall-clock minimums; verify timing
assertions are robust to slow CI runners.

### F8 — Local runs dirty the git tree (Low-Med)

Local rendering now keeps `tools/environment/assets/selenium-grid/config.toml` as the
`__NETWORK__` template and writes the resolved file under the gitignored
`tools/environment/assets/selenium-grid/generated/` directory. Per-session artifacts under
`tools/environment/assets/selenium-grid/assets/` are also gitignored.

## Test-coverage gaps

- The neutral-model layer now covers delayed horizontal lookup and repeated horizontal headers in
  `TableQueryContractTest.horizontalTypedLookupWaitsAndRejectsDuplicates`.
- `tableModelContractStability` repeats all four bounded stability selections 20 times each,
  including the exact F7 methods `TableRowWaitTest.testDynamicHorizontalTableRowAppearingWithDelayIsFound`
  and `TableQueryContractTest.addressesClassicTableByIndexAndTypedKey`, in addition to delayed-row
  and remount scenarios.

## Disposition

| Finding | Status | Evidence / contract                                                                                                                                                                                                                           |
| ------- | ------ | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| N1      | Tested | Delayed horizontal lookup and duplicate displayed headers are covered by `TableQueryContractTest.horizontalTypedLookupWaitsAndRejectsDuplicates`; only a mid-poll remount/duplication race remains uncharacterized. |
| N2      | Fixed  | Indexed `TableQueryRow.cell(int)` now validates only the index sign; bounds and text are resolved when the reference is read.                                                                                                                 |
| N3      | Fixed  | `SelenideDomTableModel.rows()` captures one row-count snapshot per returned view while row handles remain lazy/remount-safe.                                                                                                                  |
| N4      | Fixed  | Timed `uniqueRow` uses one native polling condition that counts matches and returns the matching index from that same poll.                                                                                                                   |
| N5      | Fixed  | Empty legacy helpers throw typed `TableRowNotFoundException` in common, classic, and horizontal paths.                                                                                                                                        |
| N6      | Tested | `greaterThan` intentionally retains compatibility: strict `BigDecimal` parsing makes decorated/non-numeric text a non-match. `RowConditionsContractTest` covers canonical and rejected forms; no breaking predicate exception was introduced. |
| F7      | Mitigated/tested | The full 64-test run remains recorded as 62 passed/2 timing-sensitive failures; the selected stability methods are repeated 20× in `tableModelContractStability`, including both exact F7 methods. Isolation passes support timing contention, not deletion of the failures. |
| F8      | Fixed  | Local config renders to gitignored `generated/`; session assets are gitignored; the tracked template remains unchanged.                                                                                                                       |
