package dev.quokkify.generator;

import dev.quokkify.formatter.JwtTokenFormatter;
import dev.quokkify.model.Header;
import dev.quokkify.model.JwtHeader;
import dev.quokkify.model.JwtKeyPair;
import dev.quokkify.model.JwtPayload;
import dev.quokkify.model.JwtToken;
import dev.quokkify.model.Payload;
import dev.quokkify.util.JsonConverter;

import io.jsonwebtoken.Jwts;

/**
 * Generate {@link JwtToken} or JWT token as Strings, or other types.
 */
public class JwtTokenGenerator {

  private JwtTokenGenerator() {
  }

  /**
   * Generate JWT token as obj.
   *
   * @param keyPair JWT key pair with specified key and algorithm
   * @param header  JWT header
   * @param payload JWT payload
   * @return {@link JwtToken}
   */
  public static JwtToken generate(JwtKeyPair keyPair, JwtHeader header, JwtPayload payload) {
    String token = generateAsString(keyPair, header, payload);
    Header parsedHeader = JwtTokenFormatter.getHeader(keyPair, token);
    Payload parsedPayload = JwtTokenFormatter.getPayload(keyPair, token);
    return new JwtToken(parsedHeader, parsedPayload, token);
  }

  /**
   * Generate JWT token as String.
   * note: pojo converter will ignore null fields because payload of different applications and
   * services may be different
   *
   * @param jwtKeyPair JWT key pair with specified key and algorithm
   * @param jwtHeader  JWT header
   * @param jwtPayload JWT payload (Serialized using GSON)
   * @return {@link String} with JWT token
   */
  public static String generateAsString(JwtKeyPair jwtKeyPair, JwtHeader jwtHeader, JwtPayload jwtPayload) {
    return generateAsString(jwtKeyPair, jwtHeader, JsonConverter.toJsonIgnoreNulls(jwtPayload));
  }

  /**
   * Generate JWT token as String.
   *
   * @param jwtKeyPair JWT key pair with specified key and algorithm
   * @param jwtHeader  JWT header
   * @param jwtPayload JWT payload as {@link String}
   * @return {@link String} with JWT token
   */
  public static String generateAsString(JwtKeyPair jwtKeyPair, JwtHeader jwtHeader, String jwtPayload) {
    return Jwts.builder()
        .header()
        .type(jwtHeader.getType())
        .keyId(jwtHeader.getKeyId())
        .and()
        .signWith(jwtKeyPair.keyPair().getPrivate(), jwtKeyPair.algorithm())
        .content(jwtPayload)
        .compact();
  }
}
