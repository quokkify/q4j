package dev.quokkify.flutter.comparison;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Condenses a UiAutomator2 page source to the nodes a person scanning it would read: those with a resource id, a
 * content description or text, one compact line each.
 */
public final class NativeExcerpt {

  private NativeExcerpt() {
  }

  public static String of(String pageSource, int maxLines) {
    List<String> lines = meaningfulNodes(pageSource);
    List<String> kept = new ArrayList<>(lines.subList(0, Math.min(maxLines, lines.size())));
    if (lines.size() > maxLines) {
      kept.add("(" + (lines.size() - maxLines) + " more nodes)");
    }
    return String.join("\n", kept);
  }

  private static List<String> meaningfulNodes(String pageSource) {
    List<String> lines = new ArrayList<>();
    try {
      SAXParserFactory factory = SAXParserFactory.newInstance();
      factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
      factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
      SAXParser parser = factory.newSAXParser();
      parser.parse(new InputSource(new StringReader(pageSource)), new DefaultHandler() {
        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes) {
          String line = line(qName, attributes);
          if (line != null) {
            lines.add(line);
          }
        }
      });
    } catch (ParserConfigurationException | SAXException e) {
      return List.of("(page source could not be parsed: " + e.getMessage() + ")");
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return lines;
  }

  private static String line(String qName, Attributes attributes) {
    String id = attributes.getValue("resource-id");
    String desc = attributes.getValue("content-desc");
    String text = attributes.getValue("text");
    if (isEmpty(id) && isEmpty(desc) && isEmpty(text)) {
      return null;
    }
    StringBuilder line = new StringBuilder(qName.substring(qName.lastIndexOf('.') + 1));
    append(line, "id", id);
    append(line, "desc", desc);
    append(line, "text", text);
    String bounds = attributes.getValue("bounds");
    if (!isEmpty(bounds)) {
      line.append(' ').append(bounds);
    }
    return line.toString();
  }

  private static void append(StringBuilder line, String name, String value) {
    if (!isEmpty(value)) {
      line.append(' ').append(name).append("=\"").append(value).append('"');
    }
  }

  private static boolean isEmpty(String value) {
    return value == null || value.isEmpty();
  }
}
