package dev.quokkify.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;

/**
 * Singleton service for work with databases.
 */
public class DatabaseService {

  private static volatile DatabaseService instance;
  private final Map<String, SqlFactory> sqlQueryFactories = new ConcurrentHashMap<>();

  private DatabaseService() {
  }

  public static DatabaseService getInstance() {
    DatabaseService localInstance = instance;
    if (localInstance == null) {
      synchronized (DatabaseService.class) {
        localInstance = instance;
        if (localInstance == null) {
          instance = new DatabaseService();
        }
      }
    }
    return instance;
  }

  /**
   * Create {@link SqlFactory} for work with SQL databases.
   *
   * @param persistenceName name of persistence
   * @return {@link SqlFactory}
   */
  public SqlFactory createSqlQuery(String persistenceName) {
    return sqlQueryFactories.computeIfAbsent(persistenceName,
        name -> new SqlFactory(Persistence.createEntityManagerFactory(name)));
  }

  /**
   * Create {@link SqlFactory} for work with SQL databases.
   *
   * Only the name, properties, managed classes, non-JTA data source, mapping files, shared cache mode and
   * validation mode of the item are used; the JTA data source, transaction type (always resource-local),
   * persistence provider class name (always Hibernate), jar file URLs, excludeUnlistedClasses and class loader
   * are not.
   *
   * @param persistence Persistence class {@link dev.quokkify.persistence.PersistenceItem}
   * @return {@link SqlFactory}
   * @deprecated use {@link #createSqlQuery(PersistenceConfiguration)}
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  @SuppressWarnings("removal")
  public SqlFactory createSqlQuery(dev.quokkify.persistence.PersistenceItem persistence) {
    return createSqlQuery(persistence.toPersistenceConfiguration());
  }

  /**
   * Create {@link SqlFactory} for work with SQL databases. An already used persistence name returns the cached
   * factory and ignores the new configuration.
   *
   * @param configuration persistence configuration
   * @return {@link SqlFactory}
   */
  public SqlFactory createSqlQuery(PersistenceConfiguration configuration) {
    return sqlQueryFactories.computeIfAbsent(configuration.name(),
        name -> new SqlFactory(configuration.createEntityManagerFactory()));
  }
}
