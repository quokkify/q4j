package dev.quokkify.mongodb.service;

import java.util.Objects;

import dev.quokkify.mongodb.annotation.MongoDocument;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * Resolves typed collections of a MongoDB database from {@link MongoDocument} annotated classes.
 *
 * <p>{@link MongoDatabase} and {@link MongoCollection} are thread safe, so the factory holds no per-thread state and can
 * be shared between tests.
 */
public final class MongoFactory {

  private final MongoDatabase database;
  private final CodecRegistry codecRegistry;

  /**
   * Create a factory with the default POJO codec registry.
   *
   * @param client MongoDB client
   * @param databaseName database name
   */
  public MongoFactory(MongoClient client, String databaseName) {
    this(client, databaseName, defaultCodecRegistry());
  }

  /**
   * Create a factory with a custom codec registry.
   *
   * @param client MongoDB client
   * @param databaseName database name
   * @param codecRegistry registry used to encode and decode documents
   */
  public MongoFactory(MongoClient client, String databaseName, CodecRegistry codecRegistry) {
    Objects.requireNonNull(client, "client");
    Objects.requireNonNull(databaseName, "databaseName");
    Objects.requireNonNull(codecRegistry, "codecRegistry");
    this.database = client.getDatabase(databaseName).withCodecRegistry(codecRegistry);
    this.codecRegistry = database.getCodecRegistry();
  }

  /**
   * Return the collection of the given document class.
   *
   * @param type class annotated with {@link MongoDocument}
   * @param <T> document type
   * @return POJO-typed collection
   * @throws IllegalArgumentException if the class is not annotated with {@link MongoDocument}
   */
  public <T> MongoCollection<T> collection(Class<T> type) {
    return database.getCollection(collectionName(type), type);
  }

  /**
   * Return the same collection as {@link #collection(Class)} typed as {@link Document}.
   *
   * @param type class annotated with {@link MongoDocument}
   * @return document-typed collection
   * @throws IllegalArgumentException if the class is not annotated with {@link MongoDocument}
   */
  public MongoCollection<Document> documents(Class<?> type) {
    return database.getCollection(collectionName(type));
  }

  /**
   * Return the codec registry the database actually uses, including the client's UUID representation.
   *
   * @return effective codec registry
   */
  public CodecRegistry codecRegistry() {
    return codecRegistry;
  }

  private static String collectionName(Class<?> type) {
    Objects.requireNonNull(type, "type");
    MongoDocument document = type.getAnnotation(MongoDocument.class);
    if (document == null) {
      throw new IllegalArgumentException(type.getName() + " is not annotated with @MongoDocument");
    }
    return document.value();
  }

  private static CodecRegistry defaultCodecRegistry() {
    return CodecRegistries.fromRegistries(
        MongoClientSettings.getDefaultCodecRegistry(),
        CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build()));
  }
}
