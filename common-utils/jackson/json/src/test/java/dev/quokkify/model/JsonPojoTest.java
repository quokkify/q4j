package dev.quokkify.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class JsonPojoTest {

  @Test(description = "Verify empty constructor creates empty json object")
  public void testEmptyConstructorCreatesEmptyJson() {
    /* @Step 1: Create empty custom pojo; Expected: Empty json object */
    JsonPojo customPojo = new JsonPojo();
    /* @Step 2: Validate serialized json; Expected: Empty json object string */
    Assertions.assertThat(customPojo.asJson()).as("Json is not empty").isEqualTo("{}");
  }

  @Test(description = "Verify constructor from json string")
  public void testConstructorFromJsonString() {
    /* @Step 1: Create custom pojo from json string; Expected: Json tree parsed */
    JsonPojo customPojo = new JsonPojo("{\"fact\":\"cats sleep a lot\",\"length\":16}");
    /* @Step 2: Validate parsed fields; Expected: Fields are accessible by json pointer */
    Assertions.assertThat(customPojo.requiredAt("/fact").asText())
        .as("Parsed field is incorrect")
        .isEqualTo("cats sleep a lot");
  }

  @Test(description = "Verify creation from typed pojo")
  public void testFromPojo() {
    /* @Step 1: Prepare typed pojo; Expected: Test data prepared */
    CatFactPojo catFactPojo = new CatFactPojo("cats meow", 9);
    /* @Step 2: Convert typed pojo to custom pojo; Expected: Json tree matches typed pojo */
    JsonPojo customPojo = JsonPojo.fromPojo(catFactPojo);
    /* @Step 3: Validate converted fields; Expected: Fields are accessible by json pointer */
    Assertions.assertThat(customPojo.requiredAt("/fact").asText())
        .as("Converted field is incorrect")
        .isEqualTo("cats meow");
  }

  @Test(description = "Verify set field with scalar value")
  public void testSetFieldWithScalar() {
    /* @Step 1: Set scalar fields; Expected: Fields set */
    JsonPojo customPojo = new JsonPojo()
        .setField("name", "John")
        .setField("age", 20);
    /* @Step 2: Validate serialized json; Expected: Scalar fields serialized */
    Assertions.assertThat(customPojo.asJson())
        .as("Json is incorrect")
        .isEqualTo("{\"name\":\"John\",\"age\":20}");
  }

  @Test(description = "Verify set field with typed pojo value")
  public void testSetFieldWithPojo() {
    /* @Step 1: Set field with typed pojo value; Expected: Field set as nested object */
    JsonPojo customPojo = new JsonPojo()
        .setField("cat", new CatFactPojo("cats jump", 9));
    /* @Step 2: Validate nested field; Expected: Nested field is accessible by json pointer */
    Assertions.assertThat(customPojo.requiredAt("/cat/fact").asText())
        .as("Nested field is incorrect")
        .isEqualTo("cats jump");
  }

  @Test(description = "Verify set field with map value")
  public void testSetFieldWithMap() {
    /* @Step 1: Set field with map value; Expected: Field set as nested object */
    JsonPojo customPojo = new JsonPojo()
        .setField("errors", Map.of("name", List.of("is required")));
    /* @Step 2: Validate nested field; Expected: Nested field is accessible by json pointer */
    Assertions.assertThat(customPojo.requiredAt("/errors/name/0").asText())
        .as("Nested field is incorrect")
        .isEqualTo("is required");
  }

  @Test(description = "Verify set field with null value")
  public void testSetFieldWithNull() {
    /* @Step 1: Set field with null value; Expected: Field set as json null */
    JsonPojo customPojo = new JsonPojo().setField("identifier", null);
    /* @Step 2: Validate field; Expected: Field is json null */
    Assertions.assertThat(customPojo.requiredAt("/identifier").isNull())
        .as("Field is not json null")
        .isTrue();
  }

  @Test(description = "Verify set value by json pointer creates missing intermediate objects")
  public void testSetAtCreatesNestedObjects() {
    /* @Step 1: Set value by deep json pointer on empty pojo; Expected: Intermediate objects created */
    JsonPojo customPojo = new JsonPojo().setAt("/bonus/settings/max_win", 100);
    /* @Step 2: Validate nested value; Expected: Value is accessible by json pointer */
    Assertions.assertThat(customPojo.requiredAt("/bonus/settings/max_win").asInt())
        .as("Nested value is incorrect")
        .isEqualTo(100);
  }

  @Test(description = "Verify set value by json pointer overwrites existing value")
  public void testSetAtOverwritesExistingValue() {
    /* @Step 1: Prepare pojo with existing nested value; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"bonus\":{\"amount\":10}}");
    /* @Step 2: Overwrite nested value by json pointer; Expected: Value overwritten */
    customPojo.setAt("/bonus/amount", 20);
    /* @Step 3: Validate nested value; Expected: New value returned */
    Assertions.assertThat(customPojo.requiredAt("/bonus/amount").asInt())
        .as("Overwritten value is incorrect")
        .isEqualTo(20);
  }

  @Test(description = "Verify set value by json pointer with array index")
  public void testSetAtReplacesArrayElement() {
    /* @Step 1: Prepare pojo with array field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"ids\":[1,2,3]}");
    /* @Step 2: Replace array element by json pointer; Expected: Element replaced */
    customPojo.setAt("/ids/1", 42);
    /* @Step 3: Validate array; Expected: Only target element replaced */
    Assertions.assertThat(customPojo.asJson())
        .as("Array is incorrect")
        .isEqualTo("{\"ids\":[1,42,3]}");
  }

  @Test(description = "Verify set value by json pointer creates array for index segment")
  public void testSetAtCreatesArrayForIndexSegment() {
    /* @Step 1: Set value by json pointer with index segment on empty pojo; Expected: Array created */
    JsonPojo customPojo = new JsonPojo().setAt("/items/0", "first");
    /* @Step 2: Validate created array; Expected: Array with single element */
    Assertions.assertThat(customPojo.asJson())
        .as("Created array is incorrect")
        .isEqualTo("{\"items\":[\"first\"]}");
  }

  @Test(description = "Verify set value by json pointer pads array with nulls beyond its end")
  public void testSetAtPadsArrayBeyondEnd() {
    /* @Step 1: Prepare pojo with array field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"items\":[\"first\"]}");
    /* @Step 2: Set value by json pointer beyond array end; Expected: Array padded with nulls */
    customPojo.setAt("/items/3", "last");
    /* @Step 3: Validate array; Expected: Missing elements are json nulls */
    Assertions.assertThat(customPojo.asJson())
        .as("Padded array is incorrect")
        .isEqualTo("{\"items\":[\"first\",null,null,\"last\"]}");
  }

  @Test(description = "Verify set value by root json pointer is rejected")
  public void testSetAtRootPointerThrows() {
    /* @Step 1: Set value by root json pointer; Expected: IllegalArgumentException */
    Assertions.assertThatThrownBy(() -> new JsonPojo().setAt("", "value"))
        .as("Root pointer is not rejected")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test(description = "Verify set value in each array element by relative json pointer")
  public void testSetEachAtReplacesFieldInAllElements() {
    /* @Step 1: Prepare pojo with array of objects; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo(
        "{\"inputs\":[{\"value\":{\"games\":1}},{\"value\":{\"games\":2}}]}");
    /* @Step 2: Set value in each array element; Expected: Value set in all elements */
    customPojo.setEachAt("/inputs", "/value/games", "invalid");
    /* @Step 3: Validate array elements; Expected: All elements updated */
    Assertions.assertThat(customPojo.asJson())
        .as("Array elements are incorrect")
        .isEqualTo("{\"inputs\":[{\"value\":{\"games\":\"invalid\"}},{\"value\":{\"games\":\"invalid\"}}]}");
  }

  @Test(description = "Verify set value in each array element creates missing nested path")
  public void testSetEachAtCreatesNestedPathInElements() {
    /* @Step 1: Prepare pojo with array of objects without nested path; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"inputs\":[{\"identifier\":\"a\"},{\"identifier\":\"b\"}]}");
    /* @Step 2: Set value by missing relative json pointer; Expected: Nested path created in all elements */
    customPojo.setEachAt("/inputs", "/value/currency", "EUR");
    /* @Step 3: Validate array elements; Expected: Nested path created in all elements */
    Assertions.assertThat(customPojo.at("/inputs/0/value/currency").asText())
        .as("Nested path in first element is incorrect")
        .isEqualTo("EUR");
    Assertions.assertThat(customPojo.at("/inputs/1/value/currency").asText())
        .as("Nested path in second element is incorrect")
        .isEqualTo("EUR");
  }

  @Test(description = "Verify set value in each array element rejects non array node")
  public void testSetEachAtThrowsWhenNotArray() {
    /* @Step 1: Set value in each element of non array node; Expected: IllegalArgumentException */
    Assertions.assertThatThrownBy(
            () -> new JsonPojo("{\"inputs\":{}}").setEachAt("/inputs", "/value", 1))
        .as("Non array node is not rejected")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("/inputs");
  }

  @Test(description = "Verify set value in each array element rejects missing array")
  public void testSetEachAtThrowsWhenArrayMissing() {
    /* @Step 1: Set value in each element of missing array; Expected: IllegalArgumentException from jackson */
    Assertions.assertThatThrownBy(() -> new JsonPojo().setEachAt("/missing", "/value", 1))
        .as("Missing array is not rejected")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test(description = "Verify remove node by json pointer")
  public void testRemoveAtField() {
    /* @Step 1: Prepare pojo with nested field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"bonus\":{\"amount\":10,\"currency\":\"EUR\"}}");
    /* @Step 2: Remove nested field by json pointer; Expected: Field removed */
    customPojo.removeAt("/bonus/currency");
    /* @Step 3: Validate json; Expected: Removed field is absent */
    Assertions.assertThat(customPojo.asJson())
        .as("Json after removal is incorrect")
        .isEqualTo("{\"bonus\":{\"amount\":10}}");
  }

  @Test(description = "Verify remove array element by json pointer")
  public void testRemoveAtArrayElement() {
    /* @Step 1: Prepare pojo with array field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"ids\":[1,2,3]}");
    /* @Step 2: Remove array element by json pointer; Expected: Element removed */
    customPojo.removeAt("/ids/1");
    /* @Step 3: Validate array; Expected: Target element removed */
    Assertions.assertThat(customPojo.asJson())
        .as("Array after removal is incorrect")
        .isEqualTo("{\"ids\":[1,3]}");
  }

  @Test(description = "Verify remove node by missing json pointer is no-op")
  public void testRemoveAtMissingPathIsNoOp() {
    /* @Step 1: Prepare pojo; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"name\":\"John\"}");
    /* @Step 2: Remove node by missing json pointer; Expected: No exception, json unchanged */
    customPojo.removeAt("/missing/path");
    /* @Step 3: Validate json; Expected: Json unchanged */
    Assertions.assertThat(customPojo.asJson())
        .as("Json is changed")
        .isEqualTo("{\"name\":\"John\"}");
  }

  @Test(description = "Verify get node by json pointer returns missing node for absent path")
  public void testAtReturnsMissingNodeForAbsentPath() {
    /* @Step 1: Get node by absent json pointer; Expected: Missing node without exception */
    JsonNode node = new JsonPojo().at("/absent/path");
    /* @Step 2: Validate node; Expected: Node is missing */
    Assertions.assertThat(node.isMissingNode()).as("Node is not missing").isTrue();
  }

  @Test(description = "Verify get required node by json pointer")
  public void testRequiredAtReturnsNode() {
    /* @Step 1: Prepare pojo with nested error field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"errors\":{\"name\":[\"is required\"]}}");
    /* @Step 2: Get required node by json pointer; Expected: Node returned */
    Assertions.assertThat(customPojo.requiredAt("/errors/name/0").asText())
        .as("Required node is incorrect")
        .isEqualTo("is required");
  }

  @Test(description = "Verify get required node by absent json pointer fails with jackson exception")
  public void testRequiredAtThrowsForAbsentPath() {
    /* @Step 1: Get required node by absent json pointer; Expected: IllegalArgumentException from jackson */
    Assertions.assertThatThrownBy(() -> new JsonPojo().requiredAt("/absent/path"))
        .as("Absent path is not rejected")
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test(description = "Verify read typed value by json pointer")
  public void testReadAtTypedPojo() {
    /* @Step 1: Prepare pojo with nested object; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo()
        .setField("cat", new CatFactPojo("cats run", 8));
    /* @Step 2: Read nested object as typed pojo; Expected: Typed pojo deserialized */
    CatFactPojo catFactPojo = customPojo.readAt("/cat", CatFactPojo.class);
    /* @Step 3: Validate typed pojo; Expected: Fields deserialized correctly */
    Assertions.assertThat(catFactPojo)
        .as("Typed pojo is incorrect")
        .isEqualTo(new CatFactPojo("cats run", 8));
  }

  @Test(description = "Verify read scalar value by json pointer")
  public void testReadAtScalar() {
    /* @Step 1: Prepare pojo with scalar field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"fact\":\"cats sleep\",\"length\":10}");
    /* @Step 2: Read scalar values by json pointer; Expected: Typed scalars deserialized */
    Assertions.assertThat(customPojo.readAt("/fact", String.class))
        .as("String value is incorrect")
        .isEqualTo("cats sleep");
    Assertions.assertThat(customPojo.readAt("/length", Integer.class))
        .as("Integer value is incorrect")
        .isEqualTo(10);
  }

  @Test(description = "Verify find first object by field value")
  public void testFindFirstObjectByFieldValueFound() {
    /* @Step 1: Prepare pojo with array of typed objects; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo(
        "{\"actions\":[{\"type\":\"email\"},{\"type\":\"time_delay\",\"value\":5}]}");
    /* @Step 2: Find first object by field value; Expected: Matching object found */
    Optional<ObjectNode> found = customPojo.findFirstObjectByFieldValue("type", "time_delay");
    /* @Step 3: Validate found object; Expected: Object with expected fields */
    Assertions.assertThat(found).as("Object is not found").isPresent();
    Assertions.assertThat(found.get().path("value").asInt())
        .as("Found object is incorrect")
        .isEqualTo(5);
  }

  @Test(description = "Verify find first object by field value returns empty for no match")
  public void testFindFirstObjectByFieldValueNotFound() {
    /* @Step 1: Prepare pojo without matching objects; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"actions\":[{\"type\":\"email\"}]}");
    /* @Step 2: Find first object by absent field value; Expected: Empty optional */
    Assertions.assertThat(customPojo.findFirstObjectByFieldValue("type", "time_delay"))
        .as("Optional is not empty")
        .isEmpty();
  }

  @Test(description = "Verify custom pojo json round trip")
  public void testAsJsonRoundTrip() {
    /* @Step 1: Prepare pojo from json string; Expected: Test data prepared */
    String json = "{\"name\":\"John\",\"tags\":[\"a\",\"b\"],\"meta\":{\"age\":20}}";
    /* @Step 2: Serialize pojo back to json; Expected: Json unchanged */
    Assertions.assertThat(new JsonPojo(json).asJson())
        .as("Round trip json is incorrect")
        .isEqualTo(json);
  }

  @Test(description = "Verify set value by json pointer with numeric key on existing object")
  public void testSetAtNumericKeyOnExistingObject() {
    /* @Step 1: Prepare pojo with object keyed by numeric strings; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"mapping\":{\"1\":\"x\"}}");
    /* @Step 2: Set value by json pointer with numeric segment; Expected: Object property set, not array */
    customPojo.setAt("/mapping/2", "y");
    /* @Step 3: Validate json; Expected: Object with both numeric keys */
    Assertions.assertThat(customPojo.asJson())
        .as("Object with numeric keys is incorrect")
        .isEqualTo("{\"mapping\":{\"1\":\"x\",\"2\":\"y\"}}");
  }

  @Test(description = "Verify set field deep copies json node values")
  public void testSetFieldCopiesJsonNodeValue() {
    /* @Step 1: Prepare source pojo with nested object; Expected: Test data prepared */
    JsonPojo sourcePojo = new JsonPojo("{\"settings\":{\"max_win\":100}}");
    /* @Step 2: Set node from source pojo into target pojo and mutate target; Expected: Node deep copied */
    JsonPojo targetPojo = new JsonPojo()
        .setField("settings", sourcePojo.at("/settings"))
        .setAt("/settings/max_win", 999);
    /* @Step 3: Validate source pojo; Expected: Source pojo is not affected */
    Assertions.assertThat(sourcePojo.requiredAt("/settings/max_win").asInt())
        .as("Source pojo is mutated through shared node")
        .isEqualTo(100);
    Assertions.assertThat(targetPojo.requiredAt("/settings/max_win").asInt())
        .as("Target pojo is not mutated")
        .isEqualTo(999);
  }

  @Test(description = "Verify set value in each array element skips non object elements")
  public void testSetEachAtSkipsNonObjectElements() {
    /* @Step 1: Prepare pojo with mixed array elements; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"inputs\":[{\"value\":1},\"skip-me\",null]}");
    /* @Step 2: Set value in each array element; Expected: Only object elements updated */
    customPojo.setEachAt("/inputs", "/value", 2);
    /* @Step 3: Validate array; Expected: Non object elements unchanged */
    Assertions.assertThat(customPojo.asJson())
        .as("Mixed array is incorrect")
        .isEqualTo("{\"inputs\":[{\"value\":2},\"skip-me\",null]}");
  }

  @Test(description = "Verify set property by json pointer on array node is rejected")
  public void testSetAtPropertyOnArrayThrows() {
    /* @Step 1: Set property by json pointer on array node; Expected: IllegalArgumentException with pointer */
    Assertions.assertThatThrownBy(() -> new JsonPojo("{\"ids\":[1,2]}").setAt("/ids/name", "x"))
        .as("Property on array node is not rejected")
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("name");
  }

  @Test(description = "Verify find first object by null field value returns empty without exception")
  public void testFindFirstObjectByFieldValueWithNullValue() {
    /* @Step 1: Prepare pojo with array of objects; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo("{\"actions\":[{\"type\":\"email\"}]}");
    /* @Step 2: Find first object by null field value; Expected: Empty optional without NPE */
    Assertions.assertThat(customPojo.findFirstObjectByFieldValue("type", null))
        .as("Optional is not empty")
        .isEmpty();
  }

  @Test(description = "Verify mutation methods chaining")
  public void testMutationMethodsChaining() {
    /* @Step 1: Chain mutation methods; Expected: All mutations applied in order */
    JsonPojo customPojo = new JsonPojo("{\"inputs\":[{\"value\":{\"games\":1}}]}")
        .setField("name", "bonus")
        .setAt("/settings/max_win", 100)
        .setEachAt("/inputs", "/value/games", 2)
        .removeAt("/settings/max_win");
    /* @Step 2: Validate resulting json; Expected: All mutations applied */
    Assertions.assertThat(customPojo.asJson())
        .as("Chained mutations result is incorrect")
        .isEqualTo("{\"inputs\":[{\"value\":{\"games\":2}}],\"name\":\"bonus\",\"settings\":{}}");
  }

  @Test(description = "Verify instance json array serialization wraps underlying json")
  public void testAsJsonArrayWrapsSinglePojo() {
    /* @Step 1: Prepare pojo with scalar field; Expected: Test data prepared */
    JsonPojo customPojo = new JsonPojo().setField("name", "John");
    /* @Step 2: Serialize pojo as json array; Expected: Array with single element */
    Assertions.assertThat(customPojo.asJsonArray())
        .as("Json array is incorrect")
        .isEqualTo("[{\"name\":\"John\"}]");
  }

  @Test(description = "Verify static json array serialization joins several pojos")
  public void testAsJsonArrayJoinsSeveralPojos() {
    /* @Step 1: Prepare several pojos; Expected: Test data prepared */
    JsonPojo first = new JsonPojo().setField("name", "John");
    JsonPojo second = new JsonPojo("{\"name\":\"Jane\"}");
    /* @Step 2: Serialize pojos as json array; Expected: Array with both elements in order */
    Assertions.assertThat(JsonPojo.asJsonArray(first, second))
        .as("Json array is incorrect")
        .isEqualTo("[{\"name\":\"John\"},{\"name\":\"Jane\"}]");
  }

  record CatFactPojo(String fact, Integer length) implements Pojo {
  }
}
