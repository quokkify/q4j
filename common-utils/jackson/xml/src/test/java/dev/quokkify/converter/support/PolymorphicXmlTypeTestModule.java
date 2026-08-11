package dev.quokkify.converter.support;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class PolymorphicXmlTypeTestModule extends SimpleModule {

  public PolymorphicXmlTypeTestModule() {
    setMixInAnnotation(Pet.class, PetTypeMixIn.class);
  }

  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
  @JsonSubTypes({
      @JsonSubTypes.Type(value = Cat.class, name = "cat")
  })
  public interface PetTypeMixIn {
  }

  public interface Pet {
  }

  public record Cat(String name, int lives) implements Pet {
  }
}
