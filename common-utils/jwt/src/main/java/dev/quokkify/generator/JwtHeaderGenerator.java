package dev.quokkify.generator;

import dev.quokkify.model.Header;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;

/**
 * Generate JWT {@link Header} using several parameters.
 */
public class JwtHeaderGenerator {

  public static final String JWT = "JWT";

  private JwtHeaderGenerator() {
  }

  /**
   * Generate JWT Header obj using {@code RSASSA-PKCS1-v1_5 using SHA-512} signature algorithm.
   *
   * @param keyId JWT case-sensitive kid (Key ID) header value
   * @return {@link Header}
   */
  public static Header generateRs512(String keyId) {
    return generate(Jwts.SIG.RS512, JWT, keyId);
  }

  /**
   * Generate JWT Header obj by given parameters.
   *
   * @param algorithm JWS algorithm {@link SignatureAlgorithm} to use with the key to digitally sign the JWT
   * @param type      JWT typ (Type) header value
   * @param keyId     JWT case-sensitive kid (Key ID) header value
   * @return {@link Header}
   */
  public static Header generate(SignatureAlgorithm algorithm, String type, String keyId) {
    return new Header(algorithm.getId(), type, keyId);
  }
}
