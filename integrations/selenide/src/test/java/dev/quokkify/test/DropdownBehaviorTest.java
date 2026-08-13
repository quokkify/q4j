package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import dev.quokkify.page.local.DropdownPage;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class DropdownBehaviorTest extends BaseTest {

  @TmsLink("UI_ID_24")
  @Test(description = "Verify selectExact closes an already-open dropdown after selecting the exact option")
  public void testSelectExactClosesInitiallyOpenDropdown() {
    DropdownPage page = openDropdownPage();

    page.openSingleDropdown().selectExact("Apricot");

    Assertions.assertThat(page.openSingleDropdown().selectedLabel()).isEqualTo("Apricot");
    Assertions.assertThat(page.openSingleDropdown().isExpanded()).isFalse();
  }

  @TmsLink("UI_ID_25")
  @Test(description = "Verify typeAndSelectPartial opens a closed dropdown, picks the first partial match, and closes it")
  public void testTypeAndSelectPartialFromClosedDropdown() {
    DropdownPage page = openDropdownPage();

    page.closedSingleDropdown().typeAndSelectPartial("berry");

    Assertions.assertThat(page.closedSingleDropdown().selectedLabel()).isEqualTo("Blueberry");
    Assertions.assertThat(page.closedSingleDropdown().isExpanded()).isFalse();
  }

  @TmsLink("UI_ID_26")
  @Test(description = "Verify typeAndPressEnter selects the highlighted option and closes the widget")
  public void testTypeAndPressEnterSelectsHighlightedOption() {
    DropdownPage page = openDropdownPage();

    page.closedSingleDropdown().typeAndPressEnter("blue");

    Assertions.assertThat(page.closedSingleDropdown().selectedLabel()).isEqualTo("Blueberry");
    Assertions.assertThat(page.closedSingleDropdown().isExpanded()).isFalse();
  }

  @TmsLink("UI_ID_34")
  @Test(description = "Verify typeAndSelectExact selects the exact matching option and closes the widget")
  public void testTypeAndSelectExactClosesDropdown() {
    DropdownPage page = openDropdownPage();

    page.closedSingleDropdown().typeAndSelectExact("Cranberry");

    Assertions.assertThat(page.closedSingleDropdown().selectedLabel()).isEqualTo("Cranberry");
    Assertions.assertThat(page.closedSingleDropdown().isExpanded()).isFalse();
  }

  @TmsLink("UI_ID_27")
  @Test(description = "Verify selectPartial keeps widget selection isolated between two dropdown instances")
  public void testTwoWidgetIsolation() {
    DropdownPage page = openDropdownPage();

    page.openSingleDropdown().selectPartial("Apri");
    page.closedSingleDropdown().selectPartial("Cran");

    Assertions.assertThat(page.openSingleDropdown().selectedLabel()).isEqualTo("Apricot");
    Assertions.assertThat(page.closedSingleDropdown().selectedLabel()).isEqualTo("Cranberry");
  }

  @TmsLink("UI_ID_28")
  @Test(description = "Verify selectedTexts returns chip labels only and excludes visible remove-button text")
  public void testSelectedTextsReturnsOnlyLabels() {
    DropdownPage page = openDropdownPage();

    Assertions.assertThat(page.primaryMultiDropdown().chipRemoveTexts())
        .containsExactly("REMOVE ALPHA", "REMOVE BETA");
    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts())
        .containsExactly("Alpha", "Beta");
  }

  @TmsLink("UI_ID_29")
  @Test(description = "Verify selectAllExact adds multiple chips and removeSelectedPartial removes only the requested chip")
  public void testSelectAllExactAndRemoveSelectedPartial() {
    DropdownPage page = openDropdownPage();

    page.primaryMultiDropdown().selectAllExact(List.of("Gamma", "Delta"));
    page.primaryMultiDropdown().removeSelectedPartial("Gamma");

    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts())
        .containsExactly("Alpha", "Beta", "Delta");
    Assertions.assertThat(page.primaryMultiDropdown().chipRemoveTexts())
        .containsExactly("REMOVE ALPHA", "REMOVE BETA", "REMOVE DELTA");
  }

  @TmsLink("UI_ID_30")
  @Test(description = "Verify selectAllPartial works with a non-autoclosing widget and preserves the second widget state")
  public void testSelectAllPartialWithAutoCloseDisabled() {
    DropdownPage page = openDropdownPage();

    page.secondaryMultiDropdown().selectAllPartial(List.of("Omi", "Sig"));

    Assertions.assertThat(page.secondaryMultiDropdown().selectedTexts())
        .containsExactly("Omega", "Omicron", "Sigma");
    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts())
        .containsExactly("Alpha", "Beta");
  }

  @TmsLink("UI_ID_35")
  @Test(description = "Verify removeSelectedPartial treats regex metacharacters literally")
  public void testRemoveSelectedPartialTreatsRegexMetacharactersLiterally() {
    DropdownPage page = openDropdownPage();

    page.literalMultiDropdown().removeSelectedPartial("Alpha|Beta");

    Assertions.assertThat(page.literalMultiDropdown().selectedTexts())
        .containsExactly("AlphaBeta");
  }

  @TmsLink("UI_ID_37")
  @Test(description = "Verify removeSelectedPartial matches chip labels only when earlier remove-control text also contains the query")
  public void testRemoveSelectedPartialIgnoresRemoveControlTextWhenMatchingLabels() {
    DropdownPage page = openDropdownPage();

    page.adversarialMultiDropdown().removeSelectedPartial("Delta");

    Assertions.assertThat(page.adversarialMultiDropdown().selectedTexts())
        .containsExactly("Alpha");
    Assertions.assertThat(page.adversarialMultiDropdown().chipRemoveTexts())
        .containsExactly("REMOVE Delta BAIT");
  }

  @TmsLink("UI_ID_36")
  @Test(description = "Verify selectedTexts uses the default direct-text chip contract without including remove-control text")
  public void testSelectedTextsSupportsDirectTextChips() {
    DropdownPage page = openDropdownPage();

    Assertions.assertThat(page.directTextMultiDropdown().chipRemoveTexts())
        .containsExactly("REMOVE DIRECT", "REMOVE SECONDARY");
    Assertions.assertThat(page.directTextMultiDropdown().selectedTexts())
        .containsExactly("Direct Alpha", "Direct Beta");
  }

  @TmsLink("UI_ID_38")
  @Test(description = "Verify overriding the authoritative remove selector also governs direct-text label extraction and chip removal")
  public void testSelectedTextsAndRemovalHonorOverriddenRemoveSelector() {
    DropdownPage page = openDropdownPage();

    Assertions.assertThat(page.deleteSelectorMultiDropdown().chipRemoveTexts())
        .containsExactly("DELETE CONTROL ONE", "DELETE CONTROL TWO");
    Assertions.assertThat(page.deleteSelectorMultiDropdown().selectedTexts())
        .containsExactly("Delete Alpha", "Delete Beta");

    page.deleteSelectorMultiDropdown().removeSelectedPartial("Delete Alpha");

    Assertions.assertThat(page.deleteSelectorMultiDropdown().selectedTexts())
        .containsExactly("Delete Beta");
    Assertions.assertThat(page.deleteSelectorMultiDropdown().chipRemoveTexts())
        .containsExactly("DELETE CONTROL TWO");
  }

  @TmsLink("UI_ID_31")
  @Test(description = "Verify clearSelected clears a pending query, removes chips via remove controls, and waits for async removals")
  public void testClearSelectedClearsInputAndWaitsForAsyncRemoval() {
    DropdownPage page = openDropdownPage();
    page.primaryMultiDropdown().selectAllExact(List.of("Gamma", "Delta"));
    page.primaryMultiDropdown().setQuery("Del");

    page.primaryMultiDropdown().clearSelected();

    Assertions.assertThat(page.primaryMultiDropdown().inputValue()).isEmpty();
    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts()).isEmpty();
    Assertions.assertThat(page.primaryMultiDropdown().isExpanded()).isFalse();
  }

  @TmsLink("UI_ID_32")
  @Test(description = "Verify clearSelected fails fast when chip removal makes no progress")
  public void testClearSelectedFailsWhenChipRemovalMakesNoProgress() {
    DropdownPage page = openDropdownPage();

    Assertions.assertThatThrownBy(() -> page.stuckMultiDropdown().clearSelected())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("No progress while removing selected chip 'Locked'")
        .hasCauseInstanceOf(org.awaitility.core.ConditionTimeoutException.class);
  }

  @TmsLink("UI_ID_33")
  @Test(description = "Mutation guard: selectedTexts must not regress to returning chip text plus visible remove controls")
  public void testSelectedTextsMutationGuard() {
    DropdownPage page = openDropdownPage();
    String mutatedChipText = String.join(" ", page.primaryMultiDropdown().visibleChipTexts());

    Assertions.assertThat(mutatedChipText).contains("REMOVE ALPHA");
    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts())
        .allMatch(text -> !text.contains("REMOVE"));
  }

  private DropdownPage openDropdownPage() {
    return Selenide.open(dropdownFixtureDataUrl(), DropdownPage.class);
  }

  private String dropdownFixtureDataUrl() {
    InputStream resourceStream = getClass().getResourceAsStream("/dropdown/index.html");
    if (resourceStream == null) {
      throw new IllegalStateException("Dropdown fixture resource is missing");
    }
    try (InputStream fixtureStream = resourceStream) {
      String html = new String(fixtureStream.readAllBytes(), StandardCharsets.UTF_8);
      return "data:text/html;base64," + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ioException) {
      throw new IllegalStateException("Failed to load dropdown fixture", ioException);
    }
  }

}
