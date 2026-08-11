package dev.quokkify.attribute;

import java.util.Objects;

import jakarta.persistence.AttributeConverter;

public abstract class AbstractEnumConverter<T extends Enum<T> & DatabaseEnum> implements AttributeConverter<T, String> {

  private final Class<T> clazz;

  protected AbstractEnumConverter(Class<T> clazz) {
    this.clazz = clazz;
  }

  @Override
  public String convertToDatabaseColumn(T enumAttribute) {
    return enumAttribute == null ? null : enumAttribute.getDatabaseValue();
  }

  @Override
  public T convertToEntityAttribute(String databaseValue) {
    if (databaseValue == null) return null;
    for (T constant : clazz.getEnumConstants()) {
      if (Objects.equals(constant.getDatabaseValue(), databaseValue)) {
        return constant;
      }
    }
    throw new IllegalArgumentException("Unknown database value '" + databaseValue + "' for enum " + clazz.getName());
  }
}
