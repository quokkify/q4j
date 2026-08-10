package dev.quokkify.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import dev.quokkify.elements.single.Button;

import com.codeborne.selenide.Config;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelectorMode;
import com.codeborne.selenide.impl.SelenidePageFactory;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NativeContainerCollectionTest {

  @Test
  public void initializesTypedContainerListWithNativeSelenidePageFactory() {
    WebElement webElement = proxy(WebElement.class, Map.of());
    WebDriver webDriver = proxy(WebDriver.class, (instance, method, arguments) -> {
      if (method.getName().equals("findElements") || method.getName().equals("findElement")) {
        assertThat(arguments).containsExactly(By.cssSelector("button"));
        return method.getName().equals("findElements") ? List.of(webElement) : webElement;
      }
      return defaultValue(method.getReturnType());
    });
    Config config = proxy(Config.class, Map.of("selectorMode", SelectorMode.CSS));
    Driver driver = proxy(Driver.class, Map.of(
        "config", config,
        "getWebDriver", webDriver
    ));

    PageWithButtons page = new SelenidePageFactory().page(driver, PageWithButtons.class);

    assertThat(page.buttons).hasSize(1);
    assertThat(page.buttons.getFirst()).isInstanceOf(Button.class);
    assertThat(page.buttons.getFirst().getSelf().toWebElement()).isSameAs(webElement);
  }

  @Test
  public void returnsTypeCompatibleDefaultsForPrimitiveProxyMethods() {
    PrimitiveMethods values = proxy(PrimitiveMethods.class, Map.of());

    assertThat(values.booleanValue()).isFalse();
    assertThat(values.charValue()).isEqualTo('\0');
    assertThat(values.byteValue()).isZero();
    assertThat(values.shortValue()).isZero();
    assertThat(values.intValue()).isZero();
    assertThat(values.longValue()).isZero();
    assertThat(values.floatValue()).isZero();
    assertThat(values.doubleValue()).isZero();
    values.voidValue();
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, Map<String, Object> answers) {
    return proxy(type, (instance, method, arguments) ->
        answers.getOrDefault(method.getName(), defaultValue(method.getReturnType())));
  }

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, InvocationHandler handler) {
    return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (instance, method, arguments) -> {
      if (method.getName().equals("toString")) {
        return type.getSimpleName();
      }
      if (method.getName().equals("hashCode")) {
        return System.identityHashCode(instance);
      }
      if (method.getName().equals("equals")) {
        return instance == arguments[0];
      }
      return handler.invoke(instance, method, arguments);
    });
  }

  private static Object defaultValue(Class<?> type) {
    return switch (type.getName()) {
      case "boolean" -> false;
      case "char" -> '\0';
      case "byte" -> (byte) 0;
      case "short" -> (short) 0;
      case "int" -> 0;
      case "long" -> 0L;
      case "float" -> 0.0F;
      case "double" -> 0.0D;
      default -> null;
    };
  }

  private static class PageWithButtons {
    @FindBy(css = "button")
    private List<Button> buttons;
  }

  private interface PrimitiveMethods {
    boolean booleanValue();

    char charValue();

    byte byteValue();

    short shortValue();

    int intValue();

    long longValue();

    float floatValue();

    double doubleValue();

    void voidValue();
  }
}
