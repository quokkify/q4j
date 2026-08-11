package dev.quokkify.model;

/**
 * Header type for JWT token.
 */
public interface JwtHeader {

  /**
   * Token algorithm.
   * note: Serialize name must be 'alg'
   *
   * @return {@link String}, ex: 'RS512'
   */
  String getAlgorithm();

  /**
   * Token type.
   * note: Serialize name must be 'typ'
   *
   * @return {@link String}, ex: 'JWT'
   */
  String getType();

  /**
   * Token key id.
   * note: Serialize name must be 'kid'
   *
   * @return {@link String}, ex: '1'
   */
  String getKeyId();
}
