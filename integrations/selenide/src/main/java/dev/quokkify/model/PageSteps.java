package dev.quokkify.model;

import java.util.function.Supplier;

import dev.quokkify.impl.Page;
import dev.quokkify.step.AbstractSteps;

import com.codeborne.selenide.Selenide;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.qameta.allure.Step;

/**
 * Abstract class for page steps class.
 *
 * @param <S> steps class
 * @param <V> verification steps class
 * @param <P> page class
 */
@SuppressFBWarnings({"UWF_UNWRITTEN_PUBLIC_OR_PROTECTED_FIELD", "UUF_UNUSED_PUBLIC_OR_PROTECTED_FIELD"})
public abstract class PageSteps<S extends PageSteps<S, V, P>, V extends Verification<S, V, P>, P extends Page>
    extends AbstractSteps<V> {

  protected V verification;
  protected P page;

  @Override
  public V verify() {
    return verification;
  }

  /**
   * Refresh current page.
   *
   * @return steps class
   */
  @Step("Refresh page")
  public S refreshPage() {
    Selenide.refresh();
    return (S) this;
  }

  /**
   * Back to previous page.
   *
   * @param stepFactory factory creating the expected page steps
   * @param <T>         like {@link PageSteps}
   * @return expected page steps
   */
  @Step("Back to previous page")
  public <T extends PageSteps<?, ?, ?>> T backToPreviousPage(Supplier<T> stepFactory) {
    Selenide.back();
    return stepFactory.get();
  }

  /**
   * Navigate to specific page.
   *
   * @param navigationSteps expected navigation steps
   * @param <T>             like {@link Navigation}
   * @return navigation page steps
   */
  public <T extends Navigation> T navigate(T navigationSteps) {
    return navigationSteps;
  }
}
