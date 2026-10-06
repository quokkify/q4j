package dev.quokkify.listener.lifecycle;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestNGExtension;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import org.apache.commons.lang3.StringUtils;
import org.testng.IAlterSuiteListener;
import org.testng.IInvokedMethod;
import org.testng.IInvokedMethodListener;
import org.testng.ITestResult;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

/**
 * Default listener for generating tests suites.
 *
 * <p>
 * List of environment variables that should be provided in child projects:
 * SUITE_NAME, TEST_THREAD_COUNT, TEST_PARALLEL_MODE, DATA_PROVIDER_THREAD_COUNT,
 * SINGLE_THREAD_TESTS_IN_PARALLEL.
 * Used default values if not overridden.
 * </p>
 *
 * <p>
 * TestNG group filters of the original suites and tests are merged and applied to the generated tests.
 * Known limits: methods in other classes that are targets of dependsOnGroups or dependsOnMethods are not
 * added when the group filter drops them, XML {@code <define>} meta-groups and groups changed by an
 * {@link org.testng.IAnnotationTransformer} are not used to select test methods.
 * SingleGroupListener must run before this listener, because this listener keeps only included methods
 * of the original suites.
 * </p>
 */
public class SuiteListener implements IAlterSuiteListener, IInvokedMethodListener {

  private static final int SINGLE_THREAD_COUNT = 1;
  private static final int GENERATED_TESTS_COUNT = 2;
  private static final TestNGExtension CONFIG = ConfigRegistry.get(TestNGExtension.class);

  @Override
  public void alter(List<XmlSuite> suites) {
    Map<XmlClass, List<Method>> classesWithTests = getClassesWithTestsFromSuites(suites);
    List<String> includedGroups = collectGroups(suites, XmlSuite::getIncludedGroups, XmlTest::getIncludedGroups);
    List<String> excludedGroups = collectGroups(suites, XmlSuite::getExcludedGroups, XmlTest::getExcludedGroups);
    XmlSuite suite = generateSuite(filterTestsByGroups(classesWithTests, includedGroups, excludedGroups));
    if (!includedGroups.isEmpty()) {
      suite.setIncludedGroups(includedGroups);
    }
    if (!excludedGroups.isEmpty()) {
      suite.setExcludedGroups(excludedGroups);
    }
    suites.clear();
    suites.add(suite);
    IAlterSuiteListener.super.alter(suites);
  }

  @Override
  public void afterInvocation(IInvokedMethod method, ITestResult testResult) {
    addProvenanceLabels();
  }

  private void addProvenanceLabels() {
    String environment = ubuntuEnvironment();
    String module = System.getenv("MODULE_PATH");
    if (StringUtils.isNotBlank(environment)) {
      Allure.getLifecycle().updateTest(result -> result.getLabels().add(new Label().setName("environment").setValue(environment)));
    }
    if (StringUtils.isNotBlank(module)) {
      Allure.getLifecycle().updateTest(result -> {
        result.getLabels().removeIf(label -> "subSuite".equals(label.getName()));
        result.getLabels().add(new Label().setName("subSuite").setValue(module.replaceFirst("^:", "")));
      });
    }
  }

  private String ubuntuEnvironment() {
    String configured = System.getenv("ALLURE_UBUNTU_ENVIRONMENT");
    if (StringUtils.isNotBlank(configured)) return configured;
    Path osRelease = Path.of(System.getenv().getOrDefault("OS_RELEASE_FILE", "/etc/os-release"));
    try {
      Map<String, String> values = Files.readAllLines(osRelease).stream()
          .map(line -> line.split("=", 2))
          .filter(parts -> parts.length == 2)
          .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1].replaceAll("^\\\"|\\\"$", ""), (first, ignored) -> first));
      String id = values.get("ID");
      String version = values.get("VERSION_ID");
      return StringUtils.isNoneBlank(id, version) ? id + "-" + version : null;
    } catch (IOException ignored) {
      return null;
    }
  }

  /**
   * Collect TestNG group filters of the original suites and their tests.
   * Groups of all original suites and tests are merged, so the generated tests
   * apply the union of the original group selection.
   *
   * @param suites      original suites {@link List}&lt;{@link XmlSuite}&gt;
   * @param suiteGroups suite groups getter
   * @param testGroups  test groups getter
   * @return distinct group names or patterns as {@link List}&lt;{@link String}&gt;
   */
  protected List<String> collectGroups(List<XmlSuite> suites,
                                       Function<XmlSuite, List<String>> suiteGroups,
                                       Function<XmlTest, List<String>> testGroups) {
    return suites.stream()
        .flatMap(xmlSuite -> Stream.concat(
            suiteGroups.apply(xmlSuite).stream(),
            xmlSuite.getTests().stream().flatMap(xmlTest -> testGroups.apply(xmlTest).stream())))
        .distinct()
        .toList();
  }

  /**
   * Keep only test methods selected by provided groups.
   * TestNG runs explicitly included methods regardless of group filters,
   * so the generated tests must list only methods that pass the group selection.
   *
   * @param tests          provided xml classes with tests
   * @param includedGroups included group patterns, all groups are included if empty
   * @param excludedGroups excluded group patterns
   * @return xml classes with selected tests
   */
  protected Map<XmlClass, List<Method>> filterTestsByGroups(Map<XmlClass, List<Method>> tests,
                                                            List<String> includedGroups,
                                                            List<String> excludedGroups) {
    if (includedGroups.isEmpty() && excludedGroups.isEmpty()) {
      return tests;
    }
    List<Pattern> included = compileGroupPatterns(includedGroups);
    List<Pattern> excluded = compileGroupPatterns(excludedGroups);
    return tests.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
        .filter(method -> isMethodSelectedByGroups(method, included, excluded))
        .toList()));
  }

  /**
   * Check is method selected by provided groups.
   * Method groups are the groups of its {@link Test} annotation and of the first class level {@link Test}
   * annotation found in the class hierarchy, as TestNG resolves them.
   *
   * @param method         provided test method
   * @param includedGroups included group patterns, all groups are included if empty
   * @param excludedGroups excluded group patterns
   * @return method selection status as {@link Boolean}
   */
  protected boolean isMethodSelectedByGroups(Method method, List<Pattern> includedGroups, List<Pattern> excludedGroups) {
    List<String> groups = Stream.concat(
        Arrays.stream(method.getAnnotation(Test.class).groups()),
        Arrays.stream(getClassLevelGroups(method.getDeclaringClass()))).toList();
    return (includedGroups.isEmpty() || matchesAnyGroup(groups, includedGroups)) && !matchesAnyGroup(groups, excludedGroups);
  }

  private String[] getClassLevelGroups(Class<?> testClass) {
    for (Class<?> current = testClass; current != null && current != Object.class; current = current.getSuperclass()) {
      Test classTest = current.getAnnotation(Test.class);
      if (Objects.nonNull(classTest)) {
        return classTest.groups();
      }
    }
    return new String[0];
  }

  private List<Pattern> compileGroupPatterns(List<String> groups) {
    return groups.stream()
        .map(group -> Pattern.compile(group.contains("\\$") ? group : group.replace("$", "\\$")))
        .toList();
  }

  private boolean matchesAnyGroup(List<String> groups, List<Pattern> patterns) {
    return groups.stream().anyMatch(group -> patterns.stream().anyMatch(pattern -> pattern.matcher(group).matches()));
  }

  /**
   * Get classes with tests form provided suites list.
   *
   * @param suites list of suites {@link List}&lt;{@link XmlSuite}&gt;
   * @return classes with included tests
   */
  protected Map<XmlClass, List<Method>> getClassesWithTestsFromSuites(List<XmlSuite> suites) {
    List<XmlTest> xmlTests = suites.stream().flatMap(xmlSuite -> xmlSuite.getTests().stream()).toList();
    List<XmlClass> xmlClasses = xmlTests.stream().flatMap(xmlTest -> xmlTest.getClasses().stream()).toList();
    return xmlClasses.stream().collect(Collectors.toMap(Function.identity(), this::getTestMethodsFromXmlClass));
  }

  /**
   * Get test methods from {@link XmlClass}.
   * If xml class contains included methods - return only included methods.
   * Else if xml class not contains included methods - return all tests methods.
   *
   * @param xmlClass provided Xml class {@link XmlClass}
   * @return xml class methods as {@link List}&lt;{@link Method}&gt;
   */
  protected List<Method> getTestMethodsFromXmlClass(XmlClass xmlClass) {
    return !xmlClass.getIncludedMethods().isEmpty()
        ? getIncludedTestMethodsFromXmlClass(xmlClass)
        : getAllTestMethodsFromXmlClass(xmlClass);
  }

  /**
   * Get included test methods from {@link XmlClass}.
   *
   * @param xmlClass provided xml class {@link XmlClass}
   * @return included xml class methods as {@link List}&lt;{@link Method}&gt;
   */
  protected List<Method> getIncludedTestMethodsFromXmlClass(XmlClass xmlClass) {
    List<String> includedTestName = xmlClass.getIncludedMethods().stream().map(XmlInclude::getName).toList();
    return getAllTestMethodsFromXmlClass(xmlClass).stream()
        .filter(testMethod -> includedTestName.contains(testMethod.getName()))
        .toList();
  }

  /**
   * Get all test methods from {@link XmlClass}.
   *
   * @param xmlClass provided xml class {@link XmlClass}
   * @return all xml class methods as {@link List}&lt;{@link Method}&gt;
   */
  protected List<Method> getAllTestMethodsFromXmlClass(XmlClass xmlClass) {
    return Arrays.stream(xmlClass.getSupportClass().getDeclaredMethods())
        .filter(method -> Objects.nonNull(method.getAnnotation(Test.class)))
        .toList();
  }

  /**
   * Generate xml suite from provided classes with tests.
   * Contains tests for multiply threads and single threads.
   * The single thread test starts after the multiply threads test has finished, unless
   * SINGLE_THREAD_TESTS_IN_PARALLEL is enabled: then both tests run at the same time,
   * while single thread tests are still executed one by one.
   *
   * @param tests provided xml classes with tests
   * @return generated xml suite as {@link XmlSuite}
   */
  protected XmlSuite generateSuite(Map<XmlClass, List<Method>> tests) {
    XmlSuite newSuite = new XmlSuite();
    newSuite.setName(getSuiteName());
    newSuite.setDataProviderThreadCount(getDataProviderThreadCount());
    if (Boolean.TRUE.equals(CONFIG.singleThreadTestsInParallel())) {
      newSuite.setParallel(XmlSuite.ParallelMode.TESTS);
      newSuite.setThreadCount(GENERATED_TESTS_COUNT);
    }
    XmlTest multiThreadTest = generateGroupXmlTest(newSuite, tests, ThreadGroup.MULTIPLY_THREADS);
    multiThreadTest.setName("Concurrency");
    multiThreadTest.setParallel(getTestParallelMode());
    multiThreadTest.setThreadCount(getTestThreadCount());
    XmlTest singleThreadTest = generateGroupXmlTest(newSuite, tests, ThreadGroup.SINGLE_THREAD);
    singleThreadTest.setName("Sequential");
    singleThreadTest.setParallel(XmlSuite.ParallelMode.NONE);
    singleThreadTest.setThreadCount(SINGLE_THREAD_COUNT);
    return newSuite;
  }

  /**
   * Generate xml test from provided classes with tests according to thread group.
   *
   * @param newSuite    provided suite with tests
   * @param tests       provided xml classes with tests
   * @param threadGroup provided thread group
   * @return generated xml test as {@link XmlTest}
   */
  protected XmlTest generateGroupXmlTest(XmlSuite newSuite,
                                         Map<XmlClass, List<Method>> tests,
                                         ThreadGroup threadGroup) {
    XmlTest test = new XmlTest(newSuite);
    List<XmlClass> xmlClasses = tests.entrySet().stream()
        .collect(Collectors.toMap(Map.Entry::getKey, xmlClass -> getXmlIncludeTests(xmlClass.getValue(), threadGroup)))
        .entrySet().stream().filter(testMethods -> !testMethods.getValue().isEmpty())
        .map(testMethods -> generateXmlClass(testMethods.getKey().getName(), testMethods.getValue()))
        .collect(Collectors.toList());
    test.setClasses(xmlClasses);
    return test;
  }

  /**
   * Get xml include tests according to provided thread group.
   *
   * @param methods     provided xml classes with tests
   * @param threadGroup provided thread group
   * @return filtered xml include tests as {@link List}&lt;{@link XmlInclude}&gt;
   */
  protected List<XmlInclude> getXmlIncludeTests(List<Method> methods, ThreadGroup threadGroup) {
    Predicate<Method> methodThreadsPredicate = threadGroup.equals(ThreadGroup.MULTIPLY_THREADS)
        ? method -> !isMethodSingleThread(method)
        : this::isMethodSingleThread;
    return methods.stream().filter(methodThreadsPredicate)
        .map(method -> new XmlInclude(method.getName()))
        .collect(Collectors.toList());
  }

  /**
   * Check is method has {@link SingleThread} annotation.
   * If method has the annotation - return true.
   * Else if method has no the annotation - return false.
   *
   * @param method provided test method
   * @return method single thread status as {@link Boolean}
   */
  protected boolean isMethodSingleThread(Method method) {
    return Objects.nonNull(method.getAnnotation(SingleThread.class));
  }

  /**
   * Generate new xml class according to provided class name and test methods.
   *
   * @param className provided class name
   * @param methods   provided class test methods
   * @return generated xml class as {@link XmlClass}
   */
  protected XmlClass generateXmlClass(String className, List<XmlInclude> methods) {
    XmlClass xmlClass = new XmlClass(className);
    xmlClass.setIncludedMethods(methods);
    return xmlClass;
  }

  /**
   * Get suite name according to provided environment variable.
   * If not provided used default value.
   *
   * @return suite name as {@link String}
   */
  private String getSuiteName() {
    return CONFIG.suiteName();
  }

  /**
   * Get test thread count according to provided environment variable.
   * If not provided used default value.
   *
   * @return test thread count as {@link Integer}
   */
  private Integer getTestThreadCount() {
    return CONFIG.testThreadCount();
  }

  /**
   * Get test parallel mode according to provided environment variable.
   * If not provided used default value.
   *
   * @return test parallel mode as {@link XmlSuite.ParallelMode}
   */
  private XmlSuite.ParallelMode getTestParallelMode() {
    String testParallelMode = System.getenv("TEST_PARALLEL_MODE");
    return StringUtils.isNotBlank(testParallelMode)
        ? XmlSuite.ParallelMode.valueOf(testParallelMode)
        : XmlSuite.ParallelMode.METHODS;
  }

  /**
   * Get data provider thread count according to provided environment variable.
   * If not provided used default value.
   *
   * @return data provider thread count as {@link Integer}
   */
  private Integer getDataProviderThreadCount() {
    String dataProviderThreadCount = System.getenv("DATA_PROVIDER_THREAD_COUNT");
    return StringUtils.isNotBlank(dataProviderThreadCount)
        ? Integer.valueOf(Integer.parseInt(dataProviderThreadCount))
        : XmlSuite.DEFAULT_DATA_PROVIDER_THREAD_COUNT;
  }

  protected enum ThreadGroup {
    SINGLE_THREAD, MULTIPLY_THREADS
  }
}
