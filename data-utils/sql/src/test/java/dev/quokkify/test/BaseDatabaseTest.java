package dev.quokkify.test;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.quokkify.listener.lifecycle.SuiteListener;
import dev.quokkify.provider.DatabaseStage;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.hibernate.cfg.AvailableSettings;
import org.testng.annotations.Listeners;

@Listeners({SuiteListener.class})
abstract class BaseDatabaseTest {

  protected DatabaseStage getStage() {
    return new DatabaseStage() {
      @Override
      public String getProjectName() {
        return "quokkify";
      }

      @Override
      public String getPersistenceName() {
        return "local";
      }

      @Override
      public String getPersistencePropertyPath() {
        return "h2.properties";
      }
    };
  }

  protected static class TestLogAppender extends AbstractAppender {

    private final List<String> messages = new ArrayList<>();

    public TestLogAppender(String name) {
      super(name, null, PatternLayout.createDefaultLayout(), false, Property.EMPTY_ARRAY);
    }

    public List<String> getMessages() {
      return messages;
    }

    @Override
    public void append(LogEvent event) {
      messages.add(event.getMessage().getFormattedMessage());
    }

    public String getCombinedLog() {
      return String.join("\n", messages);
    }
  }

  protected static Map<String, Object> getH2Properties() {
    return Map.of(
        AvailableSettings.JAKARTA_JDBC_DRIVER, "org.h2.Driver",
        AvailableSettings.JAKARTA_JDBC_URL, "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL",
        AvailableSettings.JAKARTA_JDBC_USER, "sa",
        AvailableSettings.JAKARTA_JDBC_PASSWORD, "sa",
        AvailableSettings.ISOLATION, Connection.TRANSACTION_READ_COMMITTED,
        AvailableSettings.SHOW_SQL, "true",
        AvailableSettings.FORMAT_SQL, "true",
        AvailableSettings.HBM2DDL_AUTO, "update");
  }

  protected static TestLogAppender withLogging(String name, Runnable logic) {
    return withLogging(name, logic, Level.DEBUG);
  }

  protected static TestLogAppender withLogging(String name, Runnable logic, Level level) {
    String caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
        .walk(frames -> frames
            .skip(1)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Couldn't identify the calling class"))
            .getDeclaringClass()
            .getSimpleName());
    TestLogAppender appender = new TestLogAppender(caller);
    Logger logger = (Logger) LogManager.getLogger(name);
    appender.start();
    logger.addAppender(appender);
    logger.setAdditive(false);
    logger.setLevel(level);
    try {
      logic.run();
    } finally {
      logger.removeAppender(appender);
      appender.stop();
    }
    return appender;
  }
}
