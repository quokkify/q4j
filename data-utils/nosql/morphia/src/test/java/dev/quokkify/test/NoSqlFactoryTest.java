package dev.quokkify.test;

import dev.quokkify.service.NoSqlFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.config.MorphiaConfig;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

public class NoSqlFactoryTest {

  private final MongoClient mongoClient = MongoClients.create();

  @AfterClass(alwaysRun = true)
  public void closeMongoClient() {
    mongoClient.close();
  }

  @Test(description = "Reject a null Mongo client for database-name configuration")
  public void rejectNullMongoClientForDatabaseName() {
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> new NoSqlFactory(null, "test"))
        .withMessage("mongoClient");
  }

  @Test(description = "Reject a null database name")
  public void rejectNullDatabaseName() {
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> new NoSqlFactory(mongoClient, (String) null))
        .withMessage("dbName");
  }

  @Test(description = "Reject a null Mongo client for Morphia configuration")
  public void rejectNullMongoClientForMorphiaConfig() {
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> new NoSqlFactory(null, MorphiaConfig.load()))
        .withMessage("mongoClient");
  }

  @Test(description = "Reject a null Morphia configuration")
  public void rejectNullMorphiaConfig() {
    Assertions.assertThatNullPointerException()
        .isThrownBy(() -> new NoSqlFactory(mongoClient, (MorphiaConfig) null))
        .withMessage("morphiaConfig");
  }
}
