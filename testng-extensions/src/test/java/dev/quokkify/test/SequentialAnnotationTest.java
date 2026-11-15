package dev.quokkify.test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import dev.quokkify.annotation.Sequential;
import dev.quokkify.annotation.SingleThread;
import dev.quokkify.listener.lifecycle.SuiteListener;

import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

@SuppressWarnings("removal")
public class SequentialAnnotationTest {

  private static final String CONCURRENCY = "Concurrency";
  private static final String SEQUENTIAL = "Sequential";

  @Test(description = "Regression guard: @Sequential and deprecated @SingleThread both go to the Sequential test")
  public void testBothAnnotationsLandInSequential() {
    List<XmlTest> generated = alter(new SuiteListener());

    Assertions.assertThat(generated).extracting(XmlTest::getName).containsExactly(CONCURRENCY, SEQUENTIAL);
    Assertions.assertThat(includedMethods(generated, CONCURRENCY)).containsExactly("concurrent");
    Assertions.assertThat(includedMethods(generated, SEQUENTIAL))
        .containsExactlyInAnyOrder("sequential", "legacySingleThread", "both");
  }

  @Test(description = "Regression guard: an overridden isMethodSingleThread still moves methods to Sequential")
  public void testLegacyOverrideIsHonoured() {
    List<XmlTest> generated = alter(new LegacyOverrideListener());

    Assertions.assertThat(includedMethods(generated, CONCURRENCY)).isEmpty();
    Assertions.assertThat(includedMethods(generated, SEQUENTIAL))
        .containsExactlyInAnyOrder("concurrent", "sequential", "legacySingleThread", "both");
  }

  private static List<XmlTest> alter(SuiteListener listener) {
    XmlSuite suite = new XmlSuite();
    suite.setName("Sequential fixture");
    XmlTest test = new XmlTest(suite);
    test.setName("Fixture");
    test.setXmlClasses(List.of(new XmlClass(FixtureTest.class)));
    List<XmlSuite> suites = new ArrayList<>(List.of(suite));
    listener.alter(suites);
    Assertions.assertThat(suites).as("Generated suites").hasSize(1);
    return suites.get(0).getTests();
  }

  private static List<String> includedMethods(List<XmlTest> tests, String testName) {
    return tests.stream()
        .filter(test -> testName.equals(test.getName()))
        .flatMap(test -> test.getClasses().stream())
        .flatMap(xmlClass -> xmlClass.getIncludedMethods().stream())
        .map(XmlInclude::getName)
        .toList();
  }

  // Disabled so the outer run skips the fixture methods; only the generated suite structure is inspected.
  public static class FixtureTest {

    @Test(enabled = false)
    public void concurrent() {
    }

    @Sequential
    @Test(enabled = false)
    public void sequential() {
    }

    @SingleThread
    @Test(enabled = false)
    public void legacySingleThread() {
    }

    @Sequential
    @SingleThread
    @Test(enabled = false)
    public void both() {
    }
  }

  public static class LegacyOverrideListener extends SuiteListener {

    @Override
    protected boolean isMethodSingleThread(Method method) {
      return super.isMethodSingleThread(method) || "concurrent".equals(method.getName());
    }
  }
}
