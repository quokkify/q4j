package dev.quokkify.elements.base;

import com.codeborne.selenide.Container;
import com.codeborne.selenide.SelenideElement;

/**
 * Base class for composite UI components initialized by Selenide.
 */
public abstract class Component implements Container {

  @Self
  private SelenideElement self;

  public SelenideElement getSelf() {
    return self;
  }
}
