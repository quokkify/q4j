package dev.quokkify.mongodb.entity;

import com.querydsl.core.annotations.QueryEmbeddable;
import org.bson.codecs.pojo.annotations.BsonProperty;

@QueryEmbeddable
public class Address {

  private String city;

  @BsonProperty("zip_code")
  private String zipCode;

  public Address() {
  }

  public String getCity() {
    return city;
  }

  public void setCity(String city) {
    this.city = city;
  }

  public String getZipCode() {
    return zipCode;
  }

  public void setZipCode(String zipCode) {
    this.zipCode = zipCode;
  }
}
