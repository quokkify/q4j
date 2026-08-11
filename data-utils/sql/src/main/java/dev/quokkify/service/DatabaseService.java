package dev.quokkify.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.spi.PersistenceProvider;
import jakarta.persistence.spi.PersistenceProviderResolverHolder;

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
   */
  public SqlFactory createSqlQuery(dev.quokkify.persistence.PersistenceItem persistence) {
    if (!sqlQueryFactories.containsKey(persistence.getPersistenceUnitName())) {
      List<PersistenceProvider> providers = PersistenceProviderResolverHolder.getPersistenceProviderResolver()
          .getPersistenceProviders();
      EntityManagerFactory entityManagerFactory = providers.stream()
          .filter(provider -> provider.getClass().getName().equals(persistence.getPersistenceProviderClassName()))
          .findAny()
          .orElseThrow(() -> new PersistenceException("No Persistence provider for : "
              + persistence.getPersistenceProviderClassName()))
          .createContainerEntityManagerFactory(persistence, Map.of());
      if (Objects.isNull(entityManagerFactory)) {
        throw new PersistenceException("No Persistence provider for EntityManager named: "
            + persistence.getPersistenceUnitName());
      }
      SqlFactory sqlFactory = new SqlFactory(entityManagerFactory);
      sqlQueryFactories.put(persistence.getPersistenceUnitName(), sqlFactory);
      return sqlFactory;
    } else {
      return sqlQueryFactories.get(persistence.getPersistenceUnitName());
    }
  }
}
