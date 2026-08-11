package dev.quokkify.test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.entity.DatabaseTestUserIdentity;
import dev.quokkify.entity.QDatabaseTestUser;
import dev.quokkify.entity.QDatabaseTestUserIdentity;
import dev.quokkify.persistence.PersistenceItem;
import dev.quokkify.provider.PersistenceUnitDecorator;
import dev.quokkify.provider.PersistenceXmlProvider;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.service.SqlFactory;
import dev.quokkify.steps.DatabaseUserDbSteps;
import dev.quokkify.steps.SqlDatabaseSteps;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DatabaseSqlTest extends BaseDatabaseTest {

  private final DatabaseTestUser expectedUser = new DatabaseTestUser("Fillip", "Otlip");
  private final DatabaseTestUser updatedUser = new DatabaseTestUser("Anton", "Baton");
  private final DatabaseTestUser deletedUser = new DatabaseTestUser("Big", "Bon");
  private final DatabaseTestUserIdentity expectedUserIdentity = new DatabaseTestUserIdentity("Agrip", "Tipitip");

  private SqlDatabaseSteps databaseSteps;

  @BeforeClass(alwaysRun = true)
  public void initResources() {
    PersistenceItem persistenceItem = new PersistenceItem(this.getClass().getSimpleName(), getH2Properties());
    SqlFactory query = DatabaseService.getInstance().createSqlQuery(persistenceItem);
    databaseSteps = new SqlDatabaseSteps(query);
    databaseSteps.save(Arrays.asList(expectedUser, updatedUser, deletedUser, expectedUserIdentity));
  }

  @AfterClass(alwaysRun = true)
  public void closeResources() {
    databaseSteps.closeConnection();
  }

  @TmsLink("SQL_DATABASE_ID_1")
  @Test(description = "Check select record from database")
  public void checkSelect() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<DatabaseTestUser> actualUsers =
        databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.firstName.eq(expectedUser.getFirstName()))
            .fetch();
    Assertions.assertThat(actualUsers).usingRecursiveComparison()
        .isEqualTo(Collections.singleton(expectedUser));
  }

  @TmsLink("SQL_DATABASE_ID_2")
  @Test(description = "Check update record in database")
  public void checkUpdate() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    String newName = "Pop";
    DatabaseTestUser updatingUser = Objects.requireNonNull(databaseSteps.selectDsl(databaseTestUser)
        .where(databaseTestUser.firstName.eq(updatedUser.getFirstName())).fetchOne());
    updatingUser.setLastName(newName);
    databaseSteps.update(updatingUser);
    List<DatabaseTestUser> actualUser =
        databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.lastName.eq(newName)).fetch();
    Assertions.assertThat(actualUser).extracting(DatabaseTestUser::getLastName)
        .containsOnlyOnce(newName)
        .hasSize(1);
  }

  @TmsLink("SQL_DATABASE_ID_3")
  @Test(description = "Check insert record in to database")
  public void checkInsert() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    DatabaseTestUser insertedUser = new DatabaseTestUser("Agrip", "Tipitip");
    databaseSteps.save(insertedUser);
    List<DatabaseTestUser> actualUsers =
        databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.firstName.eq(insertedUser.getFirstName()))
            .fetch();
    Assertions.assertThat(actualUsers).usingRecursiveComparison()
        .isEqualTo(Collections.singleton(insertedUser));
  }

  @TmsLink("SQL_DATABASE_ID_4")
  @Test(description = "Check delete record from database")
  public void checkDelete() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    DatabaseTestUser deletingUser =
        databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.firstName.eq(deletedUser.getFirstName()))
            .fetchOne();
    databaseSteps.delete(deletingUser);
    DatabaseTestUser actualUser =
        databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.firstName.eq(deletedUser.getFirstName()))
            .fetchOne();
    Assertions.assertThat(actualUser).isNull();
  }

  @TmsLink("SQL_DATABASE_ID_5")
  @Test(description = "Check database steps and verification")
  public void checkDatabaseSteps() {
    DatabaseUserDbSteps databaseUserDbSteps = new DatabaseUserDbSteps(databaseSteps);
    DatabaseTestUser databaseTestUser = databaseUserDbSteps.getDatabaseTestUser(expectedUser);
    databaseUserDbSteps.verify()
        .verifyLastName(databaseTestUser, expectedUser);
  }

  @TmsLink("SQL_DATABASE_ID_6")
  @Test(description = "Check reading persistence.xml")
  public void checkReadingPersistenceXml() {
    PersistenceUnitDecorator hsql = PersistenceXmlProvider.getPersistenceUnit("hsql");
    Assertions.assertThat(hsql.getUserName()).isEqualTo("sa");
    Assertions.assertThat(hsql.getUserPassword()).isEqualTo("sa");
  }

  @TmsLink("SQL_DATABASE_ID_7")
  @Test(description = "Check select record from database")
  public void checkSelectViaIdentity() {
    QDatabaseTestUserIdentity databaseTestUser = QDatabaseTestUserIdentity.databaseTestUserIdentity;
    List<DatabaseTestUserIdentity> actualUsers =
        databaseSteps.selectDsl(databaseTestUser)
            .where(databaseTestUser.firstName.eq(expectedUserIdentity.getFirstName()))
            .fetch();
    Assertions.assertThat(actualUsers).usingRecursiveComparison()
        .isEqualTo(Collections.singleton(expectedUserIdentity));
  }
}
