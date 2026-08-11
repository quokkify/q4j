package dev.quokkify.entity;

import jakarta.persistence.Entity;

@Entity
public class DatabaseTestUserIdentity extends Identity {

  private String firstName;
  private String lastName;

  public DatabaseTestUserIdentity() {
  }

  public DatabaseTestUserIdentity(String firstName, String lastName) {
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
