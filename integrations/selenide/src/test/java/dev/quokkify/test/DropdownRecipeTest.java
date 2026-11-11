package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.TmsLink;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.CollectionCondition.empty;
import static com.codeborne.selenide.CollectionCondition.exactTexts;
import static com.codeborne.selenide.CollectionCondition.size;
import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Condition.partialText;
import static com.codeborne.selenide.Selectors.byRole;
import static com.codeborne.selenide.Selenide.$;

/**
 * Plain Selenide recipes for custom dropdown widgets; the README "Dropdowns" section is copied from these tests.
 */
public class DropdownRecipeTest extends BaseTest {

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

    chips.shouldHave(size(4));

    input.setValue("");
    input.pressEscape();
    for (int remaining = chips.size(); remaining > 0; remaining--) {
      chips.first().find("button").click();
      chips.shouldHave(size(remaining - 1));
    }

    input.shouldHave(Condition.exactValue(""));
    chips.shouldBe(empty);
    input.shouldHave(attribute("aria-expanded", "false"));
  }

  private static SelenideElement listboxOf(SelenideElement combobox) {
    combobox.shouldHave(attribute("aria-controls"));
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
