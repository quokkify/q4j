package dev.quokkify.ex;

/**
 * Exception thrown when there is an error during HTML parsing.
 * Specifically, when no HTML node is found by the provided locator.
 */
public class ParserHtmlException extends RuntimeException {

  /**
   * Constructs a new ParserHtmlException with a detailed error message.
   *
   * @param locator The locator used to search for the HTML node.
   * @param html    The HTML document in which the search was performed.
   */
  public ParserHtmlException(String locator, String html) {
    super("Html parsing error - no node found by locator '%s'%nHtml document:%n%s".formatted(locator, html));
  }
}
