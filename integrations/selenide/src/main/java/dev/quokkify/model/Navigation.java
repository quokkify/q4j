package dev.quokkify.model;

import java.net.URISyntaxException;
import java.util.Map;
import java.util.Objects;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Selenide;
import io.qameta.allure.Allure;

/**
 * Navigation class for opening pages according to PageObject class.
 */
public abstract class Navigation {

  private final String baseUrl;
  private BasicAuthCredentials basicAuthCredentials;

  protected Navigation(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  protected Navigation(String baseUrl, BasicAuthCredentials basicAuthCredentials) {
    this.baseUrl = baseUrl;
    this.basicAuthCredentials = basicAuthCredentials;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public Navigation setBasicAuthCredentials(BasicAuthCredentials basicAuthCredentials) {
    this.basicAuthCredentials = basicAuthCredentials;
    return this;
  }

  /**
   * Open page with url query parameters and formatted dynamic parts.
   *
   * PageUrl: 'https://example.com/id/%s' -> 'https://example.comx/id/555?id=555'.
   *
   * @param pageClass   PageObject class
   * @param queryParams url query parameters
   * @param urlParams   dynamic url parameters
   * @return PageObject class
   */
  protected <T extends Page> T openPage(Class<T> pageClass, Map<String, Object> queryParams, Object... urlParams) {
    String fullPageUrl;
    try {
      fullPageUrl = NavigationUrlUtils.addQueryParameters(getPageUrl(pageClass, urlParams), queryParams);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return openPage(fullPageUrl, pageClass);
  }

  /**
   * Open page with formatted url dynamic parts.
   *
   * PageUrl: 'https://example.com/id/%s' -> 'https://example.comx/id/555'.
   *
   * @param pageClass PageObject class
   * @param urlParams dynamic url parameters
   * @return PageObject class
   */
  protected <T extends Page> T openPage(Class<T> pageClass, Object... urlParams) {
    String fullPageUrl = getPageUrl(pageClass, urlParams);
    return openPage(fullPageUrl, pageClass);
  }

  /**
   * Open page with url query parameters.
   *
   * PageUrl: 'https://example.com' -> 'https://example.com?id=555'.
   *
   * @param pageClass   PageObject class
   * @param queryParams url query parameters
   * @return PageObject class
   */
  protected <T extends Page> T openPage(Class<T> pageClass, Map<String, Object> queryParams) {
    String fullPageUrl;
    try {
      fullPageUrl = NavigationUrlUtils.addQueryParameters(getPageUrl(pageClass), queryParams);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return openPage(fullPageUrl, pageClass);
  }

  /**
   * Open page using full page url.
   *
   * @param fullPageUrl full page url as {@link String}
   */
  protected void openPage(String fullPageUrl) {
    if (Objects.nonNull(basicAuthCredentials)) {
      Selenide.open(
          fullPageUrl,
          basicAuthCredentials.domain(),
          basicAuthCredentials.login(),
          basicAuthCredentials.password());
    } else {
      Selenide.open(fullPageUrl);
    }
  }

  private <T extends Page> T openPage(String fullPageUrl, Class<T> pageClass) {
    Allure.step("Open page by url: '%s'".formatted(fullPageUrl));
    return Objects.nonNull(basicAuthCredentials)
        ? Selenide.open(
        fullPageUrl,
        basicAuthCredentials.domain(),
        basicAuthCredentials.login(),
        basicAuthCredentials.password(),
        pageClass)
        : Selenide.open(fullPageUrl, pageClass);
  }

  /**
   * Get page url with given url params.
   *
   * @param pageClass page class
   * @param urlParams url params
   * @param <T>       like {@link Page}
   * @return page url path with params
   */
  private <T extends Page> String getPageUrl(Class<T> pageClass, Object... urlParams) {
    String pagePath = pageClass.getAnnotation(PageUrl.class).value();
    return baseUrl.concat(pagePath.formatted(urlParams));
  }
}
