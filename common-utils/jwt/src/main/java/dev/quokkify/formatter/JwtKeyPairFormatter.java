package dev.quokkify.formatter;

import dev.quokkify.model.JwtKeyPair;
import dev.quokkify.util.EncryptionUtils;

/**
 * Formatting {@link JwtKeyPair} to obtain keys.
 */
public class JwtKeyPairFormatter {

  private JwtKeyPairFormatter() {
  }

  /**
   * Get private key from {@link JwtKeyPair} and format as String in base64.
   *
   * @param jwtKeyPair JWT key pair
   * @return private key as {@link String}
   */
  public static String formatPrivateKey(JwtKeyPair jwtKeyPair) {
    return EncryptionUtils.encodeBytes(jwtKeyPair.keyPair().getPrivate().getEncoded());
  }

  /**
   * Get public key from {@link JwtKeyPair} and format as String in base64.
   *
   * @param jwtKeyPair JWT key pair
   * @return public key as {@link String}
   */
  public static String formatPublicKey(JwtKeyPair jwtKeyPair) {
    return EncryptionUtils.encodeBytes(jwtKeyPair.keyPair().getPublic().getEncoded());
  }
}
