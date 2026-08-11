package dev.quokkify.test;

import java.io.File;

import dev.quokkify.service.Browser;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.files.FileFilters.withName;

public class UiGridFileDownloadTest {
  private static final String DOWNLOAD_PAGE = "/uploads/";
  private static final String FILE_NAME = "hello_grid_world.txt";
  private static final String FILE_CONTENT = "Hello, Selenium Grid!";

  @BeforeSuite(alwaysRun = true)
  public void configureBrowser() {
    Browser.setDefaultConfigurations();
    Configuration.headless = true;
    if (System.getenv("BROWSER_REMOTE_URL") != null) {
      Browser.setRemoteDefaultConfiguration();
    }
  }

  @AfterMethod(alwaysRun = true)
  public void closeWebDriver() {
    Configuration.fileDownload = FileDownloadMode.FOLDER;
    Selenide.closeWebDriver();
  }

  @Test(description = "Download nginx file through CDP on Selenium Grid")
  public void testCdpDownload() {
    Configuration.fileDownload = FileDownloadMode.CDP;
    Selenide.open(environment("DOWNLOAD_BROWSER_BASE_URL", "http://localhost") + DOWNLOAD_PAGE);

    File downloadedFile = $(byText("Download me")).download(withName(FILE_NAME));

    Assertions.assertThat(downloadedFile)
        .hasName(FILE_NAME)
        .content()
        .isEqualToIgnoringNewLines(FILE_CONTENT);
  }

  private static String environment(String name, String defaultValue) {
    return System.getenv().getOrDefault(name, defaultValue);
  }
}
