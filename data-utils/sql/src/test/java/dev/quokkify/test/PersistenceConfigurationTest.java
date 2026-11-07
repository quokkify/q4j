package dev.quokkify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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

  @Test(description = "Regression guard: concurrent first use of one persistence name builds a single SqlFactory")
  public void checkConcurrentFirstUseReturnsSingleFactory() throws Exception {
    int threads = 8;
    String name = "concurrent-" + UUID.randomUUID();
    Map<String, Object> props = getH2Properties();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(threads);
    try {
      List<Future<SqlFactory>> futures = new ArrayList<>();
      for (int i = 0; i < threads; i++) {
        futures.add(executor.submit(() -> {
          start.await();
          return DatabaseService.getInstance()
              .createSqlQuery(PersistenceConfigurationFactory.create(name, props));
        }));
      }
      start.countDown();
      List<SqlFactory> factories = new ArrayList<>();
      for (Future<SqlFactory> future : futures) {
        factories.add(future.get(60, TimeUnit.SECONDS));
      }
      Assertions.assertThat(factories).hasSize(threads).allSatisfy(factory ->
          Assertions.assertThat(factory).isSameAs(factories.get(0)));
    } finally {
      executor.shutdownNow();
    }
  }

  @Test(description = "Explicit entities overload uses exactly the given classes")
  public void checkExplicitEntitiesOverloadUsesExactSet() {
    HibernatePersistenceConfiguration configuration = PersistenceConfigurationFactory.create(
        "explicit", getH2Properties(), List.<Class<?>>of(DatabaseTestUser.class));
    Assertions.assertThat(configuration.managedClasses()).containsExactly(DatabaseTestUser.class);
    Assertions.assertThatNullPointerException().isThrownBy(() -> PersistenceConfigurationFactory.create(
        "explicit", getH2Properties(), (java.util.Collection<Class<?>>) null));
  }

  @Test(description = "Null arguments are rejected")
  public void checkNullArgumentsRejected() {
    Map<String, Object> props = getH2Properties();
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> PersistenceConfigurationFactory.create(null, props));
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> PersistenceConfigurationFactory.create("x", null));
  }

  @Test(description = "Provider builds a configuration for a stage per call")
  public void checkProviderBuildsConfigurationForStage() {
    DatabaseStage stage = getStage();
    HibernatePersistenceConfiguration configuration = PersistenceItemProvider.getPersistenceConfiguration(stage);
    Assertions.assertThat(configuration.name()).isEqualTo("quokkify");
    Assertions.assertThat(configuration.properties())
        .containsEntry(AvailableSettings.POOL_SIZE, "100");
    HibernatePersistenceConfiguration again = PersistenceItemProvider.getPersistenceConfiguration(stage);
    Assertions.assertThat(again.name()).isEqualTo(configuration.name());
    Assertions.assertThat(again.properties()).isEqualTo(configuration.properties());
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
