package dev.quokkify.mongodb.test;

import dev.quokkify.mongodb.entity.Address;
import dev.quokkify.mongodb.entity.Role;
import dev.quokkify.mongodb.entity.TestUser;
import dev.quokkify.mongodb.service.MongoFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoFactoryTest {

  private static final String MONGODB_URL = System.getenv().getOrDefault("MONGODB_URL", "mongodb://localhost:27017");
  private static final String DATABASE = "q4j_mongodb_test";

  private MongoClient client;
  private MongoFactory factory;

  @BeforeClass(alwaysRun = true)
  public void setUp() {
    client = MongoClients.create(MONGODB_URL);
    factory = new MongoFactory(client, DATABASE);
  }

  @AfterClass(alwaysRun = true)
  public void tearDown() {
    if (client != null) {
      client.close();
    }
  }

  @BeforeMethod(alwaysRun = true)
  public void cleanUp() {
    factory.collection(TestUser.class).deleteMany(Filters.empty());
  }

  @Test
  public void checkCollectionNameFromAnnotation() {
    assertThat(factory.collection(TestUser.class).getNamespace().getCollectionName()).isEqualTo("test_users");
    assertThat(factory.documents(TestUser.class).getNamespace().getCollectionName()).isEqualTo("test_users");
  }

  @Test
  public void checkUnannotatedClassFails() {
    assertThatThrownBy(() -> factory.collection(Unannotated.class))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(Unannotated.class.getName() + " is not annotated with @MongoDocument");
  }

  @Test
  public void checkPojoRoundTrip() {
    Address address = new Address();
    address.setCity("Minsk");
    address.setZipCode("220000");
    TestUser user = new TestUser();
    user.setFirstName("Ann");
    user.setLastName("Lee");
    user.setAge(30);
    user.setRole(Role.ADMIN);
    user.setAddress(address);

    factory.collection(TestUser.class).insertOne(user);

    assertThat(user.getId()).isNotNull();
    TestUser loaded = factory.collection(TestUser.class).find(Filters.eq("_id", user.getId())).first();
    assertThat(loaded).usingRecursiveComparison().isEqualTo(user);
  }

  static class Unannotated {
  }
}
