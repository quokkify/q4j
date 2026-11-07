package dev.quokkify.test;

import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.persistence.PersistenceConfigurationFactory;
import dev.quokkify.persistence.PersistenceItem;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.steps.SqlDatabaseSteps;

import jakarta.persistence.SharedCacheMode;
import jakarta.persistence.ValidationMode;
import org.assertj.core.api.Assertions;
import org.example.entity.ConsumerEntity;
import org.example.entity.QConsumerEntity;
import org.h2.jdbcx.JdbcDataSource;
import org.hibernate.cfg.AvailableSettings;
import org.testng.annotations.Test;

public class ConsumerEntityDiscoveryTest extends BaseDatabaseTest {

  @Test(description = "Regression guard: entities outside dev.quokkify are discovered via classpath scanning")
  @SuppressWarnings("removal")
  public void checkConsumerEntityViaPersistenceItem() {
    PersistenceItem persistenceItem = new PersistenceItem("consumer-" + UUID.randomUUID(), getH2Properties());
    SqlDatabaseSteps steps = new SqlDatabaseSteps(DatabaseService.getInstance().createSqlQuery(persistenceItem));
    try {
      steps.save(new ConsumerEntity("acme"));
      QConsumerEntity entity = QConsumerEntity.consumerEntity;
      ConsumerEntity found = steps.selectDsl(entity).where(entity.name.eq("acme")).fetchFirst();
      Assertions.assertThat(found).isNotNull();
    } finally {
      steps.closeConnection();
    }
  }

  @Test(description = "Regression guard: entities outside dev.quokkify are discovered via PersistenceConfiguration")
  public void checkConsumerEntityViaPersistenceConfiguration() {
    SqlDatabaseSteps steps = new SqlDatabaseSteps(DatabaseService.getInstance().createSqlQuery(
        PersistenceConfigurationFactory.create("consumer-cfg-" + UUID.randomUUID(), getH2Properties())));
    try {
      steps.save(new ConsumerEntity("acme"));
      QConsumerEntity entity = QConsumerEntity.consumerEntity;
      ConsumerEntity found = steps.selectDsl(entity).where(entity.name.eq("acme")).fetchFirst();
      Assertions.assertThat(found).isNotNull();
    } finally {
      steps.closeConnection();
    }
  }

  @Test(description = "Guards the override contract: a subclass's managed classes replace the discovered ones")
  @SuppressWarnings("removal")
  public void checkSubclassOverrideOfManagedClassesWins() {
    PersistenceItem persistenceItem =
        new PersistenceItem("override-" + UUID.randomUUID(), getH2Properties()) {
          @Override
          public List<String> getManagedClassNames() {
            return List.of(DatabaseTestUser.class.getName());
          }
        };
    Assertions.assertThat(persistenceItem.getManagedClassNames())
        .doesNotContain(ConsumerEntity.class.getName());
    SqlDatabaseSteps steps = new SqlDatabaseSteps(DatabaseService.getInstance().createSqlQuery(persistenceItem));
    try {
      steps.save(new DatabaseTestUser("Override", "Wins"));
    } finally {
      steps.closeConnection();
    }
  }

  @Test(description = "Guards the bridge: toPersistenceConfiguration honors the managed classes override")
  @SuppressWarnings("removal")
  public void checkBridgeHonorsManagedClassOverride() {
    PersistenceItem persistenceItem =
        new PersistenceItem("bridge-" + UUID.randomUUID(), getH2Properties()) {
          @Override
          public List<String> getManagedClassNames() {
            return List.of(DatabaseTestUser.class.getName());
          }
        };
    Assertions.assertThat(persistenceItem.toPersistenceConfiguration().managedClasses())
        .containsExactly(DatabaseTestUser.class);
  }

  @Test(description = "Guards the bridge: data source, shared cache mode and validation mode overrides are carried")
  @SuppressWarnings("removal")
  public void checkBridgeCarriesUnitInfoOverrides() {
    JdbcDataSource dataSource = new JdbcDataSource();
    PersistenceItem persistenceItem =
        new PersistenceItem("bridge-info-" + UUID.randomUUID(), getH2Properties()) {
          @Override
          public DataSource getNonJtaDataSource() {
            return dataSource;
          }

          @Override
          public SharedCacheMode getSharedCacheMode() {
            return SharedCacheMode.ENABLE_SELECTIVE;
          }

          @Override
          public ValidationMode getValidationMode() {
            return ValidationMode.NONE;
          }
        };
    var configuration = persistenceItem.toPersistenceConfiguration();
    Assertions.assertThat(configuration.properties().get(AvailableSettings.JAKARTA_NON_JTA_DATASOURCE))
        .isSameAs(dataSource);
    Assertions.assertThat(configuration.sharedCacheMode()).isEqualTo(SharedCacheMode.ENABLE_SELECTIVE);
    Assertions.assertThat(configuration.validationMode()).isEqualTo(ValidationMode.NONE);
  }
}
