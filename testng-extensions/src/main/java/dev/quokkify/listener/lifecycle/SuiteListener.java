package dev.quokkify.listener.lifecycle;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestNGExtension;

import org.apache.commons.lang3.StringUtils;
import org.testng.IAlterSuiteListener;
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
 * SUITE_NAME, TEST_THREAD_COUNT, TEST_PARALLEL_MODE, DATA_PROVIDER_THREAD_COUNT.
 * Used default values if not overridden.
 * </p>
 */
public class SuiteListener implements IAlterSuiteListener {

  private static final int SINGLE_THREAD_COUNT = 1;
  private static final TestNGExtension CONFIG = ConfigRegistry.get(TestNGExtension.class);

  @Override
  public void alter(List<XmlSuite> suites) {
    Map<XmlClass, List<Method>> classesWithTests = getClassesWithTestsFromSuites(suites);
    XmlSuite suite = generateSuite(classesWithTests);
    suites.clear();
    suites.add(suite);
    IAlterSuiteListener.super.alter(suites);
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
   *
   * @param tests provided xml classes with tests
   * @return generated xml suite as {@link XmlSuite}
   */
  protected XmlSuite generateSuite(Map<XmlClass, List<Method>> tests) {
    XmlSuite newSuite = new XmlSuite();
    newSuite.setName(getSuiteName());
    newSuite.setDataProviderThreadCount(getDataProviderThreadCount());
    XmlTest multiThreadTest = generateGroupXmlTest(newSuite, tests, ThreadGroup.MULTIPLY_THREADS);
    multiThreadTest.setName("Concurrency");
    multiThreadTest.setParallel(getTestParallelMode());
    multiThreadTest.setThreadCount(getTestThreadCount());
    XmlTest singleThreadTest = generateGroupXmlTest(newSuite, tests, ThreadGroup.SINGLE_THREAD);
    singleThreadTest.setName("Single thread tests");
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
