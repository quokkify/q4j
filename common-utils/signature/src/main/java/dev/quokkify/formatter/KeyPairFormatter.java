package dev.quokkify.formatter;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Objects;

import dev.quokkify.constant.EncryptionAlgorithm;

/**
 * Formats {@link KeyPair} from/into specified types.
 *
 * <p>Supports Base64-encoded strings and PEM (PKCS#8 for private keys, X.509/SPKI for public keys):</p>
 * <ul>
 *   <li>Private key PEM header: {@code -----BEGIN PRIVATE KEY-----}</li>
 *   <li>Public key PEM header:  {@code -----BEGIN PUBLIC KEY-----}</li>
 * </ul>
 *
 * <p><b>Note:</b> PKCS#1 ("-----BEGIN RSA PRIVATE KEY-----") is not supported here;
 * convert it to PKCS#8 before use.</p>
 */
public final class KeyPairFormatter {

  private static final String PEM_PRIVATE_BEGIN = "-----BEGIN PRIVATE KEY-----";
  private static final String PEM_PRIVATE_END   = "-----END PRIVATE KEY-----";
  private static final String PEM_PUBLIC_BEGIN  = "-----BEGIN PUBLIC KEY-----";
  private static final String PEM_PUBLIC_END    = "-----END PUBLIC KEY-----";
  private static final String PEM_RSA_PKCS1_BEGIN = "-----BEGIN RSA PRIVATE KEY-----";

  private KeyPairFormatter() {
  }

  /**
   * Build a {@link KeyPair} from Base64/PEM strings using RSA algorithm.
   *
   * @param privateKeyContent Base64 or PEM PKCS#8 private key
   * @param publicKeyContent  Base64 or PEM X.509 public key
   * @return {@link KeyPair}
   * @throws RuntimeException on invalid data or if the algorithm is not available
   */
  public static KeyPair format(String privateKeyContent, String publicKeyContent) {
    return format(privateKeyContent, publicKeyContent, EncryptionAlgorithm.RSA.name());
  }

  /**
   * Build a {@link KeyPair} from Base64/PEM strings using specified algorithm.
   *
   * @param privateKeyContent Base64 or PEM PKCS#8 private key
   * @param publicKeyContent  Base64 or PEM X.509 public key
   * @param algorithm         JCA algorithm name (e.g., "RSA", "EC")
   * @return {@link KeyPair}
   * @throws RuntimeException on invalid data or if the algorithm is not available
   */
  public static KeyPair format(String privateKeyContent, String publicKeyContent, String algorithm) {
    Objects.requireNonNull(algorithm, "algorithm must not be null");
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(algorithm);
      PrivateKey privateKey = formatPrivateKey(keyFactory, privateKeyContent);
      PublicKey publicKey = formatPublicKey(keyFactory, publicKeyContent);
      return new KeyPair(publicKey, privateKey);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Unsupported key algorithm: " + algorithm, e);
    }
  }

  /**
   * Convert Base64/PEM PKCS#8 private key into {@link PrivateKey} using provided {@link KeyFactory}.
   *
   * @param keyFactory        key factory
   * @param privateKeyContent Base64 or PEM PKCS#8 private key
   * @return {@link PrivateKey}
   * @throws RuntimeException on invalid data
   */
  public static PrivateKey formatPrivateKey(KeyFactory keyFactory, String privateKeyContent) {
    Objects.requireNonNull(keyFactory, "keyFactory must not be null");
    byte[] keyBytes = decodePrivateKey(privateKeyContent);
    try {
      return keyFactory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException("Invalid PKCS#8 private key", e);
    }
  }

  /**
   * Convert Base64/PEM X.509 public key into {@link PublicKey} using provided {@link KeyFactory}.
   *
   * @param keyFactory       key factory
   * @param publicKeyContent Base64 or PEM X.509 public key
   * @return {@link PublicKey}
   * @throws RuntimeException on invalid data
   */
  public static PublicKey formatPublicKey(KeyFactory keyFactory, String publicKeyContent) {
    Objects.requireNonNull(keyFactory, "keyFactory must not be null");
    byte[] keyBytes = decodePublicKey(publicKeyContent);
    try {
      return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
    } catch (InvalidKeySpecException e) {
      throw new RuntimeException("Invalid X.509 public key", e);
    }
  }

  private static byte[] decodePrivateKey(String content) {
    String normalized = normalize(content);
    if (normalized.contains(PEM_RSA_PKCS1_BEGIN)) {
      throw new RuntimeException(
          "PKCS#1 private key is not supported here. Convert it to PKCS#8 (-----BEGIN PRIVATE KEY-----).");
    }
    String base64 = extractPemIfPresent(normalized, PEM_PRIVATE_BEGIN, PEM_PRIVATE_END);
    return base64Decode(base64);
  }

  private static byte[] decodePublicKey(String content) {
    String normalized = normalize(content);
    String base64 = extractPemIfPresent(normalized, PEM_PUBLIC_BEGIN, PEM_PUBLIC_END);
    return base64Decode(base4(base64));
  }

  private static String extractPemIfPresent(String content, String begin, String end) {
    String c = content;
    int iBegin = c.indexOf(begin);
    int iEnd = c.indexOf(end);
    if (iBegin >= 0 && iEnd > iBegin) {
      c = c.substring(iBegin + begin.length(), iEnd);
    }
    return base4(c);
  }

  private static String base4(String s) {
    return s.replaceAll("\\s+", "");
  }

  private static String normalize(String s) {
    if (s == null) {
      throw new IllegalArgumentException("Key content must not be null");
    }
    String trimmed = s.trim();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("Key content must not be empty");
    }
    return trimmed;
  }

  private static byte[] base64Decode(String base64) {
    try {
      return Base64.getDecoder().decode(base64);
    } catch (IllegalArgumentException e) {
      throw new RuntimeException("Invalid Base64 key content", e);
    }
  }
}
