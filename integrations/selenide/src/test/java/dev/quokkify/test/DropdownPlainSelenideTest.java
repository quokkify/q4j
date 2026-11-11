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
