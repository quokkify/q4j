package dev.quokkify.flutter.device;

import dev.quokkify.flutter.FlutterBy;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.WebElementCondition;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * The same widget lookups expressed for each {@link MobileMode}.
 *
 * <p>Natively, Flutter exposes a {@code Semantics(identifier:)} as the resource-id and the text of a widget as its
 * content-desc ({@code text} stays empty).
 */
public final class ModeLocators {

  private static final String CONTENT_DESC = "content-desc";
  private static final String UI_SELECTOR = "new UiSelector()";

  private final MobileMode mode;

  public ModeLocators(MobileMode mode) {
    this.mode = mode;
  }

  public MobileMode mode() {
    return mode;
  }

  public By id(String id) {
    return mode.isFlutter() ? FlutterBy.key(id) : AppiumBy.id(id);
  }

  public By label(String label) {
    return mode.isFlutter() ? FlutterBy.semanticsLabel(label) : AppiumBy.accessibilityId(label);
  }

  public By text(String text) {
    return mode.isFlutter()
        ? FlutterBy.text(text)
        : AppiumBy.androidUIAutomator("new UiSelector().description(" + quote(text) + ")");
  }

  /**
   * The element that carries the enabled state of the tagged button {@code id}. The id sits on a wrapper in both
   * modes: the Flutter server reads {@code enabled} from the widget itself (the {@code ElevatedButton} under the
   * keyed {@code KeyedSubtree}), and natively the {@code Button} is a child of the node with the resource-id.
   */
  public By button(String id) {
    return mode.isFlutter()
        ? FlutterBy.descendant(FlutterBy.key(id), FlutterBy.type("ElevatedButton"))
        : AppiumBy.androidUIAutomator("new UiSelector().resourceId(" + quote(id)
            + ").childSelector(new UiSelector().className(\"android.widget.Button\"))");
  }

  /**
   * The element whose text the tagged text {@code id} shows. The Flutter server reads no text from the keyed
   * {@code KeyedSubtree}, so flutter mode targets its {@code Text}; natively the tagged node carries the text.
   */
  public By textOf(String id) {
    return mode.isFlutter() ? FlutterBy.descendant(FlutterBy.key(id), FlutterBy.type("Text")) : id(id);
  }

  /**
   * A widget matching {@code matching} under {@code of}. Natively this is a UiSelector {@code childSelector}, which
   * UiAutomator2 resolves among all descendants; locators that are not a plain UiSelector are not reachable.
   */
  public By descendant(By of, By matching) {
    if (mode.isFlutter()) {
      return FlutterBy.descendant(of, matching);
    }
    return AppiumBy.androidUIAutomator(uiSelector(of) + ".childSelector(" + uiSelector(matching) + ")");
  }

  /**
   * Checks the text an element from {@link #textOf(String)} shows.
   */
  public WebElementCondition shows(String text) {
    return mode.isFlutter() ? Condition.exactText(text) : Condition.attribute(CONTENT_DESC, text);
  }

  private String uiSelector(By by) {
    if (by instanceof By.Remotable remotable) {
      By.Remotable.Parameters parameters = remotable.getRemoteParameters();
      String value = String.valueOf(parameters.value());
      switch (parameters.using()) {
        case "id":
          return "new UiSelector().resourceId(" + quote(value) + ")";
        case "accessibility id":
          return "new UiSelector().description(" + quote(value) + ")";
        case "-android uiautomator":
          if (value.startsWith(UI_SELECTOR) && !value.contains(".childSelector(")) {
            return value;
          }
          break;
        default:
          break;
      }
    }
    throw new NotReachableInModeException(mode, "descendant part " + by + " has no UiSelector form");
  }

  static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
