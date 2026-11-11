# Selenide Dropdown Layer Removal Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `InputDropdown` / `MultiSelectInputDropdown` and their test scaffolding, and leave README recipes plus a few recipe tests as the migration path (phase 2a of issue #481).

**Architecture:** Pure removal plus documentation. The evidence test class from PR #869 is renamed to `DropdownRecipeTest` and trimmed to the scenarios that back the README recipes. The fixture keeps only the widgets those tests use.

**Tech Stack:** Java 21, Selenide 7.18.2, TestNG, Gradle, local `web` profile.

**Spec:** `docs/superpowers/specs/2026-11-11-selenide-dropdown-evidence-design.md` (section "Phase 2a — removal"). Owner decision: phase 2a, recipes in README, no helper.

## Global Constraints

- Branch `feat/selenide-remove-dropdown-layer`, created from `test/selenide-dropdown-plain-evidence` (stacked on PR #869). PR base: `test/selenide-dropdown-plain-evidence`. Retarget to `main` after #869 merges.
- Breaking change: PR title and squash commit are `feat(selenide)!: remove dropdown component layer` with a `BREAKING CHANGE:` footer. No deprecation period.
- `elements/base/Component` and `Container` stay.
- No new public types in `src/main`.
- PR body fills `## Highlight`, `## Usage example` and `## Migration` (not `## Breaking change`), as required by `.github/scripts/enrich_release_notes.py`.
- Markdown is formatted with `npx prettier --write`. Commits end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Commands

- `COMPILE` = `./gradlew :integrations:selenide:compileTestJava :integrations:selenide:checkstyleMain :integrations:selenide:checkstyleTest --console=plain` → `BUILD SUCCESSFUL`.
- `BROWSER <pattern>` = as in `2026-11-11-selenide-dropdown-evidence.md` (local `web` profile must be up).

## Review Focus

1. A README recipe that does not compile or differs from its backing test misleads every migrating user. Each recipe is copied from a kept test method, and the test name is cited next to it.
2. A dangling reference to a removed type in docs, Javadoc or archgate rules breaks the docs build or misleads readers: `git grep` must be empty (Task 1, Step 4).
3. Fixture JS left half-trimmed can throw on load, and then every recipe test fails: Task 1 runs the recipe tests after trimming.

---

### Task 1: Remove the layer and trim tests and fixture

**Files:**

- Delete: `integrations/selenide/src/main/java/dev/quokkify/elements/single/` (whole package: `dropdown/InputDropdown.java`, `dropdown/MultiSelectInputDropdown.java`)
- Delete: `integrations/selenide/src/test/java/dev/quokkify/page/local/DropdownPage.java`
- Delete: `integrations/selenide/src/test/java/dev/quokkify/test/DropdownBehaviorTest.java`
- Rename: `DropdownPlainSelenideTest.java` → `integrations/selenide/src/test/java/dev/quokkify/test/DropdownRecipeTest.java`
- Modify: `integrations/selenide/src/test/resources/dropdown/index.html`

**Interfaces:**

- Produces: `DropdownRecipeTest` with exactly these methods (TMS ids kept): `testTypeAndSelectPartialFromClosed` (`UI_ID_43`), `testSelectSeveralAndRemoveOne` (`UI_ID_48`), `testClearAllWithAsyncRemoval` (`UI_ID_53`), `testPortalSelectByClick` (`UI_ID_40`). Plus private `listboxOf` and `openFixture`. Task 2 copies its recipes from these methods.

- [ ] **Step 1: Delete the files listed above, rename the test class, delete every other test method in it, and update its Javadoc** to "Plain Selenide recipes for custom dropdown widgets; the README "Dropdowns" section is copied from these tests."

- [ ] **Step 2: Trim the fixture.** Keep the widgets `single-closed`, `single-portal` and `multi-primary`. Remove the other widget containers and their `create*Dropdown(...)` calls. Remove config branches that no remaining widget uses: `directSelectedText`, `initiallyOpen`/`highlightedText` handling if unused, `refuseRemoval`, chip `directText`, `backspaceRemovesLast`, `autoClose: false`. Keep `root.__dropdown` only if a test reads it (none do), otherwise remove it.

- [ ] **Step 3: Run**

Run: `COMPILE` → `BUILD SUCCESSFUL`.
Run: `BROWSER 'dev.quokkify.test.DropdownRecipeTest'` → 4 tests PASS.

- [ ] **Step 4: Check that no references remain**

Run: `git grep -n -E "InputDropdown|MultiSelectInputDropdown|DropdownPage|DropdownBehaviorTest|DropdownPlainSelenideTest|elements\.single" -- . ':!CHANGELOG.md' ':!**/CHANGELOG.md' ':!docs/superpowers/**'` → no output.

- [ ] **Step 5: Commit**

```bash
git add -A integrations/selenide
git commit -m "feat(selenide)!: remove dropdown component layer" -m "BREAKING CHANGE: InputDropdown and MultiSelectInputDropdown are removed. Use plain Selenide; see the Dropdowns section of the selenide README." -m "Refs #481"
```

### Task 2: README "Dropdowns" section

**Files:**

- Modify: `integrations/selenide/README.md` (new `## Dropdowns` section right after `## Tables`)

**Interfaces:**

- Consumes: the `DropdownRecipeTest` methods from Task 1.

- [ ] **Step 1: Write the section.** One intro sentence: custom dropdowns need no library type, because selection, chips and waiting are plain Selenide. Then four recipes, each a fenced Java block copied from its test (assertions may be shortened to one line), with "Backed by `DropdownRecipeTest#<method>`" under it:
  1. Single select: type and pick (`testTypeAndSelectPartialFromClosed`).
  2. Chips: pick several, remove one by label (`testSelectSeveralAndRemoveOne`).
  3. Clear all chips when removal is asynchronous (`testClearAllWithAsyncRemoval`), with a note that the per-removal `shouldHave(size(..))` is what makes it safe.
  4. Listbox in a portal (`testPortalSelectByClick`): `listboxOf` with the `aria-controls` wait, and a note that some widgets set `aria-controls` only while open.

  Then a migration table with one row per removed public method (`typeAndSelectExact`, `typeAndSelectPartial`, `selectExact`, `selectPartial`, `typeAndPressEnter`, `selectAllExact`, `selectAllPartial`, `clearSelected`, `selectedTexts`, `removeSelectedPartial`) mapping it to its Selenide equivalent. Add one line: page objects that extended `InputDropdown` should extend `dev.quokkify.elements.base.Component` and use these recipes on `getSelf()`.

- [ ] **Step 2: Verify the recipes match their tests**

For each recipe, compare it line by line with the cited test method; locators and calls must be identical apart from shortened assertions.
Run: `npx prettier --write integrations/selenide/README.md && npx prettier --check integrations/selenide/README.md` → clean.

- [ ] **Step 3: Full checks**

Run: `./gradlew :integrations:selenide:check -x test --console=plain` → `BUILD SUCCESSFUL`.
Run: `BROWSER 'dev.quokkify.test.*'` → no failures beyond those already failing on `main`; record any pre-existing failure by name.

- [ ] **Step 4: Commit**

```bash
git add integrations/selenide/README.md
git commit -m "docs(selenide): add dropdown recipes and migration table" -m "Refs #481"
```

- [ ] **Step 5: Hand off.** Draft the PR body: Highlight (one bold sentence), Usage example (single-select recipe), Migration (the method table, inside `> [!WARNING]`), test plan, `Closes #481`. Dry-run `render_notes` per the release-notes convention. Push and open the PR after the owner approves.
