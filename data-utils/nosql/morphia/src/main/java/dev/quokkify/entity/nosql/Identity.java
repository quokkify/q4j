package dev.quokkify.entity.nosql;

import dev.morphia.annotations.Id;
import org.bson.types.ObjectId;

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
