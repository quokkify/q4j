# Selenide Dropdown Evidence Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Re-express the user-facing dropdown scenarios in plain Selenide on the existing fixture plus a new portal widget, and record whether any repeated logic justifies a helper (phase 1 of issue #481).

**Architecture:** One new TestNG class `DropdownPlainSelenideTest` next to `DropdownBehaviorTest`, sharing the fixture `dropdown/index.html`. The fixture gains one widget, `single-portal`. No production code changes. The output is a findings table that decides between removal (phase 2a) and a thin helper (phase 2b).

**Tech Stack:** Java 21, Selenide 7.18.2, TestNG, Allure `@TmsLink`, Gradle, local `web` profile (nginx + Selenium Grid).

**Spec:** `docs/superpowers/specs/2026-11-11-selenide-dropdown-evidence-design.md`

## Global Constraints

- Branch `test/selenide-dropdown-plain-evidence`; no changes under `integrations/selenide/src/main`.
- `DropdownBehaviorTest`, `DropdownPage`, `InputDropdown`, `MultiSelectInputDropdown` stay unchanged.
- `DropdownPlainSelenideTest` may use only: `$`, `$$`, `Selectors` (including `byRole`), `find`/`findAll`, `findBy`/`filterBy`, `closest`, Selenide `Condition` / `CollectionCondition`, `setValue`, `click`, `pressEnter`, `pressEscape`, `should*` with an explicit `Duration` where noted.
- Forbidden in `DropdownPlainSelenideTest`: any `dev.quokkify.elements.*` import, `Waiter`, Awaitility, `executeJavaScript`, `Thread.sleep`, AssertJ on values read with `getText()`/`texts()` when a Selenide `should*` can express the same check.
- Helpers are `private` methods inside `DropdownPlainSelenideTest` only, so repetition stays visible.
- `@TmsLink` ids start at `UI_ID_40`, one per test, in the order given below.
- Decision rule (copied from spec): a candidate gap is logic that (1) spans more than one Selenide call or more than one line, (2) appears in at least two scenarios directly or via the same private helper, (3) has no direct Selenide equivalent. A helper that only names a locator is not a gap.
- Markdown files are formatted with `npx prettier --write <file>` before commit (CI runs `prettier --check .`).
- Commit messages: Conventional Commits, ending with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Commands

- `COMPILE` = `./gradlew :integrations:selenide:compileTestJava :integrations:selenide:checkstyleMain :integrations:selenide:checkstyleTest --console=plain` → `BUILD SUCCESSFUL`.
- `BROWSER <pattern>` = `CI= BASE_URL=http://host.docker.internal:80 NGINX_BASE_URL=http://host.docker.internal:80 BROWSER_REMOTE_URL=http://localhost:4444/wd/hub ./gradlew :integrations:selenide:test --tests '<pattern>' --console=plain`. Requires `env -u CI ./tools/environment/scripts/infra/run_app.sh web` to be up (`curl -s http://localhost:4444/wd/hub/status` shows `"ready": true`).

## Fixture facts the tests rely on

- Every widget root is `#<id>`. Input: `input[role='combobox']` with `aria-controls="<id>-listbox"` and `aria-expanded`.
- Single widgets: selected value in `.chip-row .item`; label in `.item-label` except `single-direct-text`.
- Multi widgets: chips are `.chip-row .item`; label in `.item-label` except `directText` chips (text node); remove control is a `button` whose class starts with `remove`.
- `multi-primary` removes chips after 250 ms (`pendingRemovalDelayMs`). `multi-stuck` never removes. `multi-secondary` does not auto-close.

## Review Focus

1. Async chip removal: a check that reads chips once instead of waiting would pass locally and flake in CI — M7 must fail when the wait is removed (Task 3, Step 3).
2. Label vs remove-control text: a partial match on the whole chip text picks the wrong chip in `multi-adversarial` — M5 asserts the surviving chip and its remove text.
3. Regex metacharacters in labels (`Alpha|Beta`) must match literally — M4.
4. Portal listbox removed on close: a check against `visible` on a detached listbox can pass vacuously — P1 asserts the listbox is absent from the DOM, not just hidden.
5. Two widgets on one page: option lookup not scoped to the widget's own listbox selects in the wrong widget — S5 and M3 assert the other widget is unchanged.

---

### Task 1: Portal widget and test class skeleton

**Files:**

- Modify: `integrations/selenide/src/test/resources/dropdown/index.html`
- Create: `integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java`

**Interfaces:**

- Produces: `DropdownPlainSelenideTest extends BaseTest` with `private void openFixture()` that opens `/dropdown/index.html` as a base64 `data:text/html` URL (same mechanism as `DropdownBehaviorTest.dropdownFixtureDataUrl()`, copied privately). Later tasks add tests to this class.
- Produces: fixture widget `#single-portal`, label "Portal single", options `Peach`, `Pear`, `Plum`, initially closed, nothing selected, auto-closing.

- [ ] **Step 1: Write the failing tests P1 and P2**

```java
@TmsLink("UI_ID_40")
@Test(description = "Plain Selenide: select an option from a listbox rendered in a portal outside the widget root")
public void testPortalSelectByClick() {
  // open #single-portal by clicking its combobox input
  // listbox located via the input's aria-controls id (not inside the root)
  // click option with exact text "Pear"
  // assert: #single-portal .chip-row .item .item-label has exact text "Pear"
  // assert: input has attribute aria-expanded="false"
  // assert: element #single-portal-listbox does not exist in the DOM
}

@TmsLink("UI_ID_41")
@Test(description = "Plain Selenide: type a query and select a partial match from a portal listbox")
public void testPortalTypeAndSelectPartial() {
  // setValue "Pl" on the input, click first option containing "Pl"
  // assert: selected label exact text "Plum"; aria-expanded="false"; #single-portal-listbox does not exist
}
```

- [ ] **Step 2: Run to verify they fail**

Run: `BROWSER 'dev.quokkify.test.DropdownPlainSelenideTest'`
Expected: FAIL — `ElementNotFound` for `#single-portal` (widget not in fixture yet).

- [ ] **Step 3: Add `createPortalDropdown(root, config)` to the fixture and instantiate `#single-portal`**

Add `<div id="single-portal" class="widget"></div>` to the "Single dropdowns" section. Root markup mirrors `createSingleDropdown` (label + `input[role='combobox']` with `aria-controls="single-portal-listbox"` + `.chip-row .item` with `.item-label`), but without a listbox inside the root. On open: create `<div id="single-portal-listbox" role="listbox">` with `[role='option']` children filtered by the input value, append to `document.body`. On close (Escape, selection) remove that node from the DOM and set `aria-expanded="false"`. Input `click` and `input` events open it.

- [ ] **Step 4: Run to verify they pass, and the old suite still passes**

Run: `BROWSER 'dev.quokkify.test.DropdownPlainSelenideTest'` → 2 tests PASS.
Run: `BROWSER 'dev.quokkify.test.DropdownBehaviorTest'` → 16 tests PASS.
Run: `COMPILE` → `BUILD SUCCESSFUL`.

- [ ] **Step 5: Commit**

```bash
git add integrations/selenide/src/test/resources/dropdown/index.html integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java
git commit -m "test(selenide): add portal dropdown fixture and plain Selenide tests"
```

### Task 2: Single-select scenarios S1–S5

**Files:**

- Modify: `integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java`

**Interfaces:**

- Consumes: `openFixture()` from Task 1.

These scenarios run against existing widgets, so they pass on first run; they are characterization tests. Assert with Selenide `should*`.

- [ ] **Step 1: Write S1–S5**

| Test (TMS)                                        | Action                                                                       | Assertions                                                                     |
| ------------------------------------------------- | ---------------------------------------------------------------------------- | ------------------------------------------------------------------------------ |
| `testSelectExactInOpenWidget` (`UI_ID_42`)        | `#single-open`: click option exact "Apricot" (listbox already open)          | selected label exact "Apricot"; input `aria-expanded="false"`                  |
| `testTypeAndSelectPartialFromClosed` (`UI_ID_43`) | `#single-closed`: setValue "berry", click first option containing "berry"    | selected label exact "Blueberry"; `aria-expanded="false"`                      |
| `testTypeAndPressEnter` (`UI_ID_44`)              | `#single-closed`: setValue "blue", pressEnter                                | selected label exact "Blueberry"; `aria-expanded="false"`                      |
| `testTypeAndSelectExact` (`UI_ID_45`)             | `#single-closed`: setValue "Cranberry", click option exact "Cranberry"       | selected label exact "Cranberry"; `aria-expanded="false"`                      |
| `testTwoWidgetIsolation` (`UI_ID_46`)             | `#single-open` select partial "Apri"; `#single-closed` select partial "Cran" | `#single-open` label exact "Apricot"; `#single-closed` label exact "Cranberry" |

"Closed" in the source tests includes an explicit Escape when the widget is still open after selection; reproduce only if the assertion needs it, and note it in the findings.

- [ ] **Step 2: Run**

Run: `BROWSER 'dev.quokkify.test.DropdownPlainSelenideTest'` → 7 tests PASS.

- [ ] **Step 3: Prove S1 can fail**

Temporarily change S1's expected label to "Apple", run S1, see FAIL with Selenide's text mismatch, revert.

- [ ] **Step 4: `COMPILE`, then commit**

```bash
git add integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java
git commit -m "test(selenide): cover single-select dropdown scenarios with plain Selenide"
```

### Task 3: Multi-select scenarios M1–M8

**Files:**

- Modify: `integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java`

**Interfaces:**

- Consumes: `openFixture()` from Task 1.

- [ ] **Step 1: Write M1–M8**

| Test (TMS)                                            | Widget              | Action                                                                                  | Assertions                                                                                                                                          |
| ----------------------------------------------------- | ------------------- | --------------------------------------------------------------------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| `testChipLabelsExcludeRemoveText` (`UI_ID_47`)        | `multi-primary`     | —                                                                                       | labels exactly `Alpha`, `Beta`; remove-control texts exactly `REMOVE ALPHA`, `REMOVE BETA`                                                          |
| `testSelectSeveralAndRemoveOne` (`UI_ID_48`)          | `multi-primary`     | select exact "Gamma", then "Delta"; remove chip labelled "Gamma"                        | labels exactly `Alpha`, `Beta`, `Delta`; remove texts exactly `REMOVE ALPHA`, `REMOVE BETA`, `REMOVE DELTA`                                         |
| `testSelectSeveralWithoutAutoClose` (`UI_ID_49`)      | `multi-secondary`   | select partial "Omi", then "Sig"                                                        | secondary labels exactly `Omega`, `Omicron`, `Sigma`; primary labels still exactly `Alpha`, `Beta`                                                  |
| `testRemoveChipWithRegexMetacharacters` (`UI_ID_50`)  | `multi-literal`     | remove chip whose label contains "Alpha\|Beta"                                          | labels exactly `AlphaBeta`                                                                                                                          |
| `testRemoveChipIgnoresRemoveControlText` (`UI_ID_51`) | `multi-adversarial` | remove chip whose label contains "Delta"                                                | labels exactly `Alpha`; remove texts exactly `REMOVE Delta BAIT`                                                                                    |
| `testDirectTextChipLabels` (`UI_ID_52`)               | `multi-direct-text` | —                                                                                       | labels exactly `Direct Alpha`, `Direct Beta`; remove texts exactly `REMOVE DIRECT`, `REMOVE SECONDARY`                                              |
| `testClearAllWithAsyncRemoval` (`UI_ID_53`)           | `multi-primary`     | select "Gamma", "Delta"; setValue "Del"; clear input; click every chip's remove control | input value empty; chip collection empty; `aria-expanded="false"`                                                                                   |
| `testStuckRemovalFailsWithinTimeout` (`UI_ID_54`)     | `multi-stuck`       | click remove on chip "Locked"; wait for chips to be empty with `Duration.ofMillis(500)` | the wait throws Selenide's `com.codeborne.selenide.ex.ListSizeMismatch` (or the `UIAssertionError` subtype Selenide actually raises — record which) |

"Labels" means label text without the remove-control text, for both `.item-label` chips and direct-text chips. How the test gets them is part of the evidence: write it in the most direct Selenide form and record it in Task 4.

- [ ] **Step 2: Run**

Run: `BROWSER 'dev.quokkify.test.DropdownPlainSelenideTest'` → 15 tests PASS.

- [ ] **Step 3: Prove M7 really waits**

Temporarily replace the final empty-collection `should*` in M7 with an immediate read (`$$(...).size()` asserted equal to 0), run M7, see FAIL (chips still present before 250 ms), revert. If it does not fail, the test does not exercise async removal — stop and report.

- [ ] **Step 4: `COMPILE`, then commit**

```bash
git add integrations/selenide/src/test/java/dev/quokkify/test/DropdownPlainSelenideTest.java
git commit -m "test(selenide): cover multi-select dropdown scenarios with plain Selenide"
```

### Task 4: Findings and verdict

**Files:**

- Create (not committed): `<scratchpad>/dropdown-evidence.md` — the PR body draft. The spec puts findings in the PR description and on #481, not in the repository.

**Interfaces:**

- Consumes: the finished `DropdownPlainSelenideTest` from Tasks 1–3.

- [ ] **Step 1: Check the existing layer against the portal widget (throwaway, not committed)**

Add a temporary `@FindBy(how = How.ID, using = "single-portal") TestSingleDropdown` field to `DropdownPage` and a temporary test calling `selectExact("Pear")`. Run it, record the outcome (expected: fails, because `InputDropdown` searches for the listbox inside its root). Then `git checkout -- integrations/selenide/src/test/java/dev/quokkify/page/local/DropdownPage.java` and delete the temporary test. `git status` must show no changes to those files.

- [ ] **Step 2: Write the findings draft**

Sections:

- One row per scenario (S1–S5, M1–M8, P1–P2): scenario, plain Selenide code or private helper used, line count, contributes to a candidate gap (yes/no + which).
- Candidate gaps: each one with the three decision-rule criteria checked, or "none".
- `InputDropdown` on the portal widget: outcome from Step 1.
- Verdict: phase 2a (remove) or phase 2b (thin helper, listing the gaps).
- Test plan: the commands from Step 3 with their results.

- [ ] **Step 3: Full checks**

Run: `./gradlew :integrations:selenide:check -x test --console=plain` → `BUILD SUCCESSFUL`.
Run: `BROWSER 'dev.quokkify.test.Dropdown*'` → 31 tests PASS (16 + 15).
Run: `npx prettier --check .` → no issues.

- [ ] **Step 4: Hand off for publishing**

Show the owner the findings draft as the PR description and a short #481 comment (verdict + link to the PR). Push, open the PR and post the comment only after the owner approves.
