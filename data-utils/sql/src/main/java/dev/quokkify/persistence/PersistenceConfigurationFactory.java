package dev.quokkify.persistence;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.PersistenceUnitTransactionType;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;

/**
 * Builds resource-local Hibernate {@link HibernatePersistenceConfiguration}s.
 */
public final class PersistenceConfigurationFactory {

  private PersistenceConfigurationFactory() {
  }

  /**
   * Create a configuration with every discovered {@link dev.quokkify.entity.SqlEntityInterface} entity.
   *
   * @param name       persistence unit name
   * @param properties persistence properties
   * @return configuration
   */
  public static HibernatePersistenceConfiguration create(String name, Map<String, Object> properties) {
    return create(name, properties, PersistenceClasspath.entityClassNames());
  }

  static HibernatePersistenceConfiguration create(String name, Map<String, ?> properties,
      List<String> entityClassNames) {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(properties, "properties");
    Objects.requireNonNull(entityClassNames, "entityClassNames");
    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    List<Class<?>> managedClasses = entityClassNames.stream()
        .<Class<?>>map(className -> load(className, classLoader))
        .toList();
    return new HibernatePersistenceConfiguration(name)
        .provider(HibernatePersistenceProvider.class.getName())
        .transactionType(PersistenceUnitTransactionType.RESOURCE_LOCAL)
        .properties(properties)
        .managedClasses(managedClasses);
  }

  private static Class<?> load(String className, ClassLoader classLoader) {
    try {
      return Class.forName(className, false, classLoader);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException("Cannot load managed class " + className, e);
    }
  }
}
