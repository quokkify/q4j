# Selenide table module — audit notes (work/2026-08-28)

Findings from exercising the table functionality against the local `web` infra (Selenium Grid
4.47 + nginx fixtures) and reading the table implementation. Each item records what was observed,
where, and a recommended change. Severity is relative to the module's stated contracts.

## Verified runtime behavior

- Local `web` infra brought up (nginx + Selenium Grid + Chrome 151). 64 table tests executed;
  62 passed, 2 failed on a full run. Both failures were **timing-sensitive and passed in
  isolation** → load/emulation flakiness, not deterministic bugs (see F7).
- Table tests require `BASE_URL`/`NGINX_BASE_URL` reachable **from the containerized browser**
  (`http://host.docker.internal:80`), not `localhost` — see RUNBOOK.

## Findings

### N1 — Horizontal `cell(C)` eager validation escapes the async wait (High)

`SelenideDomTableModel.SelenideRow.cell(C)` (horizontal branch, `RowHeaderCellLocator`) calls

```java
SelenideDomTableModel.this.columnIndex(column, resolver);   // result discarded
```

purely for its validation side effect. `TableModel.columnIndex(...)` (TableModel.java:21-31)
throws `TableColumnNotFoundException` — `extends RuntimeException`, **not**
`NoSuchElementException` — when the column header is absent from `displayedHeaders()`. For a
horizontal adapter, `displayedHeaders()` is the per-row header cells of _currently mounted_ rows,
so it is empty/partial while rows load asynchronously.

`MatchingTableCondition.check` (SelenideDomTableModel.java) catches only
`NoSuchElementException | StaleElementReferenceException`. A `TableColumnNotFoundException`
therefore propagates straight out of the condition poll: `requiredRow(condition, timeout)` in
`SelenideTableQuery` aborts with the exception instead of retrying until the header appears.

This is the same class of bug as the legacy `DynamicHorizontalTable` regression (UI_ID_10,
"column-index lookup throwing before the header renders"), which was fixed in the legacy path
but is **re-introduced unguarded in the query model**. No query-model test covers an async
horizontal table, so it is unexercised.

Recommended: in the horizontal `cell(C)` path, return `Optional.empty()` when the header is not
currently present table-wide, instead of throwing; or catch `TableColumnNotFoundException` in the
wait loops and treat a missing header as "not matched yet".

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
- `tableModelContractStability` repeats both timing-sensitive methods identified in F7, in addition
  to the existing delayed-row and remount scenarios.

## Disposition

| Finding | Status     | Evidence / contract                                                                                                                                                                                                                  |
| ------- | ---------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| N1      | Fixed      | Horizontal row lookup returns empty until the row header is mounted and table-wide duplicate headers raise `TableColumnAmbiguousException`; `TableQueryContractTest` covers both paths. |
| N2      | Fixed      | Indexed `TableQueryRow.cell(int)` now validates only the index sign; bounds and text are resolved when the reference is read.                                                                                                        |
| N3      | Fixed      | `SelenideDomTableModel.rows()` captures one row-count snapshot per returned view while row handles remain lazy/remount-safe.                                                                                                         |
| N4      | Fixed      | Timed `uniqueRow` uses one native polling condition that counts matches and returns the matching index from that same poll.                                                                                                          |
| N5      | Fixed      | Empty legacy helpers throw typed `TableRowNotFoundException` in common, classic, and horizontal paths.                                                                                                                               |
| N6      | Tested     | `greaterThan` intentionally retains compatibility: strict `BigDecimal` parsing makes decorated/non-numeric text a non-match. `RowConditionsContractTest` covers canonical and rejected forms; no breaking predicate exception was introduced. |
| F7      | Fixed      | Existing timing-sensitive browser tests remain bounded and are repeated 20× by `tableModelContractStability`; no sleeps were added. Full browser/grid stability remains a CI gate. |
| F8      | Fixed      | Local config renders to gitignored `generated/`; session assets are gitignored; the tracked template remains unchanged.                                                                                                              |
