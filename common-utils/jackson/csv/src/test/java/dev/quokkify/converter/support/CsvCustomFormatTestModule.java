package dev.quokkify.converter.support;

import java.io.IOException;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CsvCustomFormatTestModule extends SimpleModule {

  public CsvCustomFormatTestModule() {
    addSerializer(PetType.class, new PetTypeSerializer());
    addDeserializer(PetType.class, new PetTypeDeserializer());
  }

  private static class PetTypeSerializer extends StdSerializer<PetType> {

    protected PetTypeSerializer() {
      super(PetType.class);
    }

    @Override
    public void serialize(PetType value, JsonGenerator gen, SerializerProvider provider) throws IOException {
      gen.writeString(value.name().toLowerCase(Locale.ROOT));
    }
  }

  private static class PetTypeDeserializer extends StdDeserializer<PetType> {

    protected PetTypeDeserializer() {
      super(PetType.class);
    }

    @Override
    public PetType deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
      String value = p.getValueAsString();
      return PetType.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }
  }

  public enum PetType {
    CAT, DOG
  }

  public record PetRow(String name, PetType type) {
  }
}
