package dev.quokkify.test;

import java.io.File;

import dev.quokkify.annotation.SingleThread;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.FileDownloadMode;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import static com.codeborne.selenide.Selectors.byText;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.files.FileFilters.withName;

public class UiFileDownloadTest extends BaseTest {
  private static final String DOWNLOAD_PAGE = "/uploads/";
  private static final String FILE_NAME = "hello_grid_world.txt";
  private static final String FILE_CONTENT = "Hello, Selenium Grid!";

  @AfterMethod(alwaysRun = true)
  public void resetDownloadConfiguration() {
    Configuration.fileDownload = FileDownloadMode.FOLDER;
    Configuration.proxyEnabled = false;
  }

  @SingleThread
  @Test(description = "Download nginx file through HTTP")
  public void testHttpDownload() {
    verifyDownload(FileDownloadMode.HTTPGET);
  }

  @SingleThread
  @Test(description = "Download nginx file through CDP")
  public void testCdpDownload() {
    verifyDownload(FileDownloadMode.CDP);
  }

  @SingleThread
  @Test(description = "Download nginx file through proxy")
  public void testProxyDownload() {
    verifyDownload(FileDownloadMode.PROXY);
  }

  private void verifyDownload(FileDownloadMode mode) {
    Configuration.fileDownload = mode;
    Configuration.proxyEnabled = mode == FileDownloadMode.PROXY;

    Selenide.open(APP_CONFIG.downloadBrowserBaseUrl() + DOWNLOAD_PAGE);
    SelenideElement downloadLink = $(byText("Download me"));
    if (mode == FileDownloadMode.HTTPGET) {
      Selenide.executeJavaScript(
          "arguments[0].href = arguments[1]",
          downloadLink,
          APP_CONFIG.downloadHttpBaseUrl() + DOWNLOAD_PAGE + "files/" + FILE_NAME);
    }
    File downloadedFile = downloadLink.download(withName(FILE_NAME));

    Assertions.assertThat(downloadedFile)
        .hasName(FILE_NAME)
        .content()
        .isEqualToIgnoringNewLines(FILE_CONTENT);
  }
}
