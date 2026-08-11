package dev.quokkify.steps;

import java.util.List;
import java.util.function.Supplier;

import dev.quokkify.entity.SqlEntityInterface;
import dev.quokkify.service.SqlFactory;

import com.querydsl.core.types.EntityPath;
import com.querydsl.jpa.impl.JPAQuery;
import io.qameta.allure.Step;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class for work with sql database, this class presents base CRUD operations for you.
 */
public record SqlDatabaseSteps(SqlFactory sqlFactory) {

  private static final Logger LOG = LoggerFactory.getLogger(SqlDatabaseSteps.class);

  /**
   * Save entity to database. Use this method for save one entity.
   *
   * @param entity entity for save, extends from {@link SqlEntityInterface}
   * @param <T>    extends from {@link SqlEntityInterface}
   */
  @Step("Save entity to sql database")
  public <T extends SqlEntityInterface> void save(T entity) {
    inTransaction(sqlFactory.getSession(), () -> {
      sqlFactory.getSession().persist(entity);
      return true;
    });
  }

  /**
   * Save entities list to database. Use this method for save more than one entity.
   *
   * @param entities list of entities for save.
   * @param <T>      extents from {@link SqlEntityInterface}
   */
  @Step("Save entities to sql database")
  public <T extends SqlEntityInterface> void save(List<T> entities) {
    inTransaction(sqlFactory.getSession(), () -> {
      entities.forEach(sqlFactory.getSession()::persist);
      return true;
    });
  }

  /**
   * Update entity in database. Use this method for change data in database.
   *
   * @param entity entity with data for update, extends from {@link SqlEntityInterface}
   * @param <T>    extents from {@link SqlEntityInterface}
   */
  @Step("Update entity in sql database")
  public <T extends SqlEntityInterface> void update(T entity) {
    inTransaction(sqlFactory.getSession(), () -> {
      sqlFactory.getSession().merge(entity);
      return true;
    });
  }

  /**
   * Delete entity form database.
   *
   * @param entity entity for delete, extends from {@link SqlEntityInterface}
   * @param <T>    extents from {@link SqlEntityInterface}
   */
  @Step("Delete entity in sql database")
  public <T extends SqlEntityInterface> void delete(T entity) {
    inTransaction(sqlFactory.getSession(), () -> {
      sqlFactory.getSession().remove(entity);
      return true;
    });
  }

  /**
   * Create query for select.
   *
   * @param from path to query {@link EntityPath}
   * @param <T>  extents from {@link SqlEntityInterface}
   * @return query for execute {@link JPAQuery}
   */
  @Step("Create query for request in sql database")
  public <T extends SqlEntityInterface> JPAQuery<T> selectDsl(EntityPath<T> from) {
    sqlFactory.getSession().clear();
    return sqlFactory.getThreadLocalQueryFactory().selectFrom(from);
  }

  /**
   * Execute database query in transaction.
   *
   * @param session  session for query
   * @param supplier query for execute (CRUD)
   * @param <T>      extents from {@link SqlEntityInterface}
   */
  private <T> void inTransaction(Session session, Supplier<T> supplier) {
    session.getTransaction().begin();
    try {
      supplier.get();
      session.getTransaction().commit();
    } catch (Exception ex) {
      session.getTransaction().rollback();
      LOG.error("Transaction failed", ex);
      throw new RuntimeException("Transaction was not committed: %s".formatted(ex.getMessage()));
    }
  }

  /**
   * Close session which database work.
   */
  @Step("Close sql database connection")
  public void closeConnection() {
    sqlFactory.getSession().close();
  }
}
