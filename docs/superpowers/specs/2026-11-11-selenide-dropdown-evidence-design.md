# Selenide dropdown layer: evidence before removal — design

Status: draft for review. Date: 2026-11-11. Module: `integrations/selenide`. Issue: #481.

## Context

After PR #480, `integrations/selenide` still ships two public dropdown classes in
`dev.quokkify.elements.single.dropdown`:

- `InputDropdown` (126 lines): `typeAndSelectExact`, `typeAndSelectPartial`, `selectExact`,
  `selectPartial`, `typeAndPressEnter`, plus protected selector hooks.
- `MultiSelectInputDropdown` (103 lines): `selectAllExact`, `selectAllPartial`, `clearSelected`,
  `selectedTexts`, `removeSelectedPartial`, plus a JavaScript fallback for chip labels and an
  Awaitility wait for chip removal.

They have no production call sites in the repository and no public downstream users found on
GitHub. Their only consumers are the test-only `DropdownPage` and `DropdownBehaviorTest`
(16 scenarios, `UI_ID_24`–`UI_ID_39`) on the fixture `src/test/resources/dropdown/index.html`.
Several of those scenarios test the abstraction's own mechanics (label fallback, remove-selector
override, a mutation guard) rather than user behaviour.

The table stack was replaced in a similar situation (see `2026-10-09-thin-table-design.md`). That
decision was backed by evidence: PR #726 rewrote the table tests in plain Selenide and showed
exactly three gaps that tests had to re-implement by hand. This design applies the same method
to dropdowns before anything is removed.

`elements/base/Component` is out of scope. It is a general base for Selenide `Container` page
components and is used by other test page objects (`SearchBlock`, `OrdersPanel`, `DataTable`).

## Goal

Decide, from code rather than opinion, whether the dropdown layer covers any gap that plain
Selenide does not, and then either remove the layer or replace it with a helper limited to the
proven gaps.

Success criteria for phase 1:

- Every user-facing scenario listed under "Scenarios" passes with plain Selenide only, or its
  failure to do so is recorded as a finding.
- The PR description and a comment on #481 contain the findings table described under
  "Decision rule", with a verdict.
- No production code changes in the phase 1 PR.
- `./gradlew :integrations:selenide:check -x test` passes, and `DropdownPlainSelenideTest` and
  `DropdownBehaviorTest` pass on the local `web` profile.

## Non-goals

- Designing a new dropdown API in this phase.
- Native `<select>` elements: Selenide already provides `selectOption*`.
- Virtualised option lists, async option loading from a server, keyboard-only navigation beyond
  Enter and Escape.
- Changes to `Component`, `Container`, or other modules.

## Phase 1 — evidence

### Branch and PR

Branch `test/selenide-dropdown-plain-evidence` from `main`. One PR with test code, fixture
changes and this spec. `DropdownBehaviorTest` and the dropdown classes stay untouched so both
suites run side by side on the same fixture.

### Fixture: portal widget

Add one widget `single-portal` to `dropdown/index.html`:

- The root contains an `input[role='combobox']` with `aria-controls` pointing to the listbox id
  and `aria-expanded` reflecting the state.
- On open, a `[role='listbox']` with `[role='option']` children is appended to `<body>`, outside
  the widget root. On close or selection it is removed from the DOM.
- The selected value renders inside the root, like the existing single widgets.

This covers the most common real-world layout (react-select, MUI, Headless UI render menus into a
portal) and the only plausible gap found during brainstorming: locating the listbox through
`aria-controls`. The existing `InputDropdown` searches only inside its root and is expected to
fail on this widget; that is recorded, not fixed.

### Test class

`dev.quokkify.test.DropdownPlainSelenideTest extends BaseTest`, opened through the same data URL
mechanism as `DropdownBehaviorTest`. Allowed API: `$`, `$$`, `byRole` and other `Selectors`,
`find`/`findAll`, `findBy`/`filterBy`, `closest`, Selenide conditions and collection conditions,
`setValue`, `click`, `pressEnter`, `pressEscape`. Not allowed: `dev.quokkify.elements.*`,
`Waiter`, Awaitility, `executeJavaScript`, `Thread.sleep`.

Page-object helpers are allowed only as private methods inside the test class, so any
repetition stays visible for the decision rule. Each test gets a new `@TmsLink` id starting at
`UI_ID_40`.

### Scenarios

Ported from `DropdownBehaviorTest` (user-facing behaviour):

| #   | Scenario                                                            | Widget(s)                          | Source test                                                           |
| --- | ------------------------------------------------------------------- | ---------------------------------- | --------------------------------------------------------------------- |
| S1  | Select exact option in an initially open widget, widget ends closed | `single-open`                      | `testSelectExactClosesInitiallyOpenDropdown`                          |
| S2  | Type query, select first partial match, widget ends closed          | `single-closed`                    | `testTypeAndSelectPartialFromClosedDropdown`                          |
| S3  | Type query and press Enter selects the highlighted option           | `single-closed`                    | `testTypeAndPressEnterSelectsHighlightedOption`                       |
| S4  | Type query and select exact option, widget ends closed              | `single-closed`                    | `testTypeAndSelectExactClosesDropdown`                                |
| S5  | Selection in one widget does not change another                     | two single widgets                 | `testTwoWidgetIsolation`                                              |
| M1  | Read selected chip labels without remove-control text               | `multi-primary`                    | `testSelectedTextsReturnsOnlyLabels`                                  |
| M2  | Select several options, remove one chip, others stay                | `multi-primary`                    | `testSelectAllExactAndRemoveSelectedPartial`                          |
| M3  | Select several options in a non-autoclosing widget                  | `multi-secondary`, `multi-primary` | `testSelectAllPartialWithAutoCloseDisabled`                           |
| M4  | Remove a chip whose label contains regex metacharacters             | `multi-literal`                    | `testRemoveSelectedPartialTreatsRegexMetacharactersLiterally`         |
| M5  | Remove a chip by label when remove-control text also matches        | `multi-adversarial`                | `testRemoveSelectedPartialIgnoresRemoveControlTextWhenMatchingLabels` |
| M6  | Read labels of direct-text chips                                    | `multi-direct-text`                | `testSelectedTextsSupportsDirectTextChips`                            |
| M7  | Clear pending query and all chips, chip removal is asynchronous     | `multi-primary`                    | `testClearSelectedClearsInputAndWaitsForAsyncRemoval`                 |
| M8  | Removal that never completes fails within the configured timeout    | `multi-stuck`                      | `testClearSelectedFailsWhenChipRemovalMakesNoProgress`                |

New:

| #   | Scenario                                                                  | Widget          |
| --- | ------------------------------------------------------------------------- | --------------- |
| P1  | Open, select an option from the portal listbox, value shown, listbox gone | `single-portal` |
| P2  | Type query, select partial match from the portal listbox                  | `single-portal` |

For M8 the plain version asserts that a Selenide `should*` with a short explicit timeout fails
with Selenide's own error; the custom `IllegalStateException` wrapper is not reproduced.

Not ported, because they test the abstraction's internals and have no meaning without it:
`testSelectedTextsAndRemovalHonorOverriddenRemoveSelector`,
`testSingleDropdownFallbackReturnsFullSelectedText`, `testSelectedTextsMutationGuard`. Their
fixture widgets (`multi-delete-selector`, `single-direct-text`) remain in place for
`DropdownBehaviorTest`.

### Decision rule

Fixed before the tests are written. A **candidate gap** is a piece of logic that:

1. spans more than one Selenide call or more than one line,
2. appears in at least two scenarios (directly or through the same private helper), and
3. has no direct equivalent in Selenide's public API.

The phase 1 PR records a table with one row per scenario: the plain Selenide code (or the
private helper used), line count, and whether it contributes to a candidate gap. The verdict is:

- **No candidate gaps** → phase 2a (removal).
- **One or more candidate gaps** → phase 2b (thin helper), limited to those gaps.

Private helpers that only name a locator (for example `chips()` returning `$$(...)`) are not
candidate gaps; they are ordinary page-object code.

## Phase 2a — removal (if no gaps)

Separate PR, `feat(selenide)!:` with a `BREAKING CHANGE:` footer, no deprecation period, matching
the table precedent for the 0.x line.

| Removed                                             | Replacement                         |
| --------------------------------------------------- | ----------------------------------- |
| `elements/single/dropdown/InputDropdown`            | plain Selenide in the page object   |
| `elements/single/dropdown/MultiSelectInputDropdown` | plain Selenide in the page object   |
| `page/local/DropdownPage` (test)                    | —                                   |
| `DropdownBehaviorTest` (test)                       | trimmed `DropdownPlainSelenideTest` |
| Fixture widgets used only by removed tests          | —                                   |

`DropdownPlainSelenideTest` is trimmed to the scenarios that serve as migration examples: S2
(single select), M2 (multi select and chip removal), M7 (async clear) and P1 (portal). The
fixture is reduced to the widgets those tests use. `integrations/selenide/README.md` gets a
"Dropdowns" section next to "Tables" with short single-select and multi-select/chips examples
taken from those tests, and a migration table from each removed public method to its Selenide
equivalent.

## Phase 2b — thin helper (if gaps)

A separate spec, written after phase 1, scoped to the recorded gaps only and following the
`Table` pattern: a small type that returns `SelenideElement` / `ElementsCollection`, no
selection workflow methods, no inheritance hooks. The existing classes are removed in the same
breaking release.

## Risks

- **Fixture realism.** The fixture is hand-written; a gap that only appears with a specific UI
  library will not show up. The portal widget reduces this risk for the most common layout.
  Downstream users who hit such a gap can raise it after removal.
- **Hidden downstream users.** Only public GitHub code was searched. The breaking change is
  announced through release notes with the migration table.
