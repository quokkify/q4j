package dev.quokkify.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * JWT payload object.
 */
public record Payload(
    String session,
    @JsonProperty("user_id") Long userId,
    String ip,
    @JsonProperty("exp") Long expirationTime,
    @JsonProperty("iat") Long issuedAt,
    @JsonProperty("jti") String jwtId
) implements JwtPayload {

  @Override
  public String getSession() {
    return session;
  }

  @Override
  public Long getUserId() {
    return userId;
  }

  @Override
  public String getIp() {
    return ip;
  }

  @Override
  public Long getExpirationTime() {
    return expirationTime;
  }

  @Override
  public Long getIssuedAt() {
    return issuedAt;
  }

  @Override
  public String getJwtId() {
    return jwtId;
  }
}
