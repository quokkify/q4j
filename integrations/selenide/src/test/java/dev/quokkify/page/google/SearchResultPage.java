package dev.quokkify.page.google;

import java.util.List;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.elements.base.Component;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.How;

@PageUrl("/google")
public class SearchResultPage implements Page {

  @FindBy(how = How.CSS, using = "#rso > div")
  private List<SearchResultBlock> searchResults;

  public int getSearchTitlesCount() {
    return searchResults.size();
  }

  public void clickOnSearchResultLinkByLinkText(String linkText) {
    searchResults.stream()
        .filter(resultBlock -> resultBlock.getTitleLinkText().startsWith(linkText))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No search result starts with '%s'".formatted(linkText)))
        .clickOnTitleLink();
  }

  public static class SearchResultBlock extends Component {

    @FindBy(how = How.TAG_NAME, using = "h3")
    private SelenideElement titleLink;

    public void clickOnTitleLink() {
      titleLink.click();
    }

    public String getTitleLinkText() {
      return titleLink.getText();
    }
  }
}
