package dev.quokkify.elements.table.classic.base;

import java.io.File;
import javax.annotation.Nonnull;

import dev.quokkify.elements.base.Component;
import dev.quokkify.html.model.HtmlAttribute;
import dev.quokkify.html.model.HtmlTag;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

/**
 * Abstract class to work with table cell.
 */
public abstract class BaseCell extends Component {

  protected static final String TAG_A_WITH_TITLE_OR_TEXT_XPATH_FORMAT = ".//a[@title='%s' or text()='%s']";
  protected static final String TAG_A_WITH_SPAN_WITH_TITLE_OR_TEXT_XPATH_FORMAT =
      ".//a[.//span[@title='%s' or text()='%s']]";
  protected SelenideElement element;

  public BaseCell(SelenideElement element) {
    this.element = element;
  }

  /**
   * Click clink in cell.
   */
  public void clickLink() {
    getLink().click();
  }

  /**
   * Click link in cell by given link title or text.
   *
   * @param linkTitleOrText expected link title or text
   */
  public void clickLink(String linkTitleOrText) {
    getLink(linkTitleOrText).click();
  }

  /**
   * Get link value from cell.
   *
   * @return link as {@link String}
   */
  public String getLinkValue() {
    return getLink().getAttribute(HtmlAttribute.HREF);
  }

  /**
   * Get attribute value from link web element.
   *
   * @param attributeName attribute name ex: title
   * @return attribute value as {@link String}
   */
  public String getLinkAttribute(String attributeName) {
    return getLink().getAttribute(attributeName);
  }

  /**
   * Get link element from cell.
   *
   * @return {@link SelenideElement} element
   */
  public SelenideElement getLink() {
    return getSelf().find(By.tagName(HtmlTag.A));
  }

  /**
   * Get link element from cell by given link title or text.
   *
   * @param linkTitleOrText expected link title or text
   * @return {@link SelenideElement} element
   */
  public SelenideElement getLink(String linkTitleOrText) {
    return getSelf().find(By.xpath(TAG_A_WITH_TITLE_OR_TEXT_XPATH_FORMAT.formatted(linkTitleOrText, linkTitleOrText)));
  }

  /**
   * Click button in cell by given button title or text.
   *
   * @param buttonTitleOrText expected button title or text
   */
  public void clickButton(String buttonTitleOrText) {
    getButton(buttonTitleOrText).click();
  }

  /**
   * Get button element from cell by given button title or text.
   *
   * @param buttonTitleOrText expected button title or text
   * @return {@link SelenideElement} element
   */
  public SelenideElement getButton(String buttonTitleOrText) {
    return getSelf().find(
        By.xpath(TAG_A_WITH_SPAN_WITH_TITLE_OR_TEXT_XPATH_FORMAT.formatted(buttonTitleOrText, buttonTitleOrText)));
  }

  /**
   * Select checkbox in cell.
   */
  public void selectCheckbox() {
    getCheckbox().click();
  }

  /**
   * Get checkbox element from cell.
   *
   * @return {@link SelenideElement} element
   */
  public SelenideElement getCheckbox() {
    return getSelf().find(By.cssSelector("[type='checkbox']"));
  }

  /**
   * Download file by link.
   */
  public File download() {
    return getDownloadLink().download();
  }

  /**
   * Get download link element from cell.
   *
   * @return {@link SelenideElement} element
   */
  public SelenideElement getDownloadLink() {
    return getSelf().find(By.cssSelector("a[href*='download']"));
  }

  /**
   * Get inner text.
   */
  public String innerText() {
    return getSelf().innerText();
  }

  /**
   * Get table row.
   *
   * @param <T> enum with columns enumerations
   * @return {@link BaseRow} element
   */
  public abstract <T extends Enum<T>> BaseRow getRow();

  @Nonnull
  @Override
  public SelenideElement getSelf() {
    return element;
  }
}
