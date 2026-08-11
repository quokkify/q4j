package dev.quokkify.steps;

import java.util.List;

import dev.quokkify.entity.nosql.MongoEntityInterface;
import dev.quokkify.service.NoSqlFactory;
import dev.quokkify.verification.MongoVerifier;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import dev.morphia.DeleteOptions;
import dev.morphia.UpdateOptions;
import dev.morphia.query.Query;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperator;
import io.qameta.allure.Step;

public class MongoDatabaseSteps extends AbstractMongoSteps<MongoVerifier> {

  private final NoSqlFactory noSqlFactory;

  public MongoDatabaseSteps(NoSqlFactory noSqlFactory) {
    this.noSqlFactory = noSqlFactory;
  }

  @Override
  public MongoVerifier verify() {
    return new MongoVerifier();
  }

  public NoSqlFactory getNoSqlFactory() {
    return noSqlFactory;
  }

  @Step("Save entity to mongo database")
  public <T extends MongoEntityInterface> void save(T entity) {
    noSqlFactory.getThreadLocalDatastore().save(entity);
  }

  @Step("Save entities to mongo database")
  public <T extends MongoEntityInterface> void save(List<T> entities) {
    noSqlFactory.getThreadLocalDatastore().save(entities);
  }

  @Step("Update entity in mongo database")
  public <T extends MongoEntityInterface> UpdateResult update(T entity, List<UpdateOperator> operators) {
    return noSqlFactory.getThreadLocalDatastore().find(entity.getClass())
        .filter(Filters.eq("_id", entity.getId()))
        .update(new UpdateOptions().multi(false), operators.toArray(new UpdateOperator[0]));
  }

  @Step("Delete entity in mongo database")
  public <T extends MongoEntityInterface> DeleteResult delete(T entity) {
    return noSqlFactory.getThreadLocalDatastore().find(entity.getClass())
        .delete(new DeleteOptions().multi(true));
  }

  @Step("Create query for request in mongo database")
  public <T extends MongoEntityInterface> Query<T> selectDsl(Class<T> entity) {
    return noSqlFactory.getThreadLocalDatastore().find(entity);
  }
}
