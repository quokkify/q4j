package dev.quokkify.page.google;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.constant.StringConstant;
import dev.quokkify.elements.base.Component;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

@PageUrl(StringConstant.SLASH)
public class HomePage implements Page {

  @FindBy(how = How.XPATH, using = "//button/following-sibling::button")
  private SelenideElement acceptCookiesButton;
  @FindBy(how = How.CSS, using = "form")
  private SearchBlock searchBlock;

  public void clickAcceptCookiesButtonIfDisplayed() {
    if (acceptCookiesButton.is(Condition.appear)) {
      acceptCookiesButton.click();
    }
  }

  private static class SearchBlock extends Component {

    @FindBy(how = How.NAME, using = "q")
    private SelenideElement searchInput;
  }

  public SearchResultPage searchText(String searchText) {
    searchBlock.searchInput.sendKeys(searchText);
    searchBlock.searchInput.pressEnter();
    return Selenide.page(SearchResultPage.class);
  }
}
