package dev.quokkify.test;

import java.util.List;
import java.util.UUID;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.converter.SqlConverter;
import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.entity.QDatabaseTestUser;
import dev.quokkify.persistence.PersistenceItem;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.service.SqlFactory;
import dev.quokkify.steps.SqlDatabaseSteps;

import io.qameta.allure.TmsLink;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.assertj.core.api.Assertions;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class SqlConverterTest extends BaseDatabaseTest {

  private static final String name = "Adam";
  private static final String lastName = "Sadler";
  private static final List<String> sequences = List.of(
      String.join(StringUtils.SPACE,
          "select dtu1_0.id,dtu1_0.firstName,dtu1_0.lastName",
          "from DatabaseTestUser dtu1_0",
          "where dtu1_0.firstName=?",
          "and dtu1_0.lastName=?"
      ),
      "Replacing '?' with parameter[1] = '%s'".formatted(name),
      "Replacing '?' with parameter[2] = '%s'".formatted(lastName)
  );
  private static final String extraSequence = "fetch first ? rows only";

  private final DatabaseTestUser user = new DatabaseTestUser(name, lastName);
  private final QDatabaseTestUser targetEntity = QDatabaseTestUser.databaseTestUser;

  private SqlDatabaseSteps databaseSteps;

  @BeforeMethod(alwaysRun = true)
  public void initResources() {
    PersistenceItem persistenceItem = new PersistenceItem(UUID.randomUUID().toString(), getH2Properties());
    SqlFactory query = DatabaseService.getInstance().createSqlQuery(persistenceItem);
    databaseSteps = new SqlDatabaseSteps(query);
  }

  @AfterMethod(alwaysRun = true)
  public void closeResources() {
    databaseSteps.closeConnection();
  }

  @TmsLink("SQL_CONVERTER_ID_1")
  @Test(description = "Check sql converting when 'fetchFirst'")
  @SingleThread
  public void checkConvertingWhenFetchFirst() {
    int questions = 3;
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.eq(user.getFirstName()).and(targetEntity.lastName.eq(user.getLastName())))
        .fetchFirst()
    );
    Assertions.assertThat(appender.getMessages().getFirst().chars().filter(ch -> ch == '?').count())
        .as("Expected '?' count should be equals to {}", questions)
        .isEqualTo(questions);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain all lines")
        .contains(sequences);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain line")
        .contains(extraSequence);
  }

  @TmsLink("SQL_CONVERTER_ID_2")
  @Test(description = "Check sql converting when 'fetch'")
  @SingleThread
  public void checkConvertingWhenFetch() {
    int questions = 2;
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.eq(user.getFirstName()).and(targetEntity.lastName.eq(user.getLastName())))
        .fetch()
    );
    Assertions.assertThat(appender.getMessages().getFirst().chars().filter(ch -> ch == '?').count())
        .as("Expected '?' count should be equals to {}", questions)
        .isEqualTo(questions);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain all lines")
        .contains(sequences);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain line")
        .doesNotContain(extraSequence);
  }

  @TmsLink("SQL_CONVERTER_ID_3")
  @Test(description = "Check sql converting when 'fetchOne'")
  @SingleThread
  public void checkConvertingWhenFetchOne() {
    int questions = 2;
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.eq(user.getFirstName()).and(targetEntity.lastName.eq(user.getLastName())))
        .fetchOne()
    );
    Assertions.assertThat(appender.getMessages().getFirst().chars().filter(ch -> ch == '?').count())
        .as("Expected '?' count should be equals to {}", questions)
        .isEqualTo(questions);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain all lines")
        .contains(sequences);
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should not contain line")
        .doesNotContain(extraSequence);
  }

  @TmsLink("SQL_CONVERTER_ID_4")
  @Test(description = "Escaping: single quote in string parameter should be doubled in Executable SQL")
  @SingleThread
  public void checkEscapingSingleQuote() {
    DatabaseTestUser testEntity = new DatabaseTestUser("O'Hara", "D'Angelo");
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.eq(testEntity.getFirstName())
            .and(targetEntity.lastName.eq(testEntity.getLastName())))
        .fetchOne()
    );
    String log = appender.getCombinedLog();
    Assertions.assertThat(log)
        .as("Executable SQL should contain escaped single quotes for firstName")
        .contains("O''Hara");
    Assertions.assertThat(log)
        .as("Executable SQL should contain escaped single quotes for lastName")
        .contains("D''Angelo");
    Assertions.assertThat(log)
        .as("Executable SQL should not contain unescaped single quotes variant")
        .doesNotContain("O'Hara", "D'Angelo");
  }

  @TmsLink("SQL_CONVERTER_ID_5")
  @Test(description = "Limit/Offset: placeholders should be replaced with numeric values in Executable SQL")
  @SingleThread
  public void checkLimitAndOffsetReplacement() {
    DatabaseTestUser testEntity = new DatabaseTestUser(name, lastName);
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.eq(testEntity.getFirstName())
            .and(targetEntity.lastName.eq(testEntity.getLastName())))
        .offset(10)
        .limit(5)
        .fetch()
    );
    String log = normalize(appender.getCombinedLog());
    Assertions.assertThat(log)
        .as("Executable SQL should contain FETCH FIRST 5 ROWS ONLY")
        .contains("fetch first 5 rows only");
    Assertions.assertThat(log)
        .as("Executable SQL should contain OFFSET 10 ROWS")
        .contains("offset 10 rows");
  }

  @TmsLink("SQL_CONVERTER_ID_6")
  @Test(description = "IN: simple string collection should expand into ('a','b','c')")
  @SingleThread
  public void checkInWithStringCollection() {
    var names = List.of("Alice", "Bob", "Charlie");
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.firstName.in(names))
        .fetch()
    );
    String log = normalize(appender.getCombinedLog());
    Assertions.assertThat(log)
        .as("IN should expand string collection")
        .contains("in ('Alice', 'Bob', 'Charlie')");
    Assertions.assertThat(log)
        .as("IN should not be rendered as a single '[a, b, c]' literal")
        .doesNotContain("['Alice', 'Bob', 'Charlie']")
        .doesNotContain("[Alice, Bob, Charlie]");
  }

  @TmsLink("SQL_CONVERTER_ID_7")
  @Test(description = "IN: string items with single quotes must be escaped (O'Hara -> O''Hara)")
  @SingleThread
  public void checkInWithEscapingInCollection() {
    var tricky = List.of("O'Hara", "D'Angelo");
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.lastName.in(tricky))
        .fetch()
    );
    String log = normalize(appender.getCombinedLog());
    Assertions.assertThat(log)
        .as("Escaping inside IN should double single quotes")
        .contains("in ('O''Hara', 'D''Angelo')");
    Assertions.assertThat(log)
        .as("Unescaped literals must not appear")
        .doesNotContain("in ('O'Hara', 'D'Angelo')");
  }

  @TmsLink("SQL_CONVERTER_ID_8")
  @Test(description = "IN: numeric collection should render without quotes")
  @SingleThread
  public void checkInWithNumericCollection() {
    var ids = List.of(1L, 2L, 3L);
    TestLogAppender appender = withLogging(() -> databaseSteps
        .selectDsl(targetEntity)
        .where(targetEntity.id.in(ids))
        .fetch()
    );
    String log = normalize(appender.getCombinedLog());
    Assertions.assertThat(log)
        .as("Numeric IN should not quote numbers")
        .contains("in (1, 2, 3)");
    Assertions.assertThat(log)
        .as("Numeric IN should not render quoted numbers")
        .doesNotContain("in ('1', '2', '3')");
  }

  private static TestLogAppender withLogging(Runnable logic) {
    return withLogging(SqlConverter.class.getName(), logic, Level.TRACE);
  }

  private static String normalize(String s) {
    return s.replace("\r", "").replaceAll("\\s+", " ").trim();
  }
}
