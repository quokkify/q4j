package dev.quokkify.model;

/**
 * Payload type for JWT token.
 */
public interface JwtPayload {

  /**
   * Player session.
   *
   * @return {@link String}, ex: '768a5a92126c85dc57a67d3ba092ae13'
   */
  String getSession();

  /**
   * Player id.
   * note: Serialize name must be 'user_id'
   *
   * @return {@link Long}
   */
  Long getUserId();

  /**
   * Ip address.
   *
   * @return {@link String}, ex: '127.0.0.1'
   */
  String getIp();

  /**
   * Token expiration time (seconds since Unix epoch).
   * note: Serialize name must be 'exp'
   *
   * @return {@link Long}, ex: '1698846817'
   */
  Long getExpirationTime();

  /**
   * Token issued at time (seconds since Unix epoch).
   * note: Serialize name must be 'iat'
   *
   * @return {@link Long}, ex: '1697032417'
   */
  Long getIssuedAt();

  /**
   * JWT ID (unique identifier for this token).
   * note: Serialize name must be 'jti'
   *
   * @return {@link String}, ex: '0f2ec1ef-35a6-43ee-aacc-900862846cca'
   */
  String getJwtId();
}
