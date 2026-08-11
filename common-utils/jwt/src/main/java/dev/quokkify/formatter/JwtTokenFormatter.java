package dev.quokkify.formatter;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

import dev.quokkify.model.Header;
import dev.quokkify.model.JwtKeyPair;
import dev.quokkify.model.Payload;
import dev.quokkify.util.JsonConverter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwe;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;

/**
 * Format JWT tokens from/into specified types.
 * <p>
 * Supports both:
 * <ul>
 *   <li>JWS (signed, 3 segments: header.payload.signature)</li>
 *   <li>JWE (encrypted, 5 segments: header.encryptedKey.iv.ciphertext.tag)</li>
 * </ul>
 */
public final class JwtTokenFormatter {

  private JwtTokenFormatter() {
  }

  // ===========================
  // Public API
  // ===========================

  /**
   * Get typed header from JWT token.
   * <ul>
   *   <li>JWS: header is taken from verified {@link Jws#getHeader()} (no JSON parsing)</li>
   *   <li>JWE: header JSON is decoded from segment[0] and mapped to {@link Header}</li>
   * </ul>
   *
   * @param jwtKeyPair key pair (public key is used to verify JWS; private key is not required here)
   * @param jwtToken   compact JWT (JWS or JWE)
   * @return Header record
   */
  public static Header getHeader(JwtKeyPair jwtKeyPair, String jwtToken) {
    ensureToken(jwtToken);
    if (isJws(jwtToken)) {
      Jws<Claims> jws = parseSignedJws(jwtKeyPair, jwtToken);
      JwsHeader header = jws.getHeader();
      return new Header(header.getAlgorithm(), header.getType(), header.getKeyId());
    } else if (isJwe(jwtToken)) {
      String headerJson = decodeSegmentAsJson(jwtToken, 0);
      return JsonConverter.fromString(headerJson, Header.class);
    } else {
      throw new IllegalArgumentException("Unsupported JWT format: expected 3 (JWS) or 5 (JWE) segments");
    }
  }

  /**
   * Header JSON as String (raw header segment).
   * <ul>
   *   <li>JWS/JWE: always Base64URL-decodes segment[0] to JSON</li>
   * </ul>
   */
  public static String formatHeaderAsString(JwtKeyPair jwtKeyPair, String jwtToken) {
    ensureToken(jwtToken);
    if (isJws(jwtToken) || isJwe(jwtToken)) {
      return decodeSegmentAsJson(jwtToken, 0);
    }
    throw new IllegalArgumentException("Unsupported JWT format: expected 3 (JWS) or 5 (JWE) segments");
  }

  /**
   * Get typed payload from JWT token.
   * <ul>
   *   <li>JWS: payload JSON is decoded from segment[1] and mapped</li>
   *   <li>JWE: payload is decrypted with private key and mapped</li>
   * </ul>
   */
  public static Payload getPayload(JwtKeyPair jwtKeyPair, String jwtToken) {
    String payloadJson = formatPayloadAsString(jwtKeyPair, jwtToken);
    return JsonConverter.fromString(payloadJson, Payload.class);
  }

  /**
   * Payload JSON as String.
   * <ul>
   *   <li>JWS: Base64URL-decodes segment[1]</li>
   *   <li>JWE: decrypts with private key and returns JSON text</li>
   * </ul>
   */
  public static String formatPayloadAsString(JwtKeyPair jwtKeyPair, String jwtToken) {
    return decryptPayloadAsString(jwtKeyPair, jwtToken);
  }

  /**
   * Decrypts and extracts the payload from a compact JWT string.
   * <p>
   * Supports:
   * <ul>
   *   <li><b>JWS</b> (3 segments): payload is not encrypted; Base64URL-decodes segment[1]</li>
   *   <li><b>JWE</b> (5 segments): payload is encrypted; uses {@code decryptWith(privateKey)} and
   *   {@code parseEncryptedClaims(token)} to obtain claims and serialize to JSON</li>
   * </ul>
   *
   * @param jwtKeyPair key pair (public key used for JWS verification; private key required for JWE decryption)
   * @param jwtToken   compact JWT string; must be non-null and non-empty
   * @return JSON payload as String
   */
  public static String decryptPayloadAsString(JwtKeyPair jwtKeyPair, String jwtToken) {
    ensureToken(jwtToken);
    if (isJws(jwtToken)) {
      // JWS: just decode payload segment[1]
      return decodeSegmentAsJson(jwtToken, 1);
    } else if (isJwe(jwtToken)) {
      Jwe<Claims> jwe = parseEncryptedJwe(jwtKeyPair, jwtToken);
      return JsonConverter.toJson(jwe.getPayload());
    } else {
      throw new IllegalArgumentException("Unsupported JWT format: expected 3 (JWS) or 5 (JWE) segments");
    }
  }

  // ===========================
  // Internals / helpers
  // ===========================

  private static void ensureToken(String jwtToken) {
    if (jwtToken == null || jwtToken.isEmpty()) {
      throw new IllegalArgumentException("JWT is null or empty");
    }
  }

  private static boolean isJws(String jwtToken) {
    String[] parts = split(jwtToken);
    return parts.length == 3;
  }

  private static boolean isJwe(String jwtToken) {
    String[] parts = split(jwtToken);
    return parts.length == 5;
  }

  private static String[] split(String jwtToken) {
    return jwtToken.split("\\.");
  }

  /**
   * Parse & verify a signed JWS with the public key.
   * No decryption here (that's for JWE).
   */
  private static Jws<Claims> parseSignedJws(JwtKeyPair jwtKeyPair, String jwtToken) {
    Objects.requireNonNull(jwtKeyPair, "jwtKeyPair must not be null");
    Objects.requireNonNull(jwtKeyPair.keyPair(), "jwtKeyPair.keyPair() must not be null");
    return Jwts.parser()
        .verifyWith(jwtKeyPair.keyPair().getPublic())
        .build()
        .parseSignedClaims(jwtToken);
  }

  /**
   * Decrypt an encrypted JWE with the private key.
   */
  private static Jwe<Claims> parseEncryptedJwe(JwtKeyPair jwtKeyPair, String jwtToken) {
    Objects.requireNonNull(jwtKeyPair, "jwtKeyPair must not be null");
    Objects.requireNonNull(jwtKeyPair.keyPair(), "jwtKeyPair.keyPair() must not be null");
    return Jwts.parser()
        .decryptWith(jwtKeyPair.keyPair().getPrivate())
        .build()
        .parseEncryptedClaims(jwtToken);
  }

  /**
   * Decode a specific JWT segment (0=header, 1=payload) as UTF-8 JSON.
   * Works for both JWS and JWE header; for payload only valid for JWS.
   */
  private static String decodeSegmentAsJson(String jwtToken, int index) {
    String[] parts = split(jwtToken);
    if (index < 0 || index >= parts.length) {
      throw new IllegalArgumentException("Segment index out of bounds: " + index);
    }
    if (index == 1 && parts.length < 2) {
      throw new IllegalArgumentException("Invalid JWT: not enough segments to extract payload");
    }
    byte[] json = Decoders.BASE64URL.decode(parts[index]);
    return new String(json, StandardCharsets.UTF_8);
  }
}
