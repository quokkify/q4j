package dev.quokkify.entity.nosql;

import org.bson.types.ObjectId;

/**
 * This interface should be implements for all Mongo entities.
 *
 * @deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public interface MongoEntityInterface {

  ObjectId getId();

  String toString();
}
