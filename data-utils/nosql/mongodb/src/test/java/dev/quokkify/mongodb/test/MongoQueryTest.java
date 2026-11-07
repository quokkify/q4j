package dev.quokkify.mongodb.test;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import dev.quokkify.mongodb.entity.Address;
import dev.quokkify.mongodb.entity.QTestUser;
import dev.quokkify.mongodb.entity.Role;
import dev.quokkify.mongodb.entity.TestUser;
import dev.quokkify.mongodb.query.MongoQuery;
import dev.quokkify.mongodb.service.MongoFactory;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.core.types.Predicate;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MongoQueryTest {

  private static final String MONGODB_URL = System.getenv().getOrDefault("MONGODB_URL", "mongodb://localhost:27017");
  private static final String DATABASE = "q4j_mongodb_test";
  private static final String QUERY_ATTACHMENT = "Mongo query";

  private final QTestUser q = QTestUser.testUser;
  private MongoClient client;
  private MongoFactory factory;
  private TestUser ann;
  private TestUser bob;
  private TestUser cid;

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
  public void seed() {
    factory.collection(TestUser.class).deleteMany(Filters.empty());
    ann = user("Ann", "Lee", 30, Role.ADMIN, "Riga", "LV-1010");
    bob = user("Bob", "Lee", 25, Role.USER, "Oslo", "0150");
    cid = user("Cid", "Moe", 41, Role.USER, "Riga", "LV-1050");
    factory.collection(TestUser.class).insertMany(List.of(ann, bob, cid));
  }

  @Test
  public void checkFilterByField() {
    List<TestUser> users = query().where(q.lastName.eq("Lee")).fetch();

    assertThat(users).extracting(TestUser::getFirstName).containsExactlyInAnyOrder("Ann", "Bob");
  }

  @Test
  public void checkFilterById() {
    TestUser found = query().where(q.id.eq(bob.getId())).fetchOne();

    assertThat(found).usingRecursiveComparison().isEqualTo(bob);
  }

  @Test
  public void checkFilterByRenamedField() {
    TestUser found = query().where(q.firstName.eq("Cid")).fetchOne();

    assertThat(found).usingRecursiveComparison().isEqualTo(cid);
  }

  @Test
  public void checkFilterByRenamedNestedField() {
    List<TestUser> users = query().where(q.address.zipCode.eq("0150")).fetch();

    assertThat(users).singleElement().extracting(TestUser::getFirstName).isEqualTo("Bob");
  }

  @Test
  public void checkFilterByNestedFieldAndEnum() {
    List<TestUser> users = query().where(q.address.city.eq("Riga"), q.role.eq(Role.USER)).fetch();

    assertThat(users).singleElement().extracting(TestUser::getFirstName).isEqualTo("Cid");
  }

  @Test
  public void checkNoMatch() {
    assertThat(query().where(q.lastName.eq("Nobody")).fetch()).isEmpty();
    assertThat(query().where(q.lastName.eq("Nobody")).fetchFirst()).isNull();
  }

  @Test
  public void checkFetchOneWithSeveralMatchesFails() {
    assertThatThrownBy(() -> query().where(q.lastName.eq("Lee")).fetchOne())
        .isInstanceOf(NonUniqueResultException.class);
  }

  @Test
  public void checkQueryOnUnannotatedClassFails() {
    assertThatThrownBy(() -> new MongoQuery<>(factory, Unannotated.class).fetch())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage(Unannotated.class.getName() + " is not annotated with @MongoDocument");
  }

  @Test
  public void checkFetchAttachesQuery() {
    assertQueryAttached(MongoQuery::fetch);
    assertQueryAttached(MongoQuery::fetchFirst);
    assertQueryAttached(MongoQuery::fetchOne);
  }

  private void assertQueryAttached(Consumer<MongoQuery<TestUser>> execution) {
    Predicate moe = q.lastName.eq("Moe");
    List<Map.Entry<String, String>> attachments = AllureCapture.run(() -> execution.accept(query().where(moe)));

    assertThat(attachments).singleElement().satisfies(attachment -> {
      assertThat(attachment.getKey()).isEqualTo(QUERY_ATTACHMENT);
      assertThat(attachment.getValue()).contains("lastName").contains("Moe");
    });
  }

  private MongoQuery<TestUser> query() {
    return new MongoQuery<>(factory, TestUser.class);
  }

  private static TestUser user(String firstName, String lastName, int age, Role role, String city, String zipCode) {
    Address address = new Address();
    address.setCity(city);
    address.setZipCode(zipCode);
    TestUser user = new TestUser();
    user.setFirstName(firstName);
    user.setLastName(lastName);
    user.setAge(age);
    user.setRole(role);
    user.setAddress(address);
    return user;
  }

  static class Unannotated {
  }
}
