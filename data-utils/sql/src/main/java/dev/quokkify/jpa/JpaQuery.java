package dev.quokkify.jpa;

import java.util.List;

import dev.quokkify.converter.SqlConverter;

import com.querydsl.core.NonUniqueResultException;
import com.querydsl.jpa.impl.JPAQuery;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import jakarta.persistence.EntityManager;

/**
 * Query builder for execute SQL.
 * For example:
 * <pre>
 * {@code
 * databaseSteps.selectDsl(databaseTestUser).where(databaseTestUser.lastName.eq(newName)).fetch();
 * }
 * </pre>
 *
 * @param <T> type of object
 */
public class JpaQuery<T> extends JPAQuery<T> {

  public JpaQuery(EntityManager entityManager) {
    super(entityManager);
  }

  @Override
  public String toString() {
    return SqlConverter.convertToString(createQuery());
  }

  /**
   * Execute sql query and return result as list objects.
   *
   * @return list of objects
   */
  @Override
  @Step("Execute query")
  public List<T> fetch() {
    attachSqlRequest();
    return super.fetch();
  }

  /**
   * Execute sql query and return result as one object.
   *
   * @return object
   * @throws NonUniqueResultException exception
   */
  @Override
  @Step("Execute query")
  public T fetchOne() throws NonUniqueResultException {
    attachSqlRequest();
    return super.fetchOne();
  }

  /**
   * Attach sql request to allure report.
   *
   * @return sql request
   */
  @Attachment("SQL query")
  private String attachSqlRequest() {
    return toString();
  }
}
