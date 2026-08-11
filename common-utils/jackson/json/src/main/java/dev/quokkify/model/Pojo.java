package dev.quokkify.model;

import dev.quokkify.util.JsonConverter;

/**
 * Interface for all types of pojo.
 */
public interface Pojo {

  /**
   * Convert pojo to String.
   *
   * @return pojo as {@link String}
   */
  default String asJson() {
    return JsonConverter.toJson(this);
  }
}
