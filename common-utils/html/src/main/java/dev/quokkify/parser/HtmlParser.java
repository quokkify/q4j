package dev.quokkify.parser;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.DomSerializer;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.TagNode;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * Utility class for parsing HTML content and extracting elements using XPath expressions.
 *
 * <p>Example usage:</p>
 * <pre>
 *     String htmlContent = "&lt;div&gt;&lt;p&gt;Hello, World!&lt;/p&gt;&lt;/div&gt;";
 *     String xpath = "//p";
 *     Node node = HtmlParser.getHtmlNode(htmlContent, xpath);
 * </pre>
 */
public class HtmlParser {

  private static final Logger LOG = LogManager.getLogger(HtmlParser.class);

  private HtmlParser() {
  }

  /**
   * Parses the provided HTML string and extracts a {@link Node} that matches the given XPath expression.
   *
   * @param outerHtml    the raw HTML string to be parsed
   * @param xpathLocator the XPath expression used to locate the desired HTML element
   * @return the {@link Node} matching the XPath expression, or {@code null} if no matching node is found
   */
  public static Node getHtmlNode(String outerHtml, String xpathLocator) {
    TagNode tagNode = new HtmlCleaner().clean(outerHtml);
    try {
      Document doc = new DomSerializer(new CleanerProperties()).createDOM(tagNode);
      XPath xpath = XPathFactory.newInstance().newXPath();
      return (Node) xpath.evaluate(xpathLocator, doc, XPathConstants.NODE);
    } catch (XPathExpressionException | ParserConfigurationException e) {
      LOG.error(e);
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Unescapes HTML entities (e.g., &amp;lt;, &amp;gt;, &amp;amp;).
   */
  public static String unescapeHtml(String source) {
    if (source == null) return null;
    return StringEscapeUtils.unescapeHtml4(source);
  }

  /**
   * Escapes characters into HTML entities.
   */
  public static String escapeHtml(String source) {
    if (source == null) return null;
    return StringEscapeUtils.escapeHtml4(source);
  }
}
