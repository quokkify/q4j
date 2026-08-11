package dev.quokkify.service;

import java.util.Objects;

import dev.quokkify.jpa.JpaQueryFactory;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;

/**
 * Use this class for get methods for work with sql database.
 */

public class SqlFactory {

  private final EntityManagerFactory entityManagerFactory;
  private final ThreadLocal<EntityManager> entityManagerThreadLocal = new ThreadLocal<>();
  private final ThreadLocal<JpaQueryFactory> queryFactoryThreadLocal = new ThreadLocal<>();

  public SqlFactory(EntityManagerFactory entityManagerFactory) {
    this.entityManagerFactory = entityManagerFactory;
  }

  /**
   * Return thread safe entity manager for use persistence.
   *
   * @return {@link EntityManager}
   */
  public EntityManager getThreadLocalEntityManager() {
    if (Objects.isNull(entityManagerThreadLocal.get())) {
      entityManagerThreadLocal.set(entityManagerFactory.createEntityManager());
    }
    return entityManagerThreadLocal.get();
  }

  /**
   * Thread safe query factory for execute sql queries.
   *
   * @return {@link JpaQueryFactory}
   */
  public JpaQueryFactory getThreadLocalQueryFactory() {
    if (Objects.isNull(queryFactoryThreadLocal.get())) {
      queryFactoryThreadLocal.set(new JpaQueryFactory(getThreadLocalEntityManager()));
    }
    return queryFactoryThreadLocal.get();
  }

  /**
   * Session for work with database from thread safe entity manager.
   *
   * @return {@link Session}
   */
  public Session getSession() {
    return getThreadLocalEntityManager().unwrap(Session.class);
  }
}
