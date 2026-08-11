package dev.quokkify.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Immutable JWT header model.
 */
public record Header(
    @JsonProperty("alg") String algorithm,
    @JsonProperty("typ") String type,
    @JsonProperty("kid") String keyId
) implements JwtHeader {

  @Override
  public String getAlgorithm() {
    return algorithm;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getKeyId() {
    return keyId;
  }
}
