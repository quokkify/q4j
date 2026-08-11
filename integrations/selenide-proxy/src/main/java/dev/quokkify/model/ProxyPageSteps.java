package dev.quokkify.model;

import dev.quokkify.impl.Page;
import dev.quokkify.service.ProxyBrowser;

import de.sstoehr.harreader.model.Har;
import io.qameta.allure.Step;

/**
 * Abstract class for page steps with proxy (HAR recording) support.
 *
 * @param <S> steps class
 * @param <V> verification steps class
 * @param <P> page class
 */
public abstract class ProxyPageSteps<S extends ProxyPageSteps<S, V, P>, V extends Verification<S, V, P>, P extends Page>
    extends PageSteps<S, V, P> {

  /**
   * Start proxy har recording.
   *
   * @return steps class
   */
  @Step("Start proxy har recording")
  public S startProxyHarRecording() {
    ProxyBrowser.newProxyHar();
    return (S) this;
  }

  /**
   * Stop proxy har recording.
   *
   * @return recorded har as {@link Har}
   */
  @Step("Stop proxy har recording")
  public Har stopProxyHarRecording() {
    return ProxyBrowser.endProxyHar();
  }
}
