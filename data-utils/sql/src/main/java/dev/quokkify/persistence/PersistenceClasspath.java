package dev.quokkify.persistence;

import java.util.List;

import dev.quokkify.entity.SqlEntityInterface;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.ScanResult;
import jakarta.persistence.Entity;

final class PersistenceClasspath {

  private PersistenceClasspath() {
  }

  static List<String> entityClassNames() {
    return Holder.ENTITY_CLASS_NAMES;
  }

  private static final class Holder {

    private static final List<String> ENTITY_CLASS_NAMES = scan();

    private static List<String> scan() {
      try (ScanResult scanResult = new ClassGraph()
          .enableNonSystemModules()
          .enableClasspath()
          .enableClassInfo()
          .enableAnnotationInfo()
          .ignoreClassVisibility()
          .scan()) {
        return scanResult.getAllClassesImplementing(SqlEntityInterface.class.getName())
            .filter(classInfo -> classInfo.hasAnnotation(Entity.class.getName()))
            .stream()
            .map(ClassInfo::getName)
            .sorted()
            .toList();
      }
    }
  }
}
