package dev.quokkify.entity.nosql;

import dev.morphia.annotations.Entity;

@Entity
public class DatabaseTestUserMongo extends Identity {

  private String firstName;
  private String lastName;

  public DatabaseTestUserMongo() {
  }

  public DatabaseTestUserMongo(String firstName, String lastName) {
    this.firstName = firstName;
    this.lastName = lastName;
  }

  public String getFirstName() {
    return firstName;
  }

  public String getLastName() {
    return lastName;
  }
}
