package dev.quokkify.test;

import java.lang.reflect.Method;

import dev.quokkify.util.TestUtils;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class TmsLinksTest {

  @TmsLink("TNG_TMS_LINKS_1")
  @Test(description = "Two method links are read in declaration order")
  public void twoMethodLinks() throws NoSuchMethodException {
    Assertions.assertThat(TestUtils.getTmsLinks(method(NoClassLinks.class, "twoLinks")))
        .containsExactly("6", "7");
  }

  @TmsLink("TNG_TMS_LINKS_2")
  @Test(description = "Class link is used when the method has none")
  public void classLinkWhenMethodHasNone() throws NoSuchMethodException {
    Assertions.assertThat(TestUtils.getTmsLinks(method(ClassLinked.class, "noLinks")))
        .containsExactly("9");
  }

  @TmsLink("TNG_TMS_LINKS_3")
  @Test(description = "Method link wins over class link")
  public void methodWinsOverClass() throws NoSuchMethodException {
    Assertions.assertThat(TestUtils.getTmsLinks(method(ClassLinked.class, "ownLink")))
        .containsExactly("6");
  }

  @TmsLink("TNG_TMS_LINKS_4")
  @Test(description = "Blank and duplicate links are ignored")
  public void blankAndDuplicateIgnored() throws NoSuchMethodException {
    Assertions.assertThat(TestUtils.getTmsLinks(method(NoClassLinks.class, "blankAndDuplicate")))
        .containsExactly("6");
  }

  private static Method method(Class<?> type, String name) throws NoSuchMethodException {
    return type.getDeclaredMethod(name);
  }

  static class NoClassLinks {

    @TmsLink("6")
    @TmsLink("7")
    public void twoLinks() {
    }

    @TmsLink(" ")
    @TmsLink("6")
    @TmsLink("6")
    public void blankAndDuplicate() {
    }
  }

  @TmsLink("9")
  static class ClassLinked {

    public void noLinks() {
    }

    @TmsLink("6")
    public void ownLink() {
    }
  }
}
