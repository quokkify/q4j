package dev.quokkify.test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.listener.lifecycle.SuiteListener;

import org.assertj.core.api.Assertions;
import org.testng.IAlterSuiteListener;
import org.testng.IAnnotationTransformer;
import org.testng.IConfigurationListener;
import org.testng.ITestListener;
import org.testng.ITestResult;
import org.testng.TestNG;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.ITestAnnotation;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class SuiteListenerGroupsTest {

  private static final String API_GROUP = "api";
  private static final String CONFIG_PREFIX = "config:";
  private static final List<Class<?>> FIXTURES = List.of(FixtureTest.class, InheritedApiTest.class);

  @Test(description = "Included groups of the original test are applied to the generated tests")
  public void testIncludedGroupsSurvive() {
    XmlSuite suite = fixtureSuite();
    suite.getTests().get(0).setIncludedGroups(List.of(API_GROUP));

    List<XmlTest> generated = alter(suite);

    Assertions.assertThat(generated).extracting(XmlTest::getName).containsExactly("Concurrency", "Sequential");
    Assertions.assertThat(includedMethods(generated)).containsExactlyInAnyOrder("api", "apiSingleThread", "inheritedApi");
    Assertions.assertThat(generated).allSatisfy(test -> {
      Assertions.assertThat(test.getIncludedGroups()).as("Included groups of %s", test.getName()).containsExactly(API_GROUP);
      Assertions.assertThat(test.getExcludedGroups()).as("Excluded groups of %s", test.getName()).isEmpty();
    });
  }

  @Test(description = "Excluded groups of the original suite and test are applied to the generated tests")
  public void testExcludedGroupsSurvive() {
    XmlSuite suite = fixtureSuite();
    suite.setExcludedGroups(List.of(API_GROUP));
    suite.getTests().get(0).setExcludedGroups(List.of("slow", API_GROUP));

    List<XmlTest> generated = alter(suite);

    Assertions.assertThat(includedMethods(generated)).containsExactlyInAnyOrder("plain", "plainSingleThread");
    Assertions.assertThat(generated).allSatisfy(test -> {
      Assertions.assertThat(test.getExcludedGroups()).as("Excluded groups of %s", test.getName())
          .containsExactlyInAnyOrder(API_GROUP, "slow");
      Assertions.assertThat(test.getIncludedGroups()).as("Included groups of %s", test.getName()).isEmpty();
    });
  }

  @Test(description = "Generated tests have no group filters when the original suite has none")
  public void testNoGroupsKeepGeneratedSuiteUnfiltered() {
    XmlSuite suite = fixtureSuite();
    List<XmlSuite> suites = new ArrayList<>(List.of(suite));

    new SuiteListener().alter(suites);

    Assertions.assertThat(includedMethods(suites.get(0).getTests()))
        .containsExactlyInAnyOrder("api", "apiSingleThread", "inheritedApi", "plain", "plainSingleThread");
    Assertions.assertThat(suites.get(0).getGroups()).as("Generated suite groups").isNull();
    Assertions.assertThat(suites.get(0).getTests()).allSatisfy(test -> {
      Assertions.assertThat(test.getXmlGroups()).as("Groups of %s", test.getName()).isNull();
      Assertions.assertThat(test.getIncludedGroups()).as("Included groups of %s", test.getName()).isEmpty();
      Assertions.assertThat(test.getExcludedGroups()).as("Excluded groups of %s", test.getName()).isEmpty();
    });
  }

  @SingleThread
  @Test(description = "Methods of a group excluded via TestNG#setExcludedGroups do not run")
  public void testExcludedGroupMethodsDoNotRun() {
    List<String> executed = run(testng -> testng.setExcludedGroups(API_GROUP));

    Assertions.assertThat(testMethods(executed)).containsExactlyInAnyOrder("plain", "plainSingleThread");
    Assertions.assertThat(executed).contains(CONFIG_PREFIX + "alwaysRunSetUp", CONFIG_PREFIX + "plainSetUp");
  }

  @SingleThread
  @Test(description = "Only methods of a group included via TestNG#setGroups run, alwaysRun configuration still runs")
  public void testOnlyIncludedGroupMethodsRun() {
    List<String> executed = run(testng -> testng.setGroups(API_GROUP));

    Assertions.assertThat(testMethods(executed)).containsExactlyInAnyOrder("api", "apiSingleThread", "inheritedApi");
    Assertions.assertThat(executed).contains(CONFIG_PREFIX + "alwaysRunSetUp").doesNotContain(CONFIG_PREFIX + "plainSetUp");
  }

  @SingleThread
  @Test(description = "All methods run when no groups are selected")
  public void testAllMethodsRunWithoutGroups() {
    List<String> executed = run(testng -> { });

    Assertions.assertThat(testMethods(executed))
        .containsExactlyInAnyOrder("api", "apiSingleThread", "inheritedApi", "plain", "plainSingleThread");
    Assertions.assertThat(executed).contains(CONFIG_PREFIX + "alwaysRunSetUp", CONFIG_PREFIX + "plainSetUp");
  }

  private static XmlSuite fixtureSuite() {
    XmlSuite suite = new XmlSuite();
    suite.setName("Groups fixture");
    XmlTest test = new XmlTest(suite);
    test.setName("Fixture");
    test.setXmlClasses(List.of(new XmlClass(FixtureTest.class), new XmlClass(InheritedApiTest.class)));
    return suite;
  }

  private static List<XmlTest> alter(XmlSuite suite) {
    List<XmlSuite> suites = new ArrayList<>(List.of(suite));
    new SuiteListener().alter(suites);
    Assertions.assertThat(suites).as("Generated suites").hasSize(1);
    return suites.get(0).getTests();
  }

  private static List<String> includedMethods(List<XmlTest> tests) {
    return tests.stream()
        .flatMap(test -> test.getClasses().stream())
        .flatMap(xmlClass -> xmlClass.getIncludedMethods().stream())
        .map(XmlInclude::getName)
        .toList();
  }

  private static List<String> testMethods(List<String> executed) {
    return executed.stream().filter(name -> !name.startsWith(CONFIG_PREFIX)).toList();
  }

  private static List<String> run(Consumer<TestNG> groupSelection) {
    List<String> executed = new CopyOnWriteArrayList<>();
    TestNG testng = new TestNG(false);
    testng.setUseDefaultListeners(false);
    testng.setServiceLoaderClassLoader(new URLClassLoader(new URL[0], null));
    testng.addListener(new IAlterSuiteListener() {
      @Override
      public void alter(List<XmlSuite> suites) {
        new SuiteListener().alter(suites);
      }
    });
    testng.addListener(new ITestListener() {
      @Override
      public void onTestStart(ITestResult result) {
        executed.add(result.getMethod().getMethodName());
      }
    });
    testng.addListener(new IConfigurationListener() {
      @Override
      public void onConfigurationSuccess(ITestResult result) {
        executed.add(CONFIG_PREFIX + result.getMethod().getMethodName());
      }
    });
    testng.addListener(new IAnnotationTransformer() {
      @Override
      public void transform(ITestAnnotation annotation, Class testClass, Constructor testConstructor, Method testMethod) {
        if (Objects.nonNull(testMethod) && FIXTURES.contains(testMethod.getDeclaringClass())) {
          annotation.setEnabled(true);
        }
      }
    });
    groupSelection.accept(testng);
    testng.setXmlSuites(new ArrayList<>(List.of(fixtureSuite())));
    testng.run();
    Assertions.assertThat(testng.getStatus()).as("Fixture suite status, executed: %s", executed).isZero();
    return executed;
  }

  // Disabled so the outer run skips it; run enables it for the nested TestNG run only.
  public static class FixtureTest {

    @BeforeClass(alwaysRun = true)
    public void alwaysRunSetUp() {
    }

    @BeforeClass
    public void plainSetUp() {
    }

    @Test(enabled = false, groups = API_GROUP)
    public void api() {
    }

    @SingleThread
    @Test(enabled = false, groups = API_GROUP)
    public void apiSingleThread() {
    }

    @Test(enabled = false)
    public void plain() {
    }

    @SingleThread
    @Test(enabled = false)
    public void plainSingleThread() {
    }
  }

  @Test(groups = API_GROUP)
  public abstract static class GroupedApiBase {
  }

  public static class InheritedApiTest extends GroupedApiBase {

    @Test(enabled = false)
    public void inheritedApi() {
    }
  }
}
