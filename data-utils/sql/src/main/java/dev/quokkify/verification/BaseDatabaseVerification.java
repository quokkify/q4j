package dev.quokkify.verification;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

import dev.quokkify.util.Waiter;

import io.qameta.allure.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BaseDatabaseVerification<T extends BaseDatabaseVerification<T>>
    implements DatabaseVerification {

  private static final Logger LOG = LogManager.getLogger(BaseDatabaseVerification.class);

  private Duration timeout;
  private Duration pollingInterval;

  protected BaseDatabaseVerification() {
    this(Duration.ofSeconds(60), Duration.ofMillis(5000));
  }

  protected BaseDatabaseVerification(Duration timeout, Duration pollingInterval) {
    this.timeout = timeout;
    this.pollingInterval = pollingInterval;
  }

  protected abstract T self();

  public T withTimeout(Duration timeout) {
    this.timeout = timeout;
    return self();
  }

  public T withPolling(Duration pollingInterval) {
    this.pollingInterval = pollingInterval;
    return self();
  }

  @Step("Database: has record matching predicate")
  public <E> T hasRecord(Callable<List<E>> query, Predicate<E> predicate) {
    LOG.debug("DB verify: has record matching predicate");
    Waiter.awaitCondition(
        () -> query.call().stream().anyMatch(predicate),
        "Expected database record matching predicate",
        timeout,
        pollingInterval);
    return self();
  }

  @Step("Database: does not have record matching predicate")
  public <E> T doesNotHaveRecord(Callable<List<E>> query, Predicate<E> predicate) {
    LOG.debug("DB verify: does not have record matching predicate");
    Waiter.assertNeverTrue(
        () -> {
          try {
            return query.call().stream().anyMatch(predicate);
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        timeout,
        pollingInterval,
        "Unexpected database record matching predicate");
    return self();
  }

  @Step("Database: record count is at least {count}")
  public <E> T hasRecordCount(Callable<List<E>> query, int count) {
    LOG.debug("DB verify: has at least {} records", count);
    Waiter.awaitCondition(
        () -> query.call().size() >= count,
        "Expected at least " + count + " database records",
        timeout,
        pollingInterval);
    return self();
  }
}
