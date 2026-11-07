package dev.quokkify.mongodb.test;

import java.time.Duration;
import java.util.List;

import dev.quokkify.mongodb.entity.QTestUser;
import dev.quokkify.mongodb.entity.TestUser;
import dev.quokkify.mongodb.service.MongoFactory;
import dev.quokkify.mongodb.steps.MongoDatabaseSteps;

import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoDatabaseStepsTest {

  private static final String MONGODB_URL = System.getenv().getOrDefault("MONGODB_URL", "mongodb://localhost:27017");
  private static final String DATABASE = "q4j_mongodb_test";
  private static final String UNSAVED_MESSAGE = "dev.quokkify.mongodb.entity.TestUser has no _id; save it first";

  private final QTestUser q = QTestUser.testUser;
  private MongoClient client;
  private MongoFactory factory;
  private MongoDatabaseSteps steps;

  @BeforeClass(alwaysRun = true)
  public void setUp() {
    client = MongoClients.create(MONGODB_URL);
    factory = new MongoFactory(client, DATABASE);
    steps = new MongoDatabaseSteps(factory);
  }

  @AfterClass(alwaysRun = true)
  public void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @BeforeMethod(alwaysRun = true)
  public void clean() {
    factory.collection(TestUser.class).deleteMany(Filters.empty());
  }

  @Test
  public void checkSaveAssignsId() {
    TestUser user = user("Ann", "Lee");

    steps.save(user);

    assertThat(user.getId()).isNotNull();
    TestUser found = steps.selectQuery(q).where(q.id.eq(user.getId())).fetchOne();
    assertThat(found).usingRecursiveComparison().isEqualTo(user);
  }

  @Test
  public void checkSaveList() {
    steps.save(List.of(user("Ann", "Lee"), user("Bob", "Lee")));

    assertThat(steps.selectQuery(q).fetch()).hasSize(2);

    assertThatCode(() -> steps.save(List.<TestUser>of())).doesNotThrowAnyException();
    assertThat(steps.selectQuery(q).fetch()).hasSize(2);
  }

  @Test
  public void checkSaveTwiceFails() {
    TestUser user = user("Ann", "Lee");
    steps.save(user);

    assertThatThrownBy(() -> steps.save(user)).isInstanceOf(MongoWriteException.class);
  }

  @Test
  public void checkUpdate() {
    TestUser user = user("Ann", "Lee");
    TestUser other = user("Bob", "Lee");
    steps.save(List.of(user, other));

    UpdateResult result = steps.update(user, Updates.set("lastName", "Pop"));

    assertThat(result.getModifiedCount()).isEqualTo(1);
    assertThat(steps.selectQuery(q).where(q.id.eq(user.getId())).fetchOne().getLastName()).isEqualTo("Pop");
    assertThat(steps.selectQuery(q).where(q.id.eq(other.getId())).fetchOne().getLastName()).isEqualTo("Lee");
  }

  @Test
  public void checkUpdateWithoutOperationsFails() {
    TestUser user = user("Ann", "Lee");
    steps.save(user);

    assertThatThrownBy(() -> steps.update(user))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("update requires at least one update operation");
  }

  /**
   * Regression guard: the legacy delete removed every document of the class.
   */
  @Test
  public void checkDeleteRemovesOnlyThisDocument() {
    TestUser first = user("Ann", "Lee");
    TestUser second = user("Bob", "Lee");
    steps.save(List.of(first, second));

    DeleteResult result = steps.delete(first);

    assertThat(result.getDeletedCount()).isEqualTo(1);
    assertThat(steps.selectQuery(q).fetch()).usingRecursiveFieldByFieldElementComparator().containsExactly(second);
  }

  @Test
  public void checkUnsavedEntityFails() {
    assertThatThrownBy(() -> steps.update(new TestUser(), Updates.set("age", 1)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(UNSAVED_MESSAGE);
    assertThatThrownBy(() -> steps.delete(new TestUser()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(UNSAVED_MESSAGE);
  }

  @Test
  public void checkVerifyHasDocument() {
    steps.save(user("Ann", "Lee"));

    steps.verify().withTimeout(Duration.ofSeconds(5))
        .hasDocument(() -> steps.selectQuery(q).fetch(), x -> "Ann".equals(x.getFirstName()));
    steps.verify().withTimeout(Duration.ofSeconds(2))
        .doesNotHaveDocument(() -> steps.selectQuery(q).fetch(), x -> "Zed".equals(x.getFirstName()));
  }

  private static TestUser user(String firstName, String lastName) {
    TestUser user = new TestUser();
    user.setFirstName(firstName);
    user.setLastName(lastName);
    return user;
  }
}
