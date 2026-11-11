package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ListSizeMismatch;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactOwnText;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.partialText;
import static com.codeborne.selenide.Selectors.byRole;
import static com.codeborne.selenide.Selenide.$;

/**
 * The dropdown scenarios of {@link DropdownBehaviorTest} expressed with plain Selenide only, as evidence for
 * issue #481.
 */
public class DropdownPlainSelenideTest extends BaseTest {

  @TmsLink("UI_ID_40")
  @Test(description = "Plain Selenide: select an option from a listbox rendered in a portal outside the widget root")
  public void testPortalSelectByClick() {
    openFixture();
    SelenideElement widget = $("#single-portal");
    SelenideElement input = widget.find(byRole("combobox"));

    input.click();
    listboxOf(input).findAll(byRole("option")).findBy(exactText("Pear")).click();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Pear"));
    input.shouldHave(attribute("aria-expanded", "false"));
    $("#single-portal-listbox").shouldNot(Condition.exist);
  }

  @TmsLink("UI_ID_41")
  @Test(description = "Plain Selenide: type a query and select a partial match from a portal listbox")
  public void testPortalTypeAndSelectPartial() {
    openFixture();
    SelenideElement widget = $("#single-portal");
    SelenideElement input = widget.find(byRole("combobox"));

    input.setValue("Pl");
    listboxOf(input).findAll(byRole("option")).findBy(partialText("Pl")).click();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Plum"));
    input.shouldHave(attribute("aria-expanded", "false"));
    $("#single-portal-listbox").shouldNot(Condition.exist);
  }

  @TmsLink("UI_ID_42")
  @Test(description = "Plain Selenide: select an exact option in an initially open single dropdown, which then closes")
  public void testSelectExactInOpenWidget() {
    openFixture();
    SelenideElement widget = $("#single-open");

    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Apricot")).click();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Apricot"));
    widget.find(byRole("combobox")).shouldHave(attribute("aria-expanded", "false"));
  }

  @TmsLink("UI_ID_43")
  @Test(description = "Plain Selenide: type a query into a closed single dropdown and select the first partial match")
  public void testTypeAndSelectPartialFromClosed() {
    openFixture();
    SelenideElement widget = $("#single-closed");

    widget.find(byRole("combobox")).setValue("berry");
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("berry")).click();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Blueberry"));
    widget.find(byRole("combobox")).shouldHave(attribute("aria-expanded", "false"));
  }

  @TmsLink("UI_ID_44")
  @Test(description = "Plain Selenide: type a query and press Enter to accept the highlighted option")
  public void testTypeAndPressEnter() {
    openFixture();
    SelenideElement widget = $("#single-closed");

    widget.find(byRole("combobox")).setValue("blue").pressEnter();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Blueberry"));
    widget.find(byRole("combobox")).shouldHave(attribute("aria-expanded", "false"));
  }

  @TmsLink("UI_ID_45")
  @Test(description = "Plain Selenide: type a query and select the exact matching option")
  public void testTypeAndSelectExact() {
    openFixture();
    SelenideElement widget = $("#single-closed");

    widget.find(byRole("combobox")).setValue("Cranberry");
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Cranberry")).click();

    widget.find(".chip-row .item .item-label").shouldHave(exactText("Cranberry"));
    widget.find(byRole("combobox")).shouldHave(attribute("aria-expanded", "false"));
  }

  @TmsLink("UI_ID_46")
  @Test(description = "Plain Selenide: selections in two single dropdowns on one page stay isolated")
  public void testTwoWidgetIsolation() {
    openFixture();
    SelenideElement openWidget = $("#single-open");
    SelenideElement closedWidget = $("#single-closed");

    openWidget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("Apri")).click();
    closedWidget.find(byRole("combobox")).click();
    closedWidget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("Cran")).click();

    openWidget.find(".chip-row .item .item-label").shouldHave(exactText("Apricot"));
    closedWidget.find(".chip-row .item .item-label").shouldHave(exactText("Cranberry"));
  }

  @TmsLink("UI_ID_47")
  @Test(description = "Plain Selenide: chip labels are read without the text of their remove controls")
  public void testChipLabelsExcludeRemoveText() {
    openFixture();
    SelenideElement widget = $("#multi-primary");

    widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("Alpha", "Beta"));
    widget.findAll(".chip-row .item button").shouldHave(exactTexts("REMOVE ALPHA", "REMOVE BETA"));
  }

  @TmsLink("UI_ID_48")
  @Test(description = "Plain Selenide: select several options, then remove one chip by its label")
  public void testSelectSeveralAndRemoveOne() {
    openFixture();
    SelenideElement widget = $("#multi-primary");

    widget.find(byRole("combobox")).click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Gamma")).click();
    widget.find(byRole("combobox")).click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Delta")).click();
    widget.findAll(".chip-row .item .item-label").findBy(exactText("Gamma")).closest(".item").find("button").click();

    widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("Alpha", "Beta", "Delta"));
    widget.findAll(".chip-row .item button").shouldHave(exactTexts("REMOVE ALPHA", "REMOVE BETA", "REMOVE DELTA"));
  }

  @TmsLink("UI_ID_49")
  @Test(description = "Plain Selenide: select several partial matches in a dropdown that stays open")
  public void testSelectSeveralWithoutAutoClose() {
    openFixture();
    SelenideElement widget = $("#multi-secondary");

    widget.find(byRole("combobox")).click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("Omi")).click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(partialText("Sig")).click();

    widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("Omega", "Omicron", "Sigma"));
    $("#multi-primary").findAll(".chip-row .item .item-label").shouldHave(exactTexts("Alpha", "Beta"));
  }

  @TmsLink("UI_ID_50")
  @Test(description = "Plain Selenide: a chip label with regex metacharacters is matched literally")
  public void testRemoveChipWithRegexMetacharacters() {
    openFixture();
    SelenideElement widget = $("#multi-literal");

    widget.findAll(".chip-row .item .item-label").findBy(partialText("Alpha|Beta")).closest(".item").find("button")
        .click();

    widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("AlphaBeta"));
  }

  @TmsLink("UI_ID_51")
  @Test(description = "Plain Selenide: remove a chip by label when another chip's remove control contains the query")
  public void testRemoveChipIgnoresRemoveControlText() {
    openFixture();
    SelenideElement widget = $("#multi-adversarial");

    widget.findAll(".chip-row .item .item-label").findBy(partialText("Delta")).closest(".item").find("button").click();

    widget.findAll(".chip-row .item .item-label").shouldHave(exactTexts("Alpha"));
    widget.findAll(".chip-row .item button").shouldHave(exactTexts("REMOVE Delta BAIT"));
  }

  @TmsLink("UI_ID_52")
  @Test(description = "Plain Selenide: labels of chips rendered as direct text exclude the remove control text")
  public void testDirectTextChipLabels() {
    openFixture();
    ElementsCollection chips = $("#multi-direct-text").findAll(".chip-row .item");

    chips.shouldHave(size(2));
    chips.get(0).shouldHave(exactOwnText("Direct Alpha"));
    chips.get(1).shouldHave(exactOwnText("Direct Beta"));
    chips.get(0).find("button").shouldHave(exactText("REMOVE DIRECT"));
    chips.get(1).find("button").shouldHave(exactText("REMOVE SECONDARY"));
  }

  @TmsLink("UI_ID_53")
  @Test(description = "Plain Selenide: clear a pending query and remove every chip while removal is asynchronous")
  public void testClearAllWithAsyncRemoval() {
    openFixture();
    SelenideElement widget = $("#multi-primary");
    SelenideElement input = widget.find(byRole("combobox"));
    ElementsCollection chips = widget.findAll(".chip-row .item");
    input.click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Gamma")).click();
    input.click();
    widget.find(byRole("listbox")).findAll(byRole("option")).findBy(exactText("Delta")).click();
    input.setValue("Del");

    input.clear();
    input.pressEscape();
    for (int remaining = chips.shouldHave(size(4)).size(); remaining > 0; remaining--) {
      chips.first().find("button").click();
      chips.shouldHave(size(remaining - 1));
    }

    input.shouldHave(Condition.exactValue(""));
    chips.shouldBe(empty);
    input.shouldHave(attribute("aria-expanded", "false"));
  }

  @TmsLink("UI_ID_54")
  @Test(description = "Plain Selenide: waiting for a chip removal that never happens fails within the given timeout")
  public void testStuckRemovalFailsWithinTimeout() {
    openFixture();
    ElementsCollection chips = $("#multi-stuck").findAll(".chip-row .item");

    chips.findBy(Condition.text("Locked")).find("button").click();

    Assertions.assertThatThrownBy(() -> chips.shouldBe(empty, Duration.ofMillis(500)))
        .isInstanceOf(ListSizeMismatch.class);
  }

  private static SelenideElement listboxOf(SelenideElement combobox) {
    return $(By.id(combobox.getAttribute("aria-controls")));
  }

  private void openFixture() {
    InputStream resourceStream = getClass().getResourceAsStream("/dropdown/index.html");
    if (resourceStream == null) {
      throw new IllegalStateException("Dropdown fixture resource is missing");
    }
    try (InputStream fixtureStream = resourceStream) {
      String html = new String(fixtureStream.readAllBytes(), StandardCharsets.UTF_8);
      Selenide.open("data:text/html;base64,"
          + Base64.getEncoder().encodeToString(html.getBytes(StandardCharsets.UTF_8)));
    } catch (IOException ioException) {
      throw new IllegalStateException("Failed to load dropdown fixture", ioException);
    }
  }
}
