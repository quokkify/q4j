package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import io.qameta.allure.TmsLink;
import org.openqa.selenium.By;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Condition.attribute;
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
