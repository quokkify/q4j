package dev.quokkify.steps;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import dev.quokkify.entity.SqlEntityInterface;
import dev.quokkify.util.Waiter;

import com.querydsl.jpa.impl.JPAQuery;
import org.hamcrest.Matchers;

/**
 * Base abstract database steps for working with JPAQuery.
 */
public abstract class AbstractDatabaseSteps {

  private static final String DEFAULT_NPE_ERROR_MESSAGE = "Database query result is null!";

  /**
   * Update entity in database. Use this method for change data in database.
   *
   * @param entity entity with data for update, extends from {@link SqlEntityInterface}
   */
  protected <T extends SqlEntityInterface> void update(T entity) {
    getDatabaseSteps().update(entity);
  }

  /**
   * Save entity to database. Use this method for save one entity.
   *
   * @param entity entity for save, extends from {@link SqlEntityInterface}
   * @param <T>    extends from {@link SqlEntityInterface}
   */
  protected <T extends SqlEntityInterface> void save(T entity) {
    getDatabaseSteps().save(entity);
  }

  /**
   * Get database entity by JPAQuery. Method wait until entity exist in database and get result.
   *
   * @param function function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q waitUntilAppear(Function<SqlDatabaseSteps, JPAQuery<Q>> function) {
    return waitUntilAppear(function, Duration.ofSeconds(60), Duration.ofMillis(5000));
  }

  /**
   * Get database entity by JPAQuery. Method wait until entity exist in database and get result.
   * Used default error message.
   *
   * @param function        function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param timeout         waiter timeout
   * @param pollingInterval waiter polling interval
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q waitUntilAppear(Function<SqlDatabaseSteps, JPAQuery<Q>> function, Duration timeout,
                                  Duration pollingInterval) {
    return waitUntilAppear(function, DEFAULT_NPE_ERROR_MESSAGE, timeout, pollingInterval);
  }

  /**
   * Get database entity by JPAQuery. Method wait until entity exist in database and get result.
   *
   * @param function              function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param noResultsErrorMessage error message if entity not found
   * @param timeout               waiter timeout
   * @param pollingInterval       waiter polling interval
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q waitUntilAppear(Function<SqlDatabaseSteps, JPAQuery<Q>> function, String noResultsErrorMessage,
                                  Duration timeout, Duration pollingInterval) {
    return Waiter.awaitCondition(() -> function.apply(getDatabaseSteps()).fetchOne(), Matchers.notNullValue(),
        noResultsErrorMessage, timeout, pollingInterval);
  }

  /**
   * Get database entity by JPAQuery. Method wait until entity exist in database and get result.
   *
   * @param function                    function to get entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param noResultsErrorMessage       error message if entity not found
   * @param timeoutSeconds              waiter timeout {@link Integer}
   * @param pollingIntervalMilliseconds waiter polling interval {@link Integer}
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q waitUntilAppear(Function<SqlDatabaseSteps, JPAQuery<Q>> function, String noResultsErrorMessage,
                                  int timeoutSeconds, int pollingIntervalMilliseconds) {
    return Waiter.awaitCondition(() -> function.apply(getDatabaseSteps()).fetchOne(), Matchers.notNullValue(),
        noResultsErrorMessage, timeoutSeconds, pollingIntervalMilliseconds);
  }

  /**
   * Get the projection as a unique result or throw exception if no result is found.
   * Used default error message.
   *
   * @param function function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q fetchOne(Function<SqlDatabaseSteps, JPAQuery<Q>> function) {
    return fetchOne(function, DEFAULT_NPE_ERROR_MESSAGE);
  }

  /**
   * Get the projection as a unique result or throw exception if no result is found.
   *
   * @param function              function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param noResultsErrorMessage error message if entity not found
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q fetchOne(Function<SqlDatabaseSteps, JPAQuery<Q>> function, String noResultsErrorMessage) {
    return checkNonNullOrElseThrow(function.apply(getDatabaseSteps()).fetchOne(), noResultsErrorMessage);
  }

  /**
   * Get the first result of Get the projection or else get alternative value.
   *
   * @param function         function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param alternativeValue alternative value if not found any result.
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q fetchFirstOrElse(Function<SqlDatabaseSteps, JPAQuery<Q>> function, Q alternativeValue) {
    return checkNonNullOrElse(function.apply(getDatabaseSteps()).fetchFirst(), alternativeValue);
  }

  /**
   * Get the first result of Get the projection or throw exception if no result is found.
   * Used default error message.
   *
   * @param function function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q fetchFirst(Function<SqlDatabaseSteps, JPAQuery<Q>> function) {
    return fetchFirst(function, DEFAULT_NPE_ERROR_MESSAGE);
  }

  /**
   * Get the first result of Get the projection or or throw exception if no result is found.
   *
   * @param function              function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param noResultsErrorMessage error message if entity not found
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> Q fetchFirst(Function<SqlDatabaseSteps, JPAQuery<Q>> function, String noResultsErrorMessage) {
    return checkNonNullOrElseThrow(function.apply(getDatabaseSteps()).fetchFirst(), noResultsErrorMessage);
  }

  /**
   * Get the projection as a typed List or throw exception if no result is found.
   * Used default error message.
   *
   * @param function function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> List<Q> fetch(Function<SqlDatabaseSteps, JPAQuery<Q>> function) {
    return fetch(function, DEFAULT_NPE_ERROR_MESSAGE);
  }

  /**
   * Get the projection as a typed List or throw exception if no result is found.
   *
   * @param function              function to get database entity by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @param noResultsErrorMessage error message if entity not found
   * @return entity extends from {@link SqlEntityInterface}
   */
  protected <Q> List<Q> fetch(Function<SqlDatabaseSteps, JPAQuery<Q>> function, String noResultsErrorMessage) {
    return checkNonNullOrElseThrow(function.apply(getDatabaseSteps()).fetch(), noResultsErrorMessage);
  }

  /**
   * Find if projection exists or throw exception if no result is found.
   * Used default error message.
   *
   * @param function function to find if database entity exists by {@link JPAQuery} using {@link SqlDatabaseSteps}
   * @return boolean value
   */
  protected <Q> boolean fetchEmpty(Function<SqlDatabaseSteps, JPAQuery<Q>> function) {
    return checkIfEmpty(function.apply(getDatabaseSteps()).fetchFirst());
  }

  private <Q> boolean checkIfEmpty(Q object) {
    return Optional.ofNullable(object).isEmpty();
  }

  private <Q> Q checkNonNullOrElseThrow(Q object, String message) {
    return Optional.ofNullable(object).orElseThrow(() -> new RuntimeException(message));
  }

  private <Q> Q checkNonNullOrElse(Q object, Q alternativeValue) {
    return Optional.ofNullable(object).orElse(alternativeValue);
  }

  protected abstract SqlDatabaseSteps getDatabaseSteps();
}
