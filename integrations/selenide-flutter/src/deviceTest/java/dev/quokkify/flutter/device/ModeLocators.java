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

  private final MobileMode mode;

  public ModeLocators(MobileMode mode) {
    this.mode = mode;
  }

  public MobileMode mode() {
    return mode;
  }

  public By id(String id) {
    return mode == MobileMode.FLUTTER ? FlutterBy.key(id) : AppiumBy.id(id);
  }

  public By label(String label) {
    return mode == MobileMode.FLUTTER ? FlutterBy.semanticsLabel(label) : AppiumBy.accessibilityId(label);
  }

  public By text(String text) {
    return mode == MobileMode.FLUTTER
        ? FlutterBy.text(text)
        : AppiumBy.androidUIAutomator("new UiSelector().description(" + quote(text) + ")");
  }

  /**
   * The element that carries the enabled state of the tagged button {@code id}. The id sits on a wrapper in both
   * modes: the Flutter server reads {@code enabled} from the widget itself (the {@code ElevatedButton} under the
   * keyed {@code KeyedSubtree}), and natively the {@code Button} is a child of the node with the resource-id.
   */
  public By button(String id) {
    return mode == MobileMode.FLUTTER
        ? FlutterBy.descendant(FlutterBy.key(id), FlutterBy.type("ElevatedButton"))
        : AppiumBy.androidUIAutomator("new UiSelector().resourceId(" + quote(id)
            + ").childSelector(new UiSelector().className(\"android.widget.Button\"))");
  }

  /**
   * The element whose text the tagged text {@code id} shows. The Flutter server reads no text from the keyed
   * {@code KeyedSubtree}, so flutter mode targets its {@code Text}; natively the tagged node carries the text.
   */
  public By textOf(String id) {
    return mode == MobileMode.FLUTTER ? FlutterBy.descendant(FlutterBy.key(id), FlutterBy.type("Text")) : id(id);
  }

  public By descendant(By of, By matching) {
    if (mode == MobileMode.NATIVE) {
      throw new NotReachableInModeException(mode, "descendant(" + of + ", " + matching + ") has no native locator");
    }
    return FlutterBy.descendant(of, matching);
  }

  /**
   * Checks the text an element from {@link #textOf(String)} shows.
   */
  public WebElementCondition shows(String text) {
    return mode == MobileMode.FLUTTER ? Condition.exactText(text) : Condition.attribute(CONTENT_DESC, text);
  }

  static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
