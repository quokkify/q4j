package dev.quokkify.page.w3school;

import dev.quokkify.impl.Page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

public class BaseTablePage implements Page {

  @FindBy(how = How.ID, using = "accept-choices")
  private SelenideElement acceptTermsButton;

  public void clickAcceptTermsButtonIfDisplayed() {
    if (acceptTermsButton.is(Condition.appear)) {
      acceptTermsButton.click();
    }
  }
}
