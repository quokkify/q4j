package dev.quokkify.test;

import java.util.List;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.parser.XmlParser;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class XmlParserTest {

  @TmsLink("XML_PARSER_ID_1")
  @TestGroup("XML")
  @Test(description = "Verify parse xml file to object")
  public void testParseXmlFileToObject() {
    List<User> expectedUsers = List.of(
        new User("John", 18),
        new User("Jack", 20)
    );

    /* @Step 1: Load data from xml file; Expected: Loaded data parse to object */
    UsersEntity usersEntity = XmlParser.parse("test_file.xml", UsersEntity.class);
    /* @Step 2: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(usersEntity.users()).as("Loaded objects are incorrect")
        .hasSameElementsAs(expectedUsers);
  }

  /**
   * XML users representation using record with Jackson.
   */
  @JacksonXmlRootElement(localName = "users")
  public record UsersEntity(
      @JacksonXmlElementWrapper(useWrapping = false)
      @JacksonXmlProperty(localName = "user")
      List<User> users
  ) {

  }

  /**
   * XML user representation using record with Jackson.
   */
  @JacksonXmlRootElement(localName = "user")
  public record User(
      @JacksonXmlProperty(isAttribute = true, localName = "name") String name,
      @JacksonXmlProperty(localName = "age") int age
  ) {

  }
}
