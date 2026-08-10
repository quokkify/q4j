package dev.quokkify.test;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;

import dev.quokkify.elements.single.Button;

import com.codeborne.selenide.Config;
import com.codeborne.selenide.Driver;
import com.codeborne.selenide.SelectorMode;
import com.codeborne.selenide.impl.SelenidePageFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NativeContainerCollectionTest {

  @Test
  public void initializesTypedContainerListWithNativeSelenidePageFactory() {
    WebElement webElement = proxy(WebElement.class, Map.of());
    WebDriver webDriver = proxy(WebDriver.class, Map.of(
        "findElements", List.of(webElement),
        "findElement", webElement
    ));
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

  @SuppressWarnings("unchecked")
  private static <T> T proxy(Class<T> type, Map<String, Object> answers) {
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
      if (answers.containsKey(method.getName())) {
        return answers.get(method.getName());
      }
      return defaultValue(method.getReturnType());
    });
  }

  private static Object defaultValue(Class<?> type) {
    if (!type.isPrimitive()) {
      return null;
    }
    if (type == boolean.class) {
      return false;
    }
    if (type == char.class) {
      return '\0';
    }
    return 0;
  }

  private static class PageWithButtons {
    @FindBy(css = "button")
    private List<Button> buttons;
  }
}
