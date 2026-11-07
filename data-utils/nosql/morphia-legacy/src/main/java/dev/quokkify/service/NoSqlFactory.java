package dev.quokkify.service;

import java.util.Objects;

import com.mongodb.client.MongoClient;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import dev.morphia.config.MorphiaConfig;

/**
 * Use this class for get methods for work with mongo database.
 *
 * @deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public class NoSqlFactory {

  private final MongoClient mongoClient;
  private final String dbName;
  private final MorphiaConfig morphiaConfig;
  private final ThreadLocal<Datastore> datastoreThreadLocal = new ThreadLocal<>();

  public NoSqlFactory(MongoClient mongoClient, String dbName) {
    this.mongoClient = Objects.requireNonNull(mongoClient, "mongoClient");
    this.dbName = Objects.requireNonNull(dbName, "dbName");
    this.morphiaConfig = null;
  }

  public NoSqlFactory(MongoClient mongoClient, MorphiaConfig morphiaConfig) {
    this.mongoClient = Objects.requireNonNull(mongoClient, "mongoClient");
    this.dbName = null;
    this.morphiaConfig = Objects.requireNonNull(morphiaConfig, "morphiaConfig");
  }

  /**
   * Return thread safe {@link dev.morphia.Datastore} for work with mongo.
   *
   * @return {@link dev.morphia.Datastore}
   */
  public Datastore getThreadLocalDatastore() {
    if (Objects.isNull(datastoreThreadLocal.get())) {
      datastoreThreadLocal.set(Objects.isNull(morphiaConfig)
          ? Morphia.createDatastore(mongoClient, dbName)
          : Morphia.createDatastore(mongoClient, morphiaConfig));
    }
    return datastoreThreadLocal.get();
  }
}
