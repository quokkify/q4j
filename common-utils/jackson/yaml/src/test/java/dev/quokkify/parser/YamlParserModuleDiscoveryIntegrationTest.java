package dev.quokkify.parser;

import java.time.LocalDateTime;
import java.util.Map;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.parser.support.PolymorphicYamlTypeTestModule.Cat;
import dev.quokkify.parser.support.PolymorphicYamlTypeTestModule.Pet;
import dev.quokkify.util.JsonConverter;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class YamlParserModuleDiscoveryIntegrationTest {

  @TmsLink("YAML_PARSER_MODULE_DISCOVERY_ID_1")
  @TestGroup("Yaml")
  @Test(description = "Verify YamlParser + JsonConverter path applies discovered polymorphic module")
  public void shouldConvertYamlToPolymorphicTypeUsingDiscoveredModule() {
    Map<String, Object> payload = YamlParser.load("polymorphic_map.yaml");

    Pet pet = JsonConverter.fromObject(payload.get("pet"), Pet.class);

    Assertions.assertThat(pet).isInstanceOf(Cat.class);
    Assertions.assertThat(((Cat) pet).name()).isEqualTo("Tom");
    Assertions.assertThat(((Cat) pet).lives()).isEqualTo(9);
  }

  @TmsLink("YAML_PARSER_MODULE_DISCOVERY_ID_2")
  @TestGroup("Yaml")
  @Test(description = "Verify YamlParser + JsonConverter path supports Java Time via discovered modules")
  public void shouldConvertYamlToJavaTimeTypeUsingDiscoveredModule() {
    Map<String, Object> payload = YamlParser.load("date_container.yaml");

    DateContainer container = JsonConverter.fromObject(payload, DateContainer.class);

    Assertions.assertThat(container.value()).isEqualTo(LocalDateTime.of(2024, 1, 2, 3, 4, 5));
  }

  record DateContainer(LocalDateTime value) {
  }
}
