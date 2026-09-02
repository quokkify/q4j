package dev.quokkify.model;

import dev.quokkify.impl.Page;
import dev.quokkify.impl.StepCreator;
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
    extends AbstractSteps<V> implements StepCreator {

  protected V verification;
  protected P page;

  @Override
  public V verify() {
    return verification;
  }

  public V verify(TimeoutOptions... options) {
    V result = verify();
    java.util.Objects.requireNonNull(options, "timeout options must not be null");
    for (TimeoutOptions option : options) {
      result.apply(java.util.Objects.requireNonNull(option, "timeout option must not be null"));
    }
    return result;
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
   * @param stepClass steps class
   * @param <T>       like {@link PageSteps}
   * @return expected page steps
   */
  @Step("Back to previous page")
  public <T extends PageSteps<?, ?, ?>> T backToPreviousPage(Class<T> stepClass) {
    Selenide.back();
    return getPageSteps(stepClass);
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
