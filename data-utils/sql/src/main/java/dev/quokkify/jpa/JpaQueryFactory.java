package dev.quokkify.jpa;

import java.util.function.Supplier;

import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;

/**
 * Factory for create SQL query.
 */
public class JpaQueryFactory extends JPAQueryFactory {

  private final Supplier<EntityManager> entityManager;

  public JpaQueryFactory(final EntityManager entityManager) {
    super(entityManager);
    this.entityManager = () -> entityManager;
  }

  /**
   * Returned query builder with entity.
   *
   * @return {@link JPAQuery}
   */
  @Override
  public JPAQuery<?> query() {
    return new JpaQuery<>(entityManager.get());
  }
}
