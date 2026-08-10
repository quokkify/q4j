package dev.quokkify.model;

import dev.quokkify.annotation.PageUrl;
import dev.quokkify.impl.Page;

import com.codeborne.selenide.BasicAuthCredentials;
import com.codeborne.selenide.Selenide;
import io.qameta.allure.Allure;
import org.assertj.core.api.Assertions;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class NavigationTest {

  private static final String FAKE_LOGIN = "fakeLogin";
  private static final String FAKE_PASSWORD = "fakeSecretPassword";
  private static final String BASE_URL = "https://example.com";
  private static final String EXPECTED_URL = BASE_URL + "/dashboard";

  @Test(description = "Typed page opening with Basic Auth must not leak credentials into url or Allure step")
  public void openPageWithBasicAuthDoesNotLeakCredentials() {
    BasicAuthCredentials credentials = new BasicAuthCredentials("example.com", FAKE_LOGIN, FAKE_PASSWORD);
    Navigation navigation = new TestNavigation(BASE_URL, credentials);

    try (MockedStatic<Selenide> selenideMock = Mockito.mockStatic(Selenide.class);
         MockedStatic<Allure> allureMock = Mockito.mockStatic(Allure.class)) {
      selenideMock.when(() -> Selenide.open(
              Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.eq(TestPage.class)))
          .thenReturn(new TestPage());

      TestPage page = navigation.openPage(TestPage.class);

      Assertions.assertThat(page).as("Typed page opening must still return the requested Page Object").isNotNull();

      ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
      selenideMock.verify(() -> Selenide.open(
          urlCaptor.capture(), Mockito.eq("example.com"), Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD), Mockito.eq(TestPage.class)));
      Assertions.assertThat(urlCaptor.getValue())
          .as("Url passed to Selenide.open must be the plain page url without embedded credentials")
          .isEqualTo(EXPECTED_URL)
          .doesNotContain(FAKE_LOGIN, FAKE_PASSWORD);

      ArgumentCaptor<String> stepCaptor = ArgumentCaptor.forClass(String.class);
      allureMock.verify(() -> Allure.step(stepCaptor.capture()));
      Assertions.assertThat(stepCaptor.getValue())
          .as("Allure step text must not contain Basic Auth credentials")
          .doesNotContain(FAKE_LOGIN, FAKE_PASSWORD)
          .contains(EXPECTED_URL);
    }
  }

  @Test(description = "Opening by raw url with Basic Auth must use the native Selenide overload without embedding credentials")
  public void openPageByUrlWithBasicAuthDoesNotLeakCredentials() {
    BasicAuthCredentials credentials = new BasicAuthCredentials(FAKE_LOGIN, FAKE_PASSWORD);
    Navigation navigation = new TestNavigation(BASE_URL, credentials);

    try (MockedStatic<Selenide> selenideMock = Mockito.mockStatic(Selenide.class)) {
      navigation.openPage(EXPECTED_URL);

      ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
      selenideMock.verify(() -> Selenide.open(
          urlCaptor.capture(), Mockito.eq("example.com"), Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD)));
      Assertions.assertThat(urlCaptor.getValue())
          .as("Url passed to Selenide.open must be the plain page url without embedded credentials")
          .isEqualTo(EXPECTED_URL)
          .doesNotContain(FAKE_LOGIN, FAKE_PASSWORD);
    }
  }

  @Test(description = "Basic Auth with an explicit domain must not be overridden by the target url's host")
  public void openPageByUrlWithExplicitBasicAuthDomainKeepsExplicitDomain() {
    BasicAuthCredentials credentials = new BasicAuthCredentials("auth.example.com", FAKE_LOGIN, FAKE_PASSWORD);
    Navigation navigation = new TestNavigation(BASE_URL, credentials);

    try (MockedStatic<Selenide> selenideMock = Mockito.mockStatic(Selenide.class)) {
      navigation.openPage(EXPECTED_URL);

      selenideMock.verify(() -> Selenide.open(
          Mockito.eq(EXPECTED_URL), Mockito.eq("auth.example.com"), Mockito.eq(FAKE_LOGIN), Mockito.eq(FAKE_PASSWORD)));
    }
  }

  @PageUrl("/dashboard")
  private static final class TestPage implements Page {
  }

  private static final class TestNavigation extends Navigation {
    private TestNavigation(String baseUrl, BasicAuthCredentials credentials) {
      super(baseUrl, credentials);
    }
  }
}
