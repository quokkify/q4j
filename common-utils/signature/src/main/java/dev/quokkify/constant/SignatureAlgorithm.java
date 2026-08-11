package dev.quokkify.constant;

/**
 * Supported signature algorithms with their JCA standard names.
 */
public enum SignatureAlgorithm {
  SHA256_WITH_RSA("SHA256withRSA"),
  SHA512_WITH_RSA("SHA512withRSA"),
  HMAC_SHA256("HmacSHA256");

  private final String value;

  SignatureAlgorithm(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }
}
