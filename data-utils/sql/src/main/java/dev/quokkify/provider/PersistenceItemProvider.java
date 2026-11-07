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

  private PersistenceItemProvider() {
  }

  /**
   * Build a persistence configuration for a stage on every call.
   *
   * @param databaseStage database stage
   * @return configuration built from the stage project name and persistence properties
   */
  public static HibernatePersistenceConfiguration getPersistenceConfiguration(DatabaseStage databaseStage) {
    Objects.requireNonNull(databaseStage);
    return PersistenceConfigurationFactory.create(
        databaseStage.getProjectName(),
        PersistencePropertiesProvider.getPersistenceProperties(databaseStage)
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
