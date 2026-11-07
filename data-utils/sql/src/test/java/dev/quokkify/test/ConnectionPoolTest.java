package dev.quokkify.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import dev.quokkify.annotation.SingleThread;
import dev.quokkify.entity.DatabaseTestUser;
import dev.quokkify.entity.QDatabaseTestUser;
import dev.quokkify.persistence.PersistenceConfigurationFactory;
import dev.quokkify.service.DatabaseService;
import dev.quokkify.service.SqlFactory;
import dev.quokkify.steps.SqlDatabaseSteps;
import dev.quokkify.util.Waiter;

import jakarta.persistence.EntityManagerFactory;
import org.assertj.core.api.Assertions;
import org.hibernate.cfg.AvailableSettings;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ConnectionPoolTest extends BaseDatabaseTest {

  private static final String JDBC_URL =
      System.getenv().getOrDefault("POSTGRES_JDBC_URL", "jdbc:postgresql://localhost:5432/q4j");
  private static final String JDBC_USER = System.getenv().getOrDefault("POSTGRES_USER", "q4j");
  private static final String JDBC_PASSWORD = System.getenv().getOrDefault("POSTGRES_PASSWORD", "q4j");
  private static final String JDBC_DRIVER = "org.postgresql.Driver";
  private static final int OPERATIONS_PER_THREAD = 10;
  private static final long SAMPLING_INTERVAL_MS = 200;
  private static final long IDLE_PAUSE_MS = 8_000;
  private static final long JOIN_TIMEOUT_MS = 60_000;
  private static final int WORKER_THREADS = 10;

  private SqlFactory sqlFactory;
  private EntityManagerFactory entityManagerFactory;

  @BeforeClass(alwaysRun = true)
  public void checkPostgresReachable() {
    try (Connection ignored = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD)) {
      Assertions.assertThat(ignored).isNotNull();
    } catch (SQLException e) {
      throw new AssertionError("PostgreSQL is not reachable at " + JDBC_URL
          + ". Set POSTGRES_JDBC_URL or run: docker compose -f tools/environment/docker/docker-compose.yml"
          + " -f tools/environment/docker/docker-compose.local.yml --profile sql up -d", e);
    }
  }

  @AfterMethod(alwaysRun = true)
  public void closeFactory() {
    if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
      entityManagerFactory.close();
    }
    entityManagerFactory = null;
    sqlFactory = null;
  }

  @Test(description = "Check pool grows on demand only")
  @SingleThread
  public void checkPoolGrowsOnDemandOnly() throws InterruptedException, SQLException {
    String app = applicationName();
    sqlFactory = bootstrap(app, 100);
    assertLoadSucceeded(runLoad(app, WORKER_THREADS), WORKER_THREADS);
    Thread.sleep(IDLE_PAUSE_MS);
    Assertions.assertThat(connections(app))
        .as("idle connections after load from 10 threads")
        .isLessThanOrEqualTo(12);
  }

  @Test(description = "Check pool never exceeds max size")
  @SingleThread
  public void checkPoolNeverExceedsMaxSize() throws InterruptedException, SQLException {
    String app = applicationName();
    sqlFactory = bootstrap(app, 5);
    LoadResult result = runLoad(app, WORKER_THREADS);
    Assertions.assertThat(result.peak()).as("peak connections").isLessThanOrEqualTo(5);
    assertLoadSucceeded(result, WORKER_THREADS);
  }

  @Test(description = "Check pool releases connections on close")
  @SingleThread
  public void checkPoolReleasesConnectionsOnClose() throws InterruptedException, SQLException {
    String app = applicationName();
    sqlFactory = bootstrap(app, 5);
    assertLoadSucceeded(runLoad(app, 3), 3);
    entityManagerFactory.close();
    Waiter.awaitQuickCondition(() -> connections(app) == 0, "Connections were not released after close");
  }

  private static void assertLoadSucceeded(LoadResult result, int threads) {
    Assertions.assertThat(result.errors()).as("worker errors").isEmpty();
    Assertions.assertThat(result.finishedThreads()).as("finished threads").isEqualTo(threads);
  }

  private SqlFactory bootstrap(String applicationName, int poolSize) {
    Map<String, Object> props = Map.of(
        AvailableSettings.JAKARTA_JDBC_DRIVER, JDBC_DRIVER,
        AvailableSettings.JAKARTA_JDBC_URL, JDBC_URL + "?ApplicationName=" + applicationName + "&connectTimeout=5",
        AvailableSettings.JAKARTA_JDBC_USER, JDBC_USER,
        AvailableSettings.JAKARTA_JDBC_PASSWORD, JDBC_PASSWORD,
        AvailableSettings.POOL_SIZE, poolSize,
        AvailableSettings.HBM2DDL_AUTO, "update");
    SqlFactory factory = DatabaseService.getInstance().createSqlQuery(PersistenceConfigurationFactory.create(applicationName, props));
    entityManagerFactory = factory.getThreadLocalEntityManager().getEntityManagerFactory();
    return factory;
  }

  private static String applicationName() {
    return "q4j-pool-" + UUID.randomUUID();
  }

  private static int connections(String applicationName) throws SQLException {
    try (Connection connection = DriverManager.getConnection(JDBC_URL, JDBC_USER, JDBC_PASSWORD);
         PreparedStatement statement = connection.prepareStatement(
             "select count(*) from pg_stat_activity where application_name = ?")) {
      statement.setString(1, applicationName);
      try (ResultSet resultSet = statement.executeQuery()) {
        resultSet.next();
        return resultSet.getInt(1);
      }
    }
  }

  private LoadResult runLoad(String app, int threads) throws InterruptedException {
    SqlDatabaseSteps steps = new SqlDatabaseSteps(sqlFactory);
    QDatabaseTestUser user = QDatabaseTestUser.databaseTestUser;
    List<Throwable> errors = new CopyOnWriteArrayList<>();
    AtomicInteger peak = new AtomicInteger();
    AtomicInteger finished = new AtomicInteger();
    AtomicBoolean running = new AtomicBoolean(true);

    Thread sampler = new Thread(() -> {
      while (running.get()) {
        try {
          peak.accumulateAndGet(connections(app), Math::max);
          Thread.sleep(SAMPLING_INTERVAL_MS);
        } catch (SQLException e) {
          errors.add(e);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
    });
    sampler.start();

    List<Thread> workers = new ArrayList<>();
    for (int i = 0; i < threads; i++) {
      Thread worker = new Thread(() -> {
        try {
          for (int j = 0; j < OPERATIONS_PER_THREAD; j++) {
            steps.save(new DatabaseTestUser("pool", UUID.randomUUID().toString()));
            steps.selectDsl(user).where(user.firstName.isNotNull()).fetchFirst();
          }
          finished.incrementAndGet();
        } catch (Throwable e) {
          errors.add(e);
        }
      });
      workers.add(worker);
      worker.start();
    }
    for (Thread worker : workers) {
      worker.join(JOIN_TIMEOUT_MS);
    }
    running.set(false);
    sampler.join();
    return new LoadResult(peak.get(), errors, finished.get());
  }

  private record LoadResult(int peak, List<Throwable> errors, int finishedThreads) {
  }
}
