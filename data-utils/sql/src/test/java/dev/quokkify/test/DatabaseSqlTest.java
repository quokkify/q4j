package dev.quokkify.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

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

import com.querydsl.core.CloseableIterator;
import io.qameta.allure.Allure;
import io.qameta.allure.AllureExternalKey;
import io.qameta.allure.AllureLifecycle;
import io.qameta.allure.AllureResultsWriter;
import io.qameta.allure.TmsLink;
import io.qameta.allure.model.Attachment;
import io.qameta.allure.model.StepResult;
import io.qameta.allure.model.TestResult;
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

  @TmsLink("SQL_DATABASE_ID_8")
  @Test(description = "Check stream returns the same record as fetch")
  public void checkStream() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<DatabaseTestUser> actualUsers;
    try (Stream<DatabaseTestUser> stream = databaseSteps.selectDsl(databaseTestUser)
        .where(databaseTestUser.firstName.eq(expectedUser.getFirstName())).stream()) {
      actualUsers = stream.toList();
    }
    Assertions.assertThat(actualUsers).usingRecursiveComparison()
        .isEqualTo(Collections.singleton(expectedUser));
  }

  @TmsLink("SQL_DATABASE_ID_9")
  @Test(description = "Check iterate returns the same record as fetch")
  public void checkIterate() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<DatabaseTestUser> actualUsers = new ArrayList<>();
    try (CloseableIterator<DatabaseTestUser> iterator = databaseSteps.selectDsl(databaseTestUser)
        .where(databaseTestUser.firstName.eq(expectedUser.getFirstName())).iterate()) {
      iterator.forEachRemaining(actualUsers::add);
    }
    Assertions.assertThat(actualUsers).usingRecursiveComparison()
        .isEqualTo(Collections.singleton(expectedUser));
  }

  @TmsLink("SQL_DATABASE_ID_10")
  @Test(description = "Check fetch attaches executed sql to allure")
  public void checkFetchAttachesSql() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<String> attachments = captureSqlAttachments(() -> databaseSteps.selectDsl(databaseTestUser).fetch());
    Assertions.assertThat(attachments).singleElement().asString().containsIgnoringCase("from");
  }

  @TmsLink("SQL_DATABASE_ID_11")
  @Test(description = "Check stream attaches executed sql to allure")
  public void checkStreamAttachesSql() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<String> attachments = captureSqlAttachments(() -> {
      try (Stream<DatabaseTestUser> stream = databaseSteps.selectDsl(databaseTestUser).stream()) {
        stream.count();
      }
    });
    Assertions.assertThat(attachments).singleElement().asString().containsIgnoringCase("from");
  }

  @TmsLink("SQL_DATABASE_ID_12")
  @Test(description = "Check iterate attaches executed sql to allure")
  public void checkIterateAttachesSql() {
    QDatabaseTestUser databaseTestUser = QDatabaseTestUser.databaseTestUser;
    List<String> attachments = captureSqlAttachments(() -> {
      try (CloseableIterator<DatabaseTestUser> iterator = databaseSteps.selectDsl(databaseTestUser).iterate()) {
        iterator.forEachRemaining(user -> { });
      }
    });
    Assertions.assertThat(attachments).singleElement().asString().containsIgnoringCase("from");
  }

  private static List<String> captureSqlAttachments(Runnable action) {
    InMemoryResultsWriter writer = new InMemoryResultsWriter();
    AllureLifecycle previous = Allure.getLifecycle();
    AllureLifecycle lifecycle = new AllureLifecycle(writer);
    Allure.setLifecycle(lifecycle);
    try {
      AllureExternalKey key = AllureExternalKey.random(DatabaseSqlTest.class);
      lifecycle.scheduleTest(key, new TestResult().setUuid(UUID.randomUUID().toString()).setName("sql attachments capture"));
      lifecycle.startTest(key);
      try {
        action.run();
      } finally {
        lifecycle.stopTest(key);
        lifecycle.writeTest(key);
      }
    } finally {
      Allure.setLifecycle(previous);
    }
    List<String> contents = new ArrayList<>();
    writer.results.forEach(result -> collectSqlAttachments(result.getAttachments(), result.getSteps(), writer, contents));
    return contents;
  }

  private static void collectSqlAttachments(List<Attachment> attachments, List<StepResult> steps,
      InMemoryResultsWriter writer, List<String> contents) {
    attachments.stream().filter(attachment -> "SQL query".equals(attachment.getName()))
        .forEach(attachment -> contents.add(writer.contents.get(attachment.getSource())));
    steps.forEach(step -> collectSqlAttachments(step.getAttachments(), step.getSteps(), writer, contents));
  }

  private static final class InMemoryResultsWriter implements AllureResultsWriter {
    private final List<TestResult> results = new ArrayList<>();
    private final Map<String, String> contents = new HashMap<>();

    @Override
    public void write(TestResult testResult) {
      results.add(testResult);
    }

    @Override
    public void write(io.qameta.allure.model.TestResultContainer testResultContainer) {
      // containers are not needed
    }

    @Override
    public void write(String source, InputStream attachment) {
      try {
        contents.put(source, new String(attachment.readAllBytes(), StandardCharsets.UTF_8));
      } catch (IOException e) {
        throw new IllegalStateException("Cannot read attachment " + source, e);
      }
    }
  }
}
