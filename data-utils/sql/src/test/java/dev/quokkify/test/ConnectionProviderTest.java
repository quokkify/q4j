package dev.quokkify.test;

import java.util.List;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.persistence.PersistenceItem;
import dev.quokkify.provider.PersistenceItemProvider;
import dev.quokkify.service.DatabaseService;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class ConnectionProviderTest extends BaseDatabaseTest {

  private static final List<String> sequences = List.of(
      "Configuring connection pool [c3p0]",
      "Pool: C3P0ConnectionProvider"
  );

  @TmsLink("SQL_DATABASE_CONNECTION_PROVIDER_ID_1")
  @Test(description = "Check connection via Xml")
  @SingleThread
  public void checkC3P0ProviderViaXml() {
    TestLogAppender appender = withLogging(() -> DatabaseService.getInstance().createSqlQuery("hsql"));
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain both lines")
        .contains(sequences);
  }

  @TmsLink("SQL_DATABASE_CONNECTION_PROVIDER_ID_2")
  @Test(description = "Check connection via Provider")
  @SingleThread
  public void checkC3P0ProviderViaPersistenceProvider() {
    PersistenceItem persistenceItem = PersistenceItemProvider.getPersistenceItem(getStage());
    TestLogAppender appender = withLogging(() -> DatabaseService.getInstance().createSqlQuery(persistenceItem));
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain both lines")
        .contains(sequences);
  }

  @TmsLink("SQL_DATABASE_CONNECTION_PROVIDER_ID_3")
  @Test(description = "Check connection via PersistenceItem")
  @SingleThread
  public void checkC3P0ProviderViaPersistenceItem() {
    PersistenceItem persistenceItem = new PersistenceItem("test", getH2Properties());
    TestLogAppender appender = withLogging(() -> DatabaseService.getInstance().createSqlQuery(persistenceItem));
    Assertions.assertThat(appender.getCombinedLog())
        .as("Combined log should contain both lines")
        .contains(sequences);
  }

  private static TestLogAppender withLogging(Runnable testLogic) {
    return withLogging("org.hibernate.orm.connections.pooling", testLogic);
  }
}
