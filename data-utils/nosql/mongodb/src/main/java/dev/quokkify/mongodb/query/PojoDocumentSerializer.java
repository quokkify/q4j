package dev.quokkify.mongodb.query;

import java.lang.reflect.AnnotatedElement;

import com.mongodb.DBRef;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.PathMetadata;
import com.querydsl.mongodb.document.MongodbDocumentSerializer;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;

/**
 * Serializer that maps Querydsl paths to the keys the POJO codec stores, honoring {@link BsonId} and {@link BsonProperty}.
 */
public class PojoDocumentSerializer extends MongodbDocumentSerializer {

  @Override
  protected String getKeyForPath(Path<?> expr, PathMetadata metadata) {
    AnnotatedElement element = expr.getAnnotatedElement();
    if (element != null) {
      if (element.isAnnotationPresent(BsonId.class)) {
        return "_id";
      }
      BsonProperty property = element.getAnnotation(BsonProperty.class);
      if (property != null && !property.value().isEmpty()) {
        return property.value();
      }
    }
    return super.getKeyForPath(expr, metadata);
  }

  @Override
  protected boolean isReference(Path<?> arg) {
    return false;
  }

  @Override
  protected DBRef asReference(Object constant) {
    throw new UnsupportedOperationException("DBRef references are not supported");
  }
}
