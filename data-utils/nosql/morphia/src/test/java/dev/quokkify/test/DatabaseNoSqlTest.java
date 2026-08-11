package dev.quokkify.test;

import java.util.List;

import dev.quokkify.entity.nosql.DatabaseTestUserMongo;
import dev.quokkify.service.NoSqlFactory;
import dev.quokkify.steps.MongoDatabaseSteps;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.morphia.config.MorphiaConfig;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DatabaseNoSqlTest extends BaseDatabaseTest {

  private final DatabaseTestUserMongo insertedUser = new DatabaseTestUserMongo("Agrip", "Tipitip");
  private final DatabaseTestUserMongo expectedUser = new DatabaseTestUserMongo("Fillip", "Otlip");
  private final DatabaseTestUserMongo updatedUser = new DatabaseTestUserMongo("Anton", "Baton");
  private final DatabaseTestUserMongo deletedUser = new DatabaseTestUserMongo("Big", "Bon");

  private MongoClient mongoClient;
  private MongoDatabaseSteps mongoDatabaseSteps;

  @BeforeClass(alwaysRun = true)
  public void initResources() {
    String mongoUrl = System.getenv().getOrDefault("MONGODB_URL", "mongodb://localhost:27017");
    mongoClient = MongoClients.create(mongoUrl);
    MorphiaConfig morphiaConfig = MorphiaConfig.load()
        .database("test")
        .packages(List.of(DatabaseTestUserMongo.class.getPackageName()))
        .applyIndexes(true);
    NoSqlFactory noSqlFactory = new NoSqlFactory(mongoClient, morphiaConfig);
    mongoDatabaseSteps = new MongoDatabaseSteps(noSqlFactory);
  }

  @AfterClass(alwaysRun = true)
  public void closeResources() {
    mongoClient.close();
  }

  @TmsLink("NO_SQL_DATABASE_ID_1")
  @Test(description = "Check select record from database")
  public void checkSelect() {
    mongoDatabaseSteps.save(expectedUser);
    DatabaseTestUserMongo actual = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.and(
            Filters.eq("_id", expectedUser.getId()),
            Filters.eq("firstName", expectedUser.getFirstName())
        )).first();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(expectedUser);
  }

  @TmsLink("NO_SQL_DATABASE_ID_2")
  @Test(description = "Check update record in database")
  public void checkUpdate() {
    mongoDatabaseSteps.save(updatedUser);
    String newLastName = "Pop";
    DatabaseTestUserMongo userToUpdate = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.eq("_id", updatedUser.getId()))
        .first();
    mongoDatabaseSteps.update(userToUpdate, List.of(UpdateOperators.set("lastName", newLastName)));
    DatabaseTestUserMongo actual = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.eq("firstName", updatedUser.getFirstName()))
        .first();
    Assertions.assertThat(actual.getLastName()).isEqualTo(newLastName);
    Assertions.assertThat(actual.getFirstName()).isEqualTo(updatedUser.getFirstName());
  }

  @TmsLink("NO_SQL_DATABASE_ID_3")
  @Test(description = "Check insert record into database")
  public void checkInsert() {
    mongoDatabaseSteps.save(insertedUser);
    DatabaseTestUserMongo actual = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.and(
            Filters.eq("_id", insertedUser.getId()),
            Filters.eq("firstName", insertedUser.getFirstName())
        )).first();
    Assertions.assertThat(actual)
        .usingRecursiveComparison()
        .isEqualTo(insertedUser);
  }

  @TmsLink("NO_SQL_DATABASE_ID_4")
  @Test(description = "Check delete records from database")
  public void checkDelete() {
    mongoDatabaseSteps.save(deletedUser);
    DatabaseTestUserMongo userToDelete = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.eq("_id", deletedUser.getId()))
        .first();
    mongoDatabaseSteps.delete(userToDelete);
    DatabaseTestUserMongo actual = mongoDatabaseSteps.selectDsl(DatabaseTestUserMongo.class)
        .filter(Filters.eq("firstName", deletedUser.getFirstName()))
        .first();
    Assertions.assertThat(actual).isNull();
  }
}
