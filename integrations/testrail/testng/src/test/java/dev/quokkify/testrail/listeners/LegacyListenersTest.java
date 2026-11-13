package dev.quokkify.testrail.listeners;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.tms.TestManagementSystem;
import dev.quokkify.tms.listeners.TmsListener;
import dev.quokkify.tms.tickets.TicketSource;

import io.qameta.allure.TmsLink;
import org.testng.IMethodInstance;
import org.testng.ITestContext;
import org.testng.ITestNGMethod;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Must stay the only test of this module that loads {@link TestStatusTestRailListener}: its enabled flag is read
 * from system properties once, in the static initializer.
 */
@SuppressWarnings("removal")
public class LegacyListenersTest {

  private static final String[] PROPERTIES = {"TESTRAIL_ID", "TESTRAIL_URL"};

  @BeforeClass
  public void enableTestRail() {
    System.setProperty("TESTRAIL_ID", "1");
    System.setProperty("TESTRAIL_URL", "http://127.0.0.1:9");
  }

  @AfterClass(alwaysRun = true)
  public void cleanUp() {
    for (String property : PROPERTIES) {
      System.clearProperty(property);
    }
    assertThat(TmsListener.isActive()).isFalse();
  }

  @TmsLink("LEGACY_1")
  @SingleThread
  @Test(description = "Legacy status listener does nothing while TmsListener is active")
  public void statusListenerIsNoOpWhileTmsListenerActive() throws ReflectiveOperationException {
    TmsListener tmsListener = tmsListenerWithoutProvider();
    tmsListener.onExecutionStart();
    try {
      assertThat(TmsListener.isActive()).isTrue();
      new TestStatusTestRailListener().onTestSuccess(linkedResult());
    } finally {
      tmsListener.onExecutionFinish();
    }
    assertThat(TmsListener.isActive()).isFalse();
  }

  @TmsLink("LEGACY_2")
  @SingleThread
  @Test(description = "Legacy interceptor returns the methods unchanged while TmsListener is active")
  public void interceptorIsNoOpWhileTmsListenerActive() throws ReflectiveOperationException {
    TmsListener tmsListener = tmsListenerWithoutProvider();
    tmsListener.onExecutionStart();
    try {
      List<IMethodInstance> methods = List.of(mock(IMethodInstance.class));
      assertThat(new TestRailInterceptor().intercept(methods, mock(ITestContext.class))).isSameAs(methods);
    } finally {
      tmsListener.onExecutionFinish();
    }
  }

  @TmsLink("LEGACY_3")
  @SingleThread
  @Test(description = "Legacy disabled-tests listener reports nothing while TmsListener is active")
  public void addDisabledListenerIsNoOpWhileTmsListenerActive() throws ReflectiveOperationException {
    TmsListener tmsListener = tmsListenerWithoutProvider();
    tmsListener.onExecutionStart();
    try {
      // a null suite throws if the listener reads it, so returning normally proves the early return
      new AddDisabledTestsToTestRailListener().onFinish(null);
    } finally {
      tmsListener.onExecutionFinish();
    }
  }

  private static TmsListener tmsListenerWithoutProvider() throws ReflectiveOperationException {
    Constructor<TmsListener> constructor = TmsListener.class.getDeclaredConstructor(Supplier.class, Supplier.class);
    constructor.setAccessible(true);
    Supplier<Optional<TestManagementSystem>> resolver = Optional::empty;
    Supplier<List<TicketSource>> tickets = List::of;
    return constructor.newInstance(resolver, tickets);
  }

  private static ITestResult linkedResult() throws NoSuchMethodException {
    ITestNGMethod method = mock(ITestNGMethod.class);
    when(method.getConstructorOrMethod()).thenReturn(
        new org.testng.internal.ConstructorOrMethod(Linked.class.getDeclaredMethod("linked")));
    when(method.getMethodName()).thenReturn("linked");
    ITestResult result = mock(ITestResult.class);
    when(result.getMethod()).thenReturn(method);
    return result;
  }

  private static final class Linked {

    @TmsLink("1")
    void linked() {
    }
  }
}
