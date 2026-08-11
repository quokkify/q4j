package dev.quokkify.generator;

import java.security.KeyPair;

import dev.quokkify.formatter.KeyPairFormatter;
import dev.quokkify.model.JwtKeyPair;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;

/**
 * Generate {@link JwtKeyPair}.
 */
public class JwtKeyPairGenerator {

  private JwtKeyPairGenerator() {
  }

  /**
   * Generate JWT key pair using private and public keys as String and {@code RSASSA-PKCS1-v1_5 using
   * SHA-512} signature algorithm.
   *
   * @param privateKeyContent Base64 encoded private key as {@link String}
   * @param publicKeyContent  Base64 encoded public key as {@link String}
   * @return {@link JwtKeyPair} with specified fields
   */
  public static JwtKeyPair generateRs512(String privateKeyContent, String publicKeyContent) {
    return generateRs512(KeyPairFormatter.format(privateKeyContent, publicKeyContent));
  }

  /**
   * Generate JWT key pair using new secure-random KeyPairs and {@code RSASSA-PKCS1-v1_5 using SHA-512} signature
   * algorithm.
   *
   * @return {@link JwtKeyPair} with specified fields
   */
  public static JwtKeyPair generateRs512() {
    return generate(Jwts.SIG.RS512);
  }

  /**
   * Generate JWT key pair using given KeyPairs and {@code RSASSA-PKCS1-v1_5 using SHA-512} signature algorithm.
   *
   * @param keyPair A simple holder for a key pair (a public key and a private key).
   * @return {@link JwtKeyPair} with specified fields
   */
  public static JwtKeyPair generateRs512(KeyPair keyPair) {
    return generate(keyPair, Jwts.SIG.RS512);
  }

  /**
   * Generate JWT key pair using new secure-random KeyPairs with a length and parameters sufficient for use with the
   * component's associated cryptographic algorithm.
   *
   * @param algorithm A digital signature algorithm computes and verifies digits using asymmetric public/private key
   *                  cryptography.
   * @return {@link JwtKeyPair} with specified fields
   */
  public static JwtKeyPair generate(SignatureAlgorithm algorithm) {
    return generate(generateKeyPair(algorithm), algorithm);
  }

  /**
   * Create new secure-random KeyPairs with a length and parameters sufficient for use with the component's
   * associated cryptographic algorithm.
   *
   * @param algorithm A digital signature algorithm computes and verifies digits using asymmetric public/private key
   *                  cryptography.
   * @return new secure-random {@link KeyPair}
   */
  private static KeyPair generateKeyPair(SignatureAlgorithm algorithm) {
    return algorithm.keyPair().build();
  }

  /**
   * Generate JWT key pair using key pair and algorithm.
   *
   * @param keyPair   A simple holder for a key pair (a public key and a private key).
   * @param algorithm A digital signature algorithm computes and verifies digits using asymmetric public/private key
   *                  cryptography.
   * @return {@link JwtKeyPair} with specified fields
   */
  public static JwtKeyPair generate(KeyPair keyPair, SignatureAlgorithm algorithm) {
    return new JwtKeyPair(algorithm, keyPair);
  }
}
