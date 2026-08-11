package dev.quokkify.service;

import java.util.Objects;

import dev.quokkify.config.BrowserConfiguration;
import dev.quokkify.config.ConfigRegistry;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;

/**
 * Project-level browser configuration. For everything else, use Selenide's static API directly.
 */
public class Browser {

  private static final BrowserConfiguration CONFIG = ConfigRegistry.get(BrowserConfiguration.class);

  protected Browser() {
  }

  /**
   * Set default configurations to browser.
   */
  public static void setDefaultConfigurations() {
    Configuration.browser = CONFIG.browser();
    Configuration.browserSize = CONFIG.browserSize();
    Configuration.reportsFolder = "build/reports/tests";
    Configuration.downloadsFolder = "build/downloads";
    Configuration.fileDownload = FileDownloadMode.FOLDER;
    Configuration.screenshots = false;
  }

  /**
   * Set remote configurations to browser.
   */
  public static void setRemoteDefaultConfiguration() {
    Configuration.remote = CONFIG.remoteUrl();
    Configuration.browserCapabilities.setCapability("se:downloadsEnabled", true);
    Configuration.proxyHost = CONFIG.proxyHost();
  }

  /**
   * Check that it is remote connection.
   *
   * @return true or false
   */
  public static boolean isRemote() {
    return Objects.nonNull(Configuration.remote);
  }

  /**
   * Merge two {@link Capabilities} together and return the union of the two as a new {@link Capabilities} instance.
   * Capabilities from {@code other} will override those in {@code this}.
   *
   * @param capabilities {@link Capabilities} from selenium lib
   * @return {@link MutableCapabilities} mutable obj
   */
  public static MutableCapabilities mergeCapabilities(Capabilities capabilities) {
    return Configuration.browserCapabilities.merge(capabilities);
  }
}
