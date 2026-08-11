package dev.quokkify.provider;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import dev.quokkify.config.DatabaseConfig;
import dev.quokkify.util.FileUtils;

import org.aeonbits.owner.ConfigFactory;
import org.hibernate.c3p0.internal.C3P0ConnectionProvider;
import org.hibernate.cfg.AvailableSettings;

public class PersistencePropertiesProvider {

  private static final Map<DatabaseStage, DatabaseConfig> CONFIG_CACHE = new ConcurrentHashMap<>();
  private static final Map<DatabaseStage, Map<String, Object>> PROPS_CACHE = new ConcurrentHashMap<>();

  public static Map<String, Object> getPersistenceProperties(DatabaseStage databaseStage) {
    Objects.requireNonNull(databaseStage);
    return PROPS_CACHE.computeIfAbsent(databaseStage, PersistencePropertiesProvider::buildPersistenceProperties);
  }

  public static DatabaseConfig getDatabaseConfig(DatabaseStage databaseStage) {
    Objects.requireNonNull(databaseStage);
    return CONFIG_CACHE.computeIfAbsent(databaseStage, PersistencePropertiesProvider::loadConfig);
  }

  /**
   * Get {@link Map} of persistence properties.
   *
   * @param databaseStage {@link DatabaseStage} database stage type
   * @return persistence properties
   */
  private static Map<String, Object> buildPersistenceProperties(DatabaseStage databaseStage) {
    DatabaseConfig databaseConfig = loadConfig(databaseStage);
    Map<String, Object> props = Map.of(
        AvailableSettings.JAKARTA_JDBC_DRIVER, databaseConfig.driver(),
        AvailableSettings.JAKARTA_JDBC_URL, databaseConfig.url(),
        AvailableSettings.JAKARTA_JDBC_USER, databaseConfig.user(),
        AvailableSettings.JAKARTA_JDBC_PASSWORD, databaseConfig.password(),
        AvailableSettings.CONNECTION_PROVIDER, C3P0ConnectionProvider.class.getName(),
        AvailableSettings.POOL_SIZE, databaseConfig.poolSize());
    return Collections.unmodifiableMap(props);
  }

  /**
   * Load persistence configuration from stage configuration properties.
   *
   * @param databaseStage {@link DatabaseStage} database stage type
   * @return loaded database connection configuration properties
   */
  private static synchronized DatabaseConfig loadConfig(DatabaseStage databaseStage) {
    String path = databaseStage.getPersistencePropertyPath();
    Properties properties = new Properties();
    try (InputStream in = FileUtils.getNonNullResourceAsStream(path)) {
      properties.load(in);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to load DB properties from: " + path, e);
    }
    return ConfigFactory.create(DatabaseConfig.class, properties);
  }
}
