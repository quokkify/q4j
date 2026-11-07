package dev.quokkify.mongodb.entity;

import java.util.UUID;

import dev.quokkify.mongodb.annotation.MongoDocument;

import com.querydsl.core.annotations.QueryEntity;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.codecs.pojo.annotations.BsonProperty;
import org.bson.types.ObjectId;

@MongoDocument("test_users")
@QueryEntity
public class TestUser {

  @BsonId
  private ObjectId id;

  @BsonProperty("first_name")
  private String firstName;

  private String lastName;
  private int age;
  private Role role;
  private Address address;
  private UUID externalId;

  public TestUser() {
  }

  public ObjectId getId() {
    return id;
  }

  public void setId(ObjectId id) {
    this.id = id;
  }

  public String getFirstName() {
    return firstName;
  }

  public void setFirstName(String firstName) {
    this.firstName = firstName;
  }

  public String getLastName() {
    return lastName;
  }

  public void setLastName(String lastName) {
    this.lastName = lastName;
  }

  public int getAge() {
    return age;
  }

  public void setAge(int age) {
    this.age = age;
  }

  public Role getRole() {
    return role;
  }

  public void setRole(Role role) {
    this.role = role;
  }

  public UUID getExternalId() {
    return externalId;
  }

  public void setExternalId(UUID externalId) {
    this.externalId = externalId;
  }

  public Address getAddress() {
    return address;
  }

  public void setAddress(Address address) {
    this.address = address;
  }
}
