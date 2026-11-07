package dev.quokkify.entity.nosql;

import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

/**
 * @deprecated Use {@code dev.quokkify:mongodb} ({@code dev.quokkify.mongodb.*}); this module will be removed.
 */
@Deprecated(since = "0.14.0", forRemoval = true)
public class Identity implements MongoEntityInterface {

  @Id
  private ObjectId id;

  @Override
  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }
}
