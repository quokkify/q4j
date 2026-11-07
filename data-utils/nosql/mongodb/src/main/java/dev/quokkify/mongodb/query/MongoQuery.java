package dev.quokkify.mongodb.query;

import java.util.List;
import java.util.function.Function;

import dev.quokkify.mongodb.service.MongoFactory;

import com.mongodb.client.MongoCollection;
import com.querydsl.core.NonUniqueResultException;
import com.querydsl.mongodb.document.AbstractFetchableMongodbQuery;
import io.qameta.allure.Attachment;
import io.qameta.allure.Step;
import org.bson.BsonDocumentReader;
import org.bson.Document;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.configuration.CodecRegistry;

/**
 * Type-safe query over a MongoDB collection built with generated Querydsl Q-classes.
 * For example:
 * <pre>
 * {@code
 * new MongoQuery<>(factory, TestUser.class).where(QTestUser.testUser.lastName.eq("Lee")).fetch();
 * }
 * </pre>
 *
 * @param <T> document type
 */
public class MongoQuery<T> extends AbstractFetchableMongodbQuery<T, MongoQuery<T>> {

  private final MongoFactory factory;

  /**
   * Create a query over the collection of the given document class.
   *
   * @param factory collection factory
   * @param type class annotated with {@code @MongoDocument}
   */
  public MongoQuery(MongoFactory factory, Class<T> type) {
    super(factory.documents(type), transformer(factory.codecRegistry(), type), new PojoDocumentSerializer());
    this.factory = factory;
  }

  @Override
  protected MongoCollection<Document> getCollection(Class<?> type) {
    return factory.documents(type);
  }

  /**
   * Execute the query and return all matching documents.
   *
   * @return list of documents
   */
  @Override
  @Step("Execute Mongo query")
  public List<T> fetch() {
    attachQuery();
    return super.fetch();
  }

  /**
   * Execute the query and return the first matching document.
   *
   * @return document or {@code null}
   */
  @Override
  @Step("Execute Mongo query")
  public T fetchFirst() {
    attachQuery();
    return super.fetchFirst();
  }

  /**
   * Execute the query and return the only matching document.
   *
   * @return document or {@code null}
   * @throws NonUniqueResultException if several documents match
   */
  @Override
  @Step("Execute Mongo query")
  public T fetchOne() throws NonUniqueResultException {
    attachQuery();
    return super.fetchOne();
  }

  @Attachment(value = "Mongo query", type = "application/json")
  private String attachQuery() {
    Document filter = asDocument();
    try {
      return filter.toJson(factory.codecRegistry().get(Document.class));
    } catch (RuntimeException e) {
      return filter.toString();
    }
  }

  private static <T> Function<Document, T> transformer(CodecRegistry registry, Class<T> type) {
    Codec<T> codec = registry.get(type);
    return document -> codec.decode(new BsonDocumentReader(document.toBsonDocument(Document.class, registry)),
        DecoderContext.builder().build());
  }
}
