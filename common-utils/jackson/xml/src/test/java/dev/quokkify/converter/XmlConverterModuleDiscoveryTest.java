package dev.quokkify.converter;

import java.time.LocalDateTime;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.converter.support.PolymorphicXmlTypeTestModule.Cat;
import dev.quokkify.converter.support.PolymorphicXmlTypeTestModule.Pet;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class XmlConverterModuleDiscoveryTest {

  @TmsLink("XML_CONVERTER_MODULE_DISCOVERY_ID_1")
  @TestGroup("XML")
  @Test(description = "Verify XML serialization does not contain type discriminator when module discovery is disabled")
  public void shouldSerializeWithoutTypeFieldWhenCustomModuleNotRegistered() throws Exception {
    XmlMapper mapper = XmlConverter.createXmlMapper(false, false);

    String xml = mapper.writeValueAsString(new Cat("Tom", 9));

    Assertions.assertThat(xml).doesNotContain("<type>");
  }

  @TmsLink("XML_CONVERTER_MODULE_DISCOVERY_ID_2")
  @TestGroup("XML")
  @Test(description = "Verify XML serialization contains type discriminator when module discovery is enabled")
  public void shouldSerializeWithTypeFieldWhenCustomModuleRegistered() {
    String xml = XmlConverter.toXml(new Cat("Tom", 9));

    Assertions.assertThat(xml).contains("type=\"cat\"");
  }

  @TmsLink("XML_CONVERTER_MODULE_DISCOVERY_ID_3")
  @TestGroup("XML")
  @Test(description = "Verify XML deserialization to base type fails without discovered custom module")
  public void shouldFailDeserializePolymorphicPayloadWhenCustomModuleNotRegistered() {
    XmlMapper mapper = XmlConverter.createXmlMapper(false, false);
    String xml = "<Pet><type>cat</type><name>Tom</name><lives>9</lives></Pet>";

    Assertions.assertThatThrownBy(() -> mapper.readValue(xml, Pet.class))
        .hasMessageContaining("Cannot construct instance");
  }

  @TmsLink("XML_CONVERTER_MODULE_DISCOVERY_ID_4")
  @TestGroup("XML")
  @Test(description = "Verify XML deserialization to base type works with discovered custom module")
  public void shouldDeserializePolymorphicPayloadWhenCustomModuleRegistered() {
    String xml = "<Pet><type>cat</type><name>Tom</name><lives>9</lives></Pet>";

    Pet pet = XmlConverter.fromString(xml, Pet.class);

    Assertions.assertThat(pet).isInstanceOf(Cat.class);
    Assertions.assertThat(((Cat) pet).name()).isEqualTo("Tom");
    Assertions.assertThat(((Cat) pet).lives()).isEqualTo(9);
  }

  @TmsLink("XML_CONVERTER_MODULE_DISCOVERY_ID_5")
  @TestGroup("XML")
  @Test(description = "Verify default discovered modules support Java Time serialization and deserialization for XML")
  public void shouldUseDefaultDiscoveredJavaTimeModule() {
    DateContainer expected = new DateContainer(LocalDateTime.of(2024, 1, 2, 3, 4, 5));

    String xml = XmlConverter.toXml(expected);
    DateContainer actual = XmlConverter.fromString(xml, DateContainer.class);

    Assertions.assertThat(xml).contains("<value>2024-01-02T03:04:05</value>");
    Assertions.assertThat(actual.value()).isEqualTo(expected.value());
  }

  record DateContainer(LocalDateTime value) {
  }
}
