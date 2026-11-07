package dev.quokkify.provider;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dev.quokkify.persistence.PersistenceConfigurationFactory;
import dev.quokkify.persistence.PersistenceItem;

import org.hibernate.jpa.HibernatePersistenceConfiguration;

/**
 * Provider for getting persistence item.
 */
public class PersistenceItemProvider {

  @SuppressWarnings("removal")
  private static final Map<DatabaseStage, PersistenceItem> PERSISTENCE_ITEMS = new ConcurrentHashMap<>();
  private static final Map<DatabaseStage, HibernatePersistenceConfiguration> PERSISTENCE_CONFIGURATIONS =
      new ConcurrentHashMap<>();

  private PersistenceItemProvider() {
  }

  /**
   * Get the cached persistence configuration for a stage.
   *
   * @param databaseStage database stage
   * @return configuration built from the stage project name and persistence properties
   */
  public static HibernatePersistenceConfiguration getPersistenceConfiguration(DatabaseStage databaseStage) {
    Objects.requireNonNull(databaseStage);
    return PERSISTENCE_CONFIGURATIONS.computeIfAbsent(
        databaseStage,
        stage -> PersistenceConfigurationFactory.create(
            stage.getProjectName(),
            PersistencePropertiesProvider.getPersistenceProperties(stage)
        )
    );
  }

  /**
   * Get the cached persistence item for a stage.
   *
   * @param databaseStage database stage
   * @return persistence item
   * @deprecated use {@link #getPersistenceConfiguration(DatabaseStage)}
   */
  @Deprecated(since = "0.14.0", forRemoval = true)
  @SuppressWarnings("removal")
  public static PersistenceItem getPersistenceItem(DatabaseStage databaseStage) {
    Objects.requireNonNull(databaseStage);
    return PERSISTENCE_ITEMS.computeIfAbsent(
        databaseStage,
        stage -> new PersistenceItem(
            stage.getProjectName(),
            PersistencePropertiesProvider.getPersistenceProperties(stage)
        )
    );
  }
}
