package dev.quokkify.test;

import java.util.Map;
import java.util.UUID;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.persistence.PersistenceConfigurationFactory;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.service.SqlFactory;

import jakarta.persistence.PersistenceConfiguration;
import jakarta.persistence.PersistenceUnitTransactionType;
import org.assertj.core.api.Assertions;
import org.example.entity.ConsumerEntity;
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
}
