package dev.quokkify.provider;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import dev.quokkify.persistence.PersistenceItem;

/**
 * Provider for getting persistence item.
 */
public class PersistenceItemProvider {

  private static final Map<DatabaseStage, PersistenceItem> PERSISTENCE_ITEMS = new ConcurrentHashMap<>();

  private PersistenceItemProvider() {
  }

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
