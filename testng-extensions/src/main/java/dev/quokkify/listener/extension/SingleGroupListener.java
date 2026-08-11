package dev.quokkify.listener.extension;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestNGExtension;

import org.testng.IAlterSuiteListener;
import org.testng.annotations.Test;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class SingleGroupListener implements IAlterSuiteListener {

  /**
   * Alter xml suites to generated suites by Test Groups.
   */
  @Override
  public void alter(List<XmlSuite> suites) {
    String testGroup = ConfigRegistry.get(TestNGExtension.class).testGroup();
    if (Objects.nonNull(testGroup) && !testGroup.isEmpty()) {
      List<XmlSuite> filteredSuites = filterSuitesByTestGroups(suites, testGroup);
      suites.clear();
      suites.addAll(filteredSuites);
      IAlterSuiteListener.super.alter(suites);
    }
  }

  private List<XmlSuite> filterSuitesByTestGroups(List<XmlSuite> suites, String testGroup) {
    return suites.stream().peek(suite -> {
      List<XmlTest> tests = suite.getTests().stream()
          .peek(test -> {
            List<XmlClass> classes = test.getClasses().stream()
                .peek(clazz -> {
                  List<Method> methods = Arrays.stream(clazz.getSupportClass().getDeclaredMethods())
                      .collect(Collectors.toList());
                  clazz.setIncludedMethods(filterMethodsByGroup(methods, testGroup));
                })
                .filter(clazz -> !clazz.getIncludedMethods().isEmpty())
                .collect(Collectors.toList());
            test.setClasses(classes);
          }).collect(Collectors.toList());
      suite.setTests(tests);
    }).collect(Collectors.toList());
  }

  private List<XmlInclude> filterMethodsByGroup(List<Method> methods, String testGroup) {
    if (Objects.isNull(testGroup) || testGroup.isEmpty()) {
      return convertMethodsToIncludes(methods);
    } else {
      List<Method> methodsWithGroup = methods.stream()
          .filter(method -> method.isAnnotationPresent(Test.class)
              && method.isAnnotationPresent(TestGroup.class)
              && Objects.nonNull(method.getAnnotation(TestGroup.class))
              && !method.getAnnotation(TestGroup.class).value().isEmpty()
              && testGroup.equalsIgnoreCase(method.getAnnotation(TestGroup.class).value()))
          .collect(Collectors.toList());
      return convertMethodsToIncludes(methodsWithGroup);
    }
  }

  private List<XmlInclude> convertMethodsToIncludes(List<Method> methods) {
    return methods.stream()
        .map(testMethod -> new XmlInclude(testMethod.getName(), Collections.emptyList(), 0))
        .collect(Collectors.toList());
  }
}
