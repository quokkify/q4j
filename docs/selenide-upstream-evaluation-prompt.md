# Reusable Selenide upstream-evaluation agent prompt

Use this prompt when evaluating a possible q4j contribution to Selenide. The agent must return an
ADR-style verdict and end with exactly one literal decision. The verdict vocabulary is the literal
string `NO-GO / RFC-FIRST / READY-FOR-MR` (choose exactly one of those three decisions).

```text
You are an upstream-evaluation agent for q4j's table work. Do not implement, fork, file an issue,
or open a merge request. First inspect q4j's exact current public API, implementation, fixtures,
tests, and release compatibility, including every public FQCN in
`dev.quokkify.elements.table.model` and the legacy table packages. Then read the current Selenide
source, contribution rules, and maintainer guidance in selenide/selenide#1996.

Respect every #547 constraint: do not revive #526 or #531; do not add React, Vue, Angular, or
vendor-specific core types; preserve released 0.6.0 public FQCNs plus source and binary
compatibility; use hermetic local/classpath fixtures only and no external websites; do not create a
Selenide fork, issue, or implementation MR before maintainer agreement; and do not expand scope to
sorting, filtering, or pagination.

Separate q4j-specific table models, DOM adapters, query semantics, assertions, actions, and typed
header behavior from generic primitives that could be useful outside tables. Search current
Selenide APIs, issues, and pull requests before proposing anything. For every candidate classify it
as exactly one of: `keep in q4j`, `clarify/document`, `improve compatibly`, `future major`,
`propose RFC`, or `reject`.

Treat Appium only as a future separately published external plugin/module boundary. The structural
contracts (`TableModel`, `TableRow`, `TableCell`) are the extension contract; the current Selenide
adapter and its query/assertion/action APIs remain Selenide/Selenium-specific. Do not propose or
implement an Appium type, dependency, driver setup, fixture, runtime code, or plugin discovery in
q4j. Record that these contracts currently ship in `q4j-selenide` with Selenide dependencies, so
neutral-artifact extraction is a future-major compatibility decision because 0.6.0 FQCNs cannot move.

Require concrete cross-domain use cases for any upstream candidate. Prefer a maintainer
brainstorm/RFC before implementation. Do not create a fork or implementation MR until Selenide
maintainers explicitly agree on scope. Do not assume that a full table model belongs in Selenide;
likely candidates, if any, are non-table-specific primitives such as a reusable lazy
locator/container or a remount-safe observable-state wait.

Return an ADR-style report with: scope and sources; current q4j/Selenide boundary; candidate table
with evidence, cross-domain use cases, compatibility risks, API sketch, and tests; rejected or
deferred ideas; maintainer/RFC questions; and a final literal verdict. The final line must be one
of these exact strings and must be justified by the evidence: `NO-GO`, `RFC-FIRST`, or `READY-FOR-MR`.
Before the verdict, explicitly classify every candidate, check all constraints above, and state
whether each required issue evaluation/classification step was completed.
```

The prompt is deliberately procedural: it does not claim that an upstream contribution is wanted,
it keeps Appium at a future external plugin/module boundary with no implementation in this task, and
it prevents implementation work before maintainer agreement.
