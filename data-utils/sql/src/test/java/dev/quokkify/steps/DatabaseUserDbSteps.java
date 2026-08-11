package dev.quokkify.steps;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.entity.QDatabaseTestUser;

import io.qameta.allure.Step;

public class DatabaseUserDbSteps extends DatabaseSteps<DatabaseUserVerification> {

  private final SqlDatabaseSteps sqlDatabaseSteps;

  public DatabaseUserDbSteps(SqlDatabaseSteps sqlDatabaseSteps) {
    this.sqlDatabaseSteps = sqlDatabaseSteps;
  }

  @Override
  public DatabaseUserVerification verify() {
    return new DatabaseUserVerification();
  }

  @Step("Get database user with '{expectedUser.firstName}' first name")
  public DatabaseTestUser getDatabaseTestUser(DatabaseTestUser expectedUser) {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    return sqlDatabaseSteps.selectDsl(databaseTestUser)
        .where(databaseTestUser.firstName.eq(expectedUser.getFirstName()))
        .fetchOne();
  }
}
