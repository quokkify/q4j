package dev.quokkify.generator;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import dev.quokkify.constant.SignatureAlgorithm;
import dev.quokkify.util.EncryptionUtils;

import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Utilities for generating cryptographic signatures (RSA and HMAC).
 *
 * <p>Notes:
 * <ul>
 *   <li>Accepts raw Base64-encoded PKCS#8 private key string or PKCS#8 PEM (-----BEGIN PRIVATE KEY----- ...).</li>
 *   <li>RSA signatures are returned as Base64 (via {@link EncryptionUtils#encodeBytes(byte[])}).</li>
 *   <li>HMAC signatures are returned as lowercase hex string.</li>
 *   <li>All checked exceptions are wrapped into {@link RuntimeException} with a meaningful message.</li>
 * </ul>
 */
public final class SignatureGenerator {

  private static final Provider BC_PROVIDER = new BouncyCastleProvider();
  private static final HexFormat HEX_LOWER = HexFormat.of().withLowerCase();

  private SignatureGenerator() {
  }

  /**
   * Generate RSA signature using provided Base64/PEM PKCS#8 private key string and algorithm enum.
   */
  public static String generateRsaSignature(String privateKey, String signatureData, SignatureAlgorithm algorithm) {
    return generateRsaSignature(privateKey, signatureData, algorithm.value());
  }

  /**
   * Generate RSA signature using provided Base64/PEM PKCS#8 private key string and algorithm name.
   * Example algorithms: "SHA256withRSA", "SHA512withRSA", etc.
   */
  public static String generateRsaSignature(String privateKey, String signatureData, String algorithm) {
    try {
      PrivateKey rsaPrivateKey = generatePrivateKey(Algorithm.RSA, buildPkcs8SpecFromString(privateKey));
      return generateSignature(rsaPrivateKey, signatureData, algorithm);
    } catch (Exception e) {
      throw new RuntimeException("Failed to generate RSA signature", e);
    }
  }

  /**
   * Deprecated overload, kept for backward compatibility.
   */
  @Deprecated(since = "0.4.31")
  public static String generateRsaSignature(String privateKey, String signatureData) {
    return generateRsaSignature(privateKey, signatureData, SignatureAlgorithm.SHA256_WITH_RSA.value());
  }

  /**
   * Generate signature with provided private key and algorithm enum.
   */
  public static String generateSignature(PrivateKey privateKey, String signatureData, SignatureAlgorithm algorithm) {
    return generateSignature(privateKey, signatureData, algorithm.value());
  }

  /**
   * Generate signature with provided private key and algorithm name.
   * Returns Base64-encoded signature.
   */
  public static String generateSignature(PrivateKey privateKey, String signatureData, String algorithm) {
    try {
      Signature signature = Signature.getInstance(normalizeSigAlgorithm(algorithm));
      signature.initSign(privateKey);
      signature.update(signatureData.getBytes(StandardCharsets.UTF_8));
      return EncryptionUtils.encodeBytes(signature.sign());
    } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
      throw new RuntimeException("Failed to generate signature using algorithm: " + algorithm, e);
    }
  }

  /**
   * Generate a PrivateKey using the given algorithm and key spec.
   */
  public static PrivateKey generatePrivateKey(Algorithm algorithm, KeySpec keySpec) {
    try {
      KeyFactory keyFactory = KeyFactory.getInstance(algorithm.name(), BC_PROVIDER);
      return keyFactory.generatePrivate(keySpec);
    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
      throw new RuntimeException("Failed to generate PrivateKey for algorithm: " + algorithm, e);
    }
  }

  /**
   * Generate HMAC-SHA256 signature (hex, lowercase).
   */
  public static String generateHmacSignature(String signatureData, String key) {
    try {
      String algo = SignatureAlgorithm.HMAC_SHA256.value();
      Mac mac = Mac.getInstance(algo);
      mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algo));
      byte[] result = mac.doFinal(signatureData.getBytes(StandardCharsets.UTF_8));
      return HEX_LOWER.formatHex(result);
    } catch (NoSuchAlgorithmException | InvalidKeyException e) {
      throw new RuntimeException("Failed to generate HMAC-SHA256 signature", e);
    }
  }

  /**
   * Build PKCS#8 key spec from Base64 or PEM string.
   */
  private static PKCS8EncodedKeySpec buildPkcs8SpecFromString(String privateKey) {
    String normalized = normalizePkcs8Base64(privateKey);
    byte[] bytes = dev.quokkify.util.EncryptionUtils.decodeString(normalized);
    return new PKCS8EncodedKeySpec(bytes);
  }

  /**
   * Normalize incoming signature algorithm names if needed.
   */
  private static String normalizeSigAlgorithm(String algorithm) {
    return StringUtils.trim(algorithm);
  }

  /**
   * Normalize PKCS#8 private key string:
   * - If PEM, strip header/footer and all whitespace.
   * - If raw Base64, just remove whitespace/newlines.
   */
  private static String normalizePkcs8Base64(String key) {
    String k = StringUtils.trimToEmpty(key);
    final String begin = "-----BEGIN PRIVATE KEY-----";
    final String end = "-----END PRIVATE KEY-----";
    if (StringUtils.contains(k, begin) && StringUtils.contains(k, end)) {
      k = StringUtils.substringBetween(k, begin, end);
    }
    return k.replaceAll("\\s+", "");
  }

  public enum Algorithm {
    RSA
  }
}
