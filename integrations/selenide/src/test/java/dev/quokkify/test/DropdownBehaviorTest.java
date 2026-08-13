package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

import dev.quokkify.page.local.DropdownPage;

import com.codeborne.selenide.Selenide;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class DropdownBehaviorTest extends BaseTest {

  private static final Duration CLEAR_SELECTED_TIMEOUT = Duration.ofSeconds(5);

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

  @TmsLink("UI_ID_31")
  @Test(description = "Verify clearSelected clears a pending query, removes chips via remove controls, and waits for async removals")
  public void testClearSelectedClearsInputAndWaitsForAsyncRemoval() {
    DropdownPage page = openDropdownPage();
    page.primaryMultiDropdown().selectAllExact(List.of("Gamma", "Delta"));
    page.primaryMultiDropdown().setQuery("Del");

    Instant start = Instant.now();
    page.primaryMultiDropdown().clearSelected();

    Assertions.assertThat(page.primaryMultiDropdown().inputValue()).isEmpty();
    Assertions.assertThat(page.primaryMultiDropdown().selectedTexts()).isEmpty();
    Assertions.assertThat(page.primaryMultiDropdown().isExpanded()).isFalse();
    Assertions.assertThat(Duration.between(start, Instant.now()))
        .isBetween(Duration.ofMillis(800), CLEAR_SELECTED_TIMEOUT.plusSeconds(1));
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
    String mutatedChipText = String.join(" ", openDropdownPage().primaryMultiDropdown().visibleChipTexts());

    Assertions.assertThat(mutatedChipText).contains("REMOVE ALPHA");
    Assertions.assertThat(openDropdownPage().primaryMultiDropdown().selectedTexts())
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
