package dev.quokkify.steps;

import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.verification.DatabaseVerification;

import io.qameta.allure.Step;
import org.assertj.core.api.Assertions;

public class DatabaseUserVerification implements DatabaseVerification {

  @Step("Verify the last name of a database user")
  public DatabaseUserVerification verifyLastName(DatabaseTestUser entity, DatabaseTestUser expectedUser) {
    Assertions.assertThat(entity.getLastName()).as("Database user last name is incorrect")
        .isEqualTo(expectedUser.getLastName());
    return this;
  }
}
