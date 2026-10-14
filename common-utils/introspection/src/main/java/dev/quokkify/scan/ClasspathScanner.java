package dev.quokkify.scan;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Objects;
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
        .enableNonSystemModules()
        .enableClasspath()
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
              return loadMethod(methodInfo);
            } catch (Throwable t) {
              throw new IllegalStateException(
                  "Failed to load method %s.%s"
                      .formatted(methodInfo.getClassName(), methodInfo.getName()), t);
            }
          })
          .collect(Collectors.toCollection(LinkedHashSet::new));
      return methods;
    }
  }

  private static Method loadMethod(MethodInfo methodInfo) throws ClassNotFoundException, NoSuchMethodException {
    ClassLoader loader = Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(),
        ClasspathScanner.class.getClassLoader());
    Class<?> owner = Class.forName(methodInfo.getClassName(), false, loader);
    return Arrays.stream(owner.getDeclaredMethods())
        .filter(method -> method.getName().equals(methodInfo.getName()))
        .filter(method -> MethodType.methodType(method.getReturnType(), method.getParameterTypes())
            .toMethodDescriptorString().equals(methodInfo.getTypeDescriptorString()))
        .findFirst()
        .orElseThrow(() -> new NoSuchMethodException(
            "%s.%s%s".formatted(owner.getName(), methodInfo.getName(), methodInfo.getTypeDescriptorString())));
  }
}
