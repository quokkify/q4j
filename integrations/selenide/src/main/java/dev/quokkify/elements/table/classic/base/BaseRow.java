package dev.quokkify.elements.table.classic.base;

import javax.annotation.Nonnull;

import dev.quokkify.elements.base.Component;

import com.codeborne.selenide.SelenideElement;

/**
 * Abstract class to work with table row.
 */
public abstract class BaseRow<T extends Enum<T>> extends Component {

  protected static final int HTML_START_INDEX = 1;
  protected SelenideElement element;

  public BaseRow(SelenideElement element) {
    this.element = element;
  }

  @Nonnull
  @Override
  public SelenideElement getSelf() {
    return element;
  }
}
