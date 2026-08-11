package dev.quokkify.test;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.parser.YamlParser;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class YamlParserTest {

  @TmsLink("PARSER_ID_1")
  @TestGroup("Yaml")
  @Test(description = "Verify parse yaml file to object")
  public void testParseToObject() {
    User expectedUser = new User("John", 18);

    /* @Step 1: Load data from yaml file; Expected: Loaded data parse to object */
    User actualUser = YamlParser.loadAsObjectFromResources("object.yaml", User.class);
    /* @Step 2: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(actualUser).as("Loaded object is incorrect")
        .extracting(User::name, User::age)
        .containsExactly(expectedUser.name(), expectedUser.age());
  }

  @TmsLink("PARSER_ID_2")
  @TestGroup("Yaml")
  @Test(description = "Verify parse yaml file to map")
  public void testParseToMap() {
    LinkedHashMap<String, User> expectedMap = new LinkedHashMap<>();
    expectedMap.put("user1", new User("Batman", 83));
    expectedMap.put("user2", new User("Superman", 84));
    expectedMap.put("user3", new User("Wonder Woman", 81));
    expectedMap.put("user4", new User("Aquaman", 78));
    expectedMap.put("user5", new User("Barry Allen", 35));

    /* @Step 1: Load data from yaml file; Expected: Loaded data parse to map */
    Map<String, User> actualMap = YamlParser.loadAsMapFromResources("map.yaml", User.class);
    /* @Step 2: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(actualMap).as("Loaded map is incorrect").containsExactlyEntriesOf(expectedMap);
  }

  @TmsLink("PARSER_ID_3")
  @TestGroup("Yaml")
  @Test(description = "Verify parse yaml file to map")
  public void testParseToMapFromFile() {
    LinkedHashMap<String, Object> expectedMap = new LinkedHashMap<>();
    expectedMap.put("user1", Map.of("name", "Batman", "age", 83));
    expectedMap.put("user2", Map.of("name", "Superman", "age", 84));
    expectedMap.put("user3", Map.of("name", "Wonder Woman", "age", 81));
    expectedMap.put("user4", Map.of("name", "Aquaman", "age", 78));
    expectedMap.put("user5", Map.of("name", "Barry Allen", "age", 35));

    /* @Step 1: Load data from yaml file; Expected: Loaded data parse to map */
    File file = new File(Thread.currentThread().getContextClassLoader().getResource("map.yaml").getFile());
    Map<String, Object> actualMap = YamlParser.load(file);
    /* @Step 2: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(actualMap).as("Loaded map is incorrect").containsExactlyEntriesOf(expectedMap);
  }

  @TmsLink("PARSER_ID_4")
  @TestGroup("Yaml")
  @Test(description = "Verify parse yaml file to list")
  public void testParseToList() {
    List<User> expectedList = new LinkedList<User>();
    expectedList.add(new User("Batman", 83));
    expectedList.add(new User("Superman", 84));
    expectedList.add(new User("Wonder Woman", 81));
    expectedList.add(new User("Aquaman", 78));
    expectedList.add(new User("Barry Allen", 35));

    /* @Step 1: Load data from yaml file; Expected: Loaded data parse to list */
    List<User> actualList = YamlParser.loadListFromResources("list.yaml", User.class);
    /* @Step 2: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(actualList).as("Loaded list is incorrect").isEqualTo(expectedList);
  }

  public record User(String name, int age) {

  }
}
