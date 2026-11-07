package dev.quokkify.mongodb.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a MongoDB document and names the collection it is stored in.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface MongoDocument {

  /**
   * Collection name.
   *
   * @return collection name
   */
  String value();
}
