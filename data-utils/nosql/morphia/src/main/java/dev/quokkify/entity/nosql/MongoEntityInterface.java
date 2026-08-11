package dev.quokkify.entity.nosql;

import org.bson.types.ObjectId;

/**
 * This interface should be implements for all Mongo entities.
 */
public interface MongoEntityInterface {

  ObjectId getId();

  String toString();
}
