package dev.quokkify.service;

import java.util.HashMap;
import java.util.Map;

import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceConfiguration;

/**
 * Singleton service for work with databases.
 */
public class DatabaseService {

  private static volatile DatabaseService instance;
  private final Map<String, SqlFactory> sqlQueryFactories = new HashMap<>();

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
    if (!sqlQueryFactories.containsKey(persistenceName)) {
      SqlFactory sqlFactory = new SqlFactory(Persistence.createEntityManagerFactory(persistenceName));
      sqlQueryFactories.put(persistenceName, sqlFactory);
      return sqlFactory;
    } else {
      return sqlQueryFactories.get(persistenceName);
    }
  }

  /**
   * Create {@link SqlFactory} for work with SQL databases.
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
    String name = configuration.name();
    if (!sqlQueryFactories.containsKey(name)) {
      SqlFactory sqlFactory = new SqlFactory(configuration.createEntityManagerFactory());
      sqlQueryFactories.put(name, sqlFactory);
      return sqlFactory;
    } else {
      return sqlQueryFactories.get(name);
    }
  }
}
