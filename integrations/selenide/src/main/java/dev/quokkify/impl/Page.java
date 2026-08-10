package dev.quokkify.impl;

import dev.quokkify.annotation.PageTitle;
import dev.quokkify.annotation.PageUrl;
import dev.quokkify.formatter.RegexFormatter;
import dev.quokkify.parser.RegexParser;

import com.codeborne.selenide.WebDriverRunner;

/**
 * Interface for Ui page class.
 */
public interface Page {

  /**
   * Get title of page.
   *
   * @param args dynamic parts in url
   * @return title as {@link String}
   */
  default String getTitle(Object... args) {
    return getTitle().formatted(args);
  }

  /**
   * Get title of page.
   *
   * @return title as {@link String}
   */
  default String getTitle() {
    return getClass().getAnnotation(PageTitle.class).value();
  }

  /**
   * Get url pattern with dynamic parts.
   *
   * @param args dynamic parts in url
   * @return pattern as {@link String}
   */
  default String getUrlPattern(Object... args) {
    return RegexFormatter.formatToAllMatchPatternWithPrefix(getUrl(args));
  }

  /**
   * Get url pattern.
   *
   * @return pattern as {@link String}
   */
  default String getUrlPattern() {
    return RegexFormatter.formatToAllMatchPatternWithPrefix(RegexFormatter.formatDigitsFormattedParts(getUrl()));
  }

  /**
   * Get url with dynamic parts.
   *
   * @param args dynamic parts in url
   * @return url with dynamic parts as {@link String}
   */
  default String getUrl(Object... args) {
    return getUrl().formatted(args);
  }

  /**
   * Get url.
   *
   * @return url as {@link String}
   */
  default String getUrl() {
    return getClass().getAnnotation(PageUrl.class).value();
  }

  /**
   * Get id from url.
   *
   * @return id from url as {@link String}
   */
  default String getIdFromUrl() {
    String pageUrl = WebDriverRunner.url();
    return RegexParser.parse("(?<=\\/)\\d+", pageUrl, "Can not find id value from url: %s".formatted(pageUrl));
  }
}
