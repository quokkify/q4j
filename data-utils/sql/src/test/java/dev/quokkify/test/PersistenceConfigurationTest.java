package dev.quokkify.test;

import java.util.Map;
import java.util.UUID;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.persistence.PersistenceConfigurationFactory;
import dev.quokkify.persistence.PersistenceItem;
import dev.quokkify.provider.DatabaseStage;
import dev.quokkify.provider.PersistenceItemProvider;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.service.SqlFactory;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.assertj.core.api.Assertions;
import org.example.entity.ConsumerEntity;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.jpa.HibernatePersistenceConfiguration;
import org.hibernate.jpa.HibernatePersistenceProvider;
import org.testng.annotations.Test;

public class PersistenceConfigurationTest extends BaseDatabaseTest {

  @Test(description = "Factory builds a resource-local Hibernate configuration with discovered entities")
  public void checkFactoryBuildsResourceLocalHibernateConfiguration() {
    Map<String, Object> props = getH2Properties();
    PersistenceConfiguration configuration = PersistenceConfigurationFactory.create("cfg", props);
    Assertions.assertThat(configuration.name()).isEqualTo("cfg");
    Assertions.assertThat(configuration.provider()).isEqualTo(HibernatePersistenceProvider.class.getName());
    Assertions.assertThat(configuration.transactionType())
        .isEqualTo(PersistenceUnitTransactionType.RESOURCE_LOCAL);
    Assertions.assertThat(configuration.properties()).containsAllEntriesOf(props);
    Assertions.assertThat(configuration.managedClasses())
        .contains(DatabaseTestUser.class, ConsumerEntity.class);
  }

  @Test(description = "The same persistence name returns the cached SqlFactory")
  public void checkSameNameReturnsCachedFactory() {
    String name = "cached-" + UUID.randomUUID();
    Map<String, Object> props = getH2Properties();
    SqlFactory first = DatabaseService.getInstance()
        .createSqlQuery(PersistenceConfigurationFactory.create(name, props));
    SqlFactory second = DatabaseService.getInstance()
        .createSqlQuery(PersistenceConfigurationFactory.create(name, props));
    Assertions.assertThat(second).isSameAs(first);
  }

  @Test(description = "Null arguments are rejected")
  public void checkNullArgumentsRejected() {
    Map<String, Object> props = getH2Properties();
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> PersistenceConfigurationFactory.create(null, props));
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> PersistenceConfigurationFactory.create("x", null));
  }

  @Test(description = "Provider builds a cached configuration for a stage")
  public void checkProviderBuildsConfigurationForStage() {
    DatabaseStage stage = getStage();
    HibernatePersistenceConfiguration configuration = PersistenceItemProvider.getPersistenceConfiguration(stage);
    Assertions.assertThat(configuration.name()).isEqualTo("quokkify");
    Assertions.assertThat(configuration.properties())
        .containsEntry(AvailableSettings.POOL_SIZE, "100");
    Assertions.assertThat(PersistenceItemProvider.getPersistenceConfiguration(stage))
        .isSameAs(configuration);
  }

  @Test(description = "Regression guard: the deprecated PersistenceItem path and the new path share one cache")
  @SuppressWarnings("removal")
  public void checkDeprecatedAndNewPathShareCache() {
    Map<String, Object> props = getH2Properties();
    String itemFirst = "share-item-first-" + UUID.randomUUID();
    SqlFactory viaItem = DatabaseService.getInstance().createSqlQuery(new PersistenceItem(itemFirst, props));
    SqlFactory viaConfiguration = DatabaseService.getInstance()
        .createSqlQuery(PersistenceConfigurationFactory.create(itemFirst, props));
    Assertions.assertThat(viaConfiguration).isSameAs(viaItem);

    String configurationFirst = "share-cfg-first-" + UUID.randomUUID();
    SqlFactory viaConfigurationFirst = DatabaseService.getInstance()
        .createSqlQuery(PersistenceConfigurationFactory.create(configurationFirst, props));
    SqlFactory viaItemSecond = DatabaseService.getInstance()
        .createSqlQuery(new PersistenceItem(configurationFirst, props));
    Assertions.assertThat(viaItemSecond).isSameAs(viaConfigurationFirst);
  }
}
