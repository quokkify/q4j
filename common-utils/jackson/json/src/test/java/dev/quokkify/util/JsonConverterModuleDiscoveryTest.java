package dev.quokkify.util;

import java.time.LocalDateTime;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.util.support.PolymorphicJsonTypeTestModule.Cat;
import dev.quokkify.util.support.PolymorphicJsonTypeTestModule.Pet;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class JsonConverterModuleDiscoveryTest {

  @TmsLink("JSON_CONVERTER_MODULE_DISCOVERY_ID_1")
  @TestGroup("Json")
  @Test(description = "Verify serialization does not contain type discriminator when module discovery is disabled")
  public void shouldSerializeWithoutTypeFieldWhenCustomModuleNotRegistered() throws Exception {
    ObjectMapper mapper = JsonConverter.createJsonMapper(false, false);
    String json = mapper.writeValueAsString(new Cat("Tom", 9));
    Assertions.assertThat(json).doesNotContain("\"type\"");
  }

  @TmsLink("JSON_CONVERTER_MODULE_DISCOVERY_ID_2")
  @TestGroup("Json")
  @Test(description = "Verify serialization contains type discriminator when module discovery is enabled")
  public void shouldSerializeWithTypeFieldWhenCustomModuleRegistered() {
    String json = JsonConverter.toJson(new Cat("Tom", 9));
    Assertions.assertThat(json).contains("\"type\":\"cat\"");
  }

  @TmsLink("JSON_CONVERTER_MODULE_DISCOVERY_ID_3")
  @TestGroup("Json")
  @Test(description = "Verify deserialization to base type fails without discovered custom module")
  public void shouldFailDeserializePolymorphicPayloadWhenCustomModuleNotRegistered() {
    ObjectMapper mapper = JsonConverter.createJsonMapper(false, false);
    String json = "{\"type\":\"cat\",\"name\":\"Tom\",\"lives\":9}";

    Assertions.assertThatThrownBy(() -> mapper.readValue(json, Pet.class))
        .hasMessageContaining("Cannot construct instance");
  }

  @TmsLink("JSON_CONVERTER_MODULE_DISCOVERY_ID_4")
  @TestGroup("Json")
  @Test(description = "Verify deserialization to base type works with discovered custom module")
  public void shouldDeserializePolymorphicPayloadWhenCustomModuleRegistered() {
    String json = "{\"type\":\"cat\",\"name\":\"Tom\",\"lives\":9}";

    Pet pet = JsonConverter.fromString(json, Pet.class);

    Assertions.assertThat(pet).isInstanceOf(Cat.class);
    Assertions.assertThat(((Cat) pet).name()).isEqualTo("Tom");
    Assertions.assertThat(((Cat) pet).lives()).isEqualTo(9);
  }

  @TmsLink("JSON_CONVERTER_MODULE_DISCOVERY_ID_5")
  @TestGroup("Json")
  @Test(description = "Verify default discovered modules support Java Time serialization and deserialization")
  public void shouldUseDefaultDiscoveredJavaTimeModule() {
    DateContainer expected = new DateContainer(LocalDateTime.of(2024, 1, 2, 3, 4, 5));

    String json = JsonConverter.toJson(expected);
    DateContainer actual = JsonConverter.fromString(json, DateContainer.class);

    Assertions.assertThat(json).contains("\"value\":\"2024-01-02T03:04:05\"");
    Assertions.assertThat(actual.value()).isEqualTo(expected.value());
  }

  record DateContainer(LocalDateTime value) {
  }
}
