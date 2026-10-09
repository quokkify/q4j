# Selenide table module — audit notes (work/2026-08-28)

Findings from exercising the table functionality against the local `web` infra (Selenium Grid
4.47 + nginx fixtures) and reading the table implementation. Each item records what was observed,
where, and a recommended change. Severity is relative to the module's stated contracts.

> **Removed code.** The legacy table stack (classic, horizontal and model packages) was replaced by `Table`,
> `TableRow`, `TableLayout`, `HorizontalTable` and `TableColumnException` in `dev.quokkify.elements.table`; see
> [`docs/table-api.md`](../../../docs/table-api.md). Findings N1-N6 described that stack and were dropped with it.

## Verified runtime behavior

- Local `web` infra run: `http://host.docker.internal:80` over HTTP from the containerized browser;
  64 table tests executed, 62 passed and 2 failed on the full run. Both failures were
  **timing-sensitive and passed in isolation** (see F7); this run supports the browser findings.
- Separate verification run: `http://localhost:80` over HTTPS was stopped by
  `ERR_SSL_PROTOCOL_ERROR` before reaching the fixture, so it provides no test evidence and does
  not support the findings. Table tests require `BASE_URL`/`NGINX_BASE_URL` reachable from the
  containerized browser; see [`RUNBOOK.md`](RUNBOOK.md).

## Infra / local-run findings

See [`RUNBOOK.md`](RUNBOOK.md) for the working local recipe. Non-obvious points worth product decisions:

### F7 — Timing-sensitive tests flake under bulk/emulation (Medium)

`TableRowWaitTest.testDynamicHorizontalTableRowAppearingWithDelayIsFound` (sub-0.5s bound on a
non-waiting row check) and `TableQueryContractTest.addressesClassicTableByIndexAndTypedKey`
(a `PT2S` row lookup) failed on a full 64-test run and passed in isolation. Both methods remain, now
exercising the thin `Table` / `HorizontalTable` helpers. Root cause is CPU
contention under `selenideBrowserTestLock` plus amd64-on-arm64 Rosetta emulation. Consider wider
`isRowExist` bounds and/or budgeting the 2s row lookup beyond wall-clock minimums; verify timing
assertions are robust to slow CI runners.

### F8 — Local runs dirty the git tree (Low-Med)

Local rendering now keeps `tools/environment/assets/selenium-grid/config.toml` as the
`__NETWORK__` template and writes the resolved file under the gitignored
`tools/environment/assets/selenium-grid/generated/` directory. Per-session artifacts under
`tools/environment/assets/selenium-grid/assets/` are also gitignored.

## Test-coverage gaps

- `TableTest` and `HorizontalTableTest` cover the `Table` and `HorizontalTable` behaviours (delayed rows,
  remount, header reordering, ambiguous and missing headers).
- `tableModelContractStability` repeats its bounded stability selections 20 times each.

## Disposition

| Finding | Status           | Evidence / contract                                                                                                                                                                                                                                                          |
| ------- | ---------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| F7      | Mitigated/tested | The full 64-test run remains recorded as 62 passed/2 timing-sensitive failures; the selected stability methods are repeated 20× in `tableModelContractStability`, including both exact F7 methods. Isolation passes support timing contention, not deletion of the failures. |
| F8      | Fixed            | Local config renders to gitignored `generated/`; session assets are gitignored; the tracked template remains unchanged.                                                                                                                                                      |
