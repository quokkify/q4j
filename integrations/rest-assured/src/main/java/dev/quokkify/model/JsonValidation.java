package dev.quokkify.model;

/**
 * Interface for Api schema validation.
 */
public interface JsonValidation {

  /**
   * Path to json schema.
   *
   * @return path to json file as {@link String}
   */
  String getSchemaPath();
}
