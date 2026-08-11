package dev.quokkify.scan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.MethodInfo;
import io.github.classgraph.ScanResult;

public class ClasspathScanner {

  /**
   * Find all methods annotated with the given annotation under the given package,
   * using ClassGraph.
   */
  public static Set<Method> getMethodsWithAnnotationFromPackage(String packageName,
                                                                Class<? extends Annotation> annotation) {
    try (ScanResult scan = new ClassGraph()
        .acceptPackages(packageName)
        .enableClassInfo()
        .enableMethodInfo()
        .enableAnnotationInfo()
        .scan()) {
      Set<Method> methods = scan
          .getClassesWithMethodAnnotation(annotation.getName())
          .stream()
          .flatMap((ClassInfo ci) -> ci.getMethodInfo().stream())
          .filter((MethodInfo mi) -> mi.hasAnnotation(annotation.getName()))
          .map(methodInfo -> {
            try {
              return methodInfo.loadClassAndGetMethod();
            } catch (Throwable t) {
              throw new IllegalStateException(
                  "Failed to load method %s.%s"
                      .formatted(methodInfo.getClassInfo().getName(), methodInfo.getName()), t);
            }
          })
          .collect(Collectors.toCollection(LinkedHashSet::new));
      return methods;
    }
  }
}
