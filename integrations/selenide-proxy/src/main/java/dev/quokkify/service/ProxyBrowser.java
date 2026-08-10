package dev.quokkify.service;

import java.util.Map;
import java.util.UUID;

import com.browserup.bup.BrowserUpProxy;
import com.browserup.bup.proxy.CaptureType;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import de.sstoehr.harreader.model.Har;

/**
 * Extension of {@link Browser} with BrowserUpProxy support (HAR recording, request interception).
 */
public class ProxyBrowser extends Browser {

  protected ProxyBrowser() {
  }

  /**
   * Open browser and add proxy request headers.
   *
   * @param requestHeaders proxy filters request headers
   */
  public static void openBrowserAndAddProxyRequestHeaders(Map<String, String> requestHeaders) {
    Selenide.open();
    addProxyRequestHeaders(requestHeaders);
  }

  /**
   * Add proxy request filter headers.
   *
   * @param headers request filter headers as {@link Map}&lt;{@link String}, {@link String}&gt;
   */
  public static void addProxyRequestHeaders(Map<String, String> headers) {
    getProxy().addHeaders(headers);
  }

  /**
   * Enable har recording.
   */
  public static void newProxyHar() {
    BrowserUpProxy browserUpProxy = getProxy();
    browserUpProxy.setHarCaptureTypes(CaptureType.getAllContentCaptureTypes());
    browserUpProxy.enableHarCaptureTypes(CaptureType.REQUEST_CONTENT, CaptureType.RESPONSE_CONTENT);
    browserUpProxy.newHar(UUID.randomUUID().toString());
  }

  /**
   * Get browser proxy recorded har.
   *
   * @return browser har as {@link Har}
   */
  public static Har getProxyHar() {
    return getProxy().getHar();
  }

  /**
   * Disable har recording.
   *
   * @return previous recorded har as {@link Har}
   */
  public static Har endProxyHar() {
    return getProxy().endHar();
  }

  /**
   * Get browser proxy.
   *
   * @return browser proxy as {@link BrowserUpProxy}
   */
  public static BrowserUpProxy getProxy() {
    return WebDriverRunner.getSelenideProxy().getProxy();
  }
}
