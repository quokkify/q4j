package dev.quokkify.util;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import dev.quokkify.config.AesEncryptionConfiguration;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.constant.EncryptionAlgorithm;

/**
 * Holds algorithm mode, prebuilt SecretKey and optional IV.
 * <p>
 * Usage example:
 * AesEncryptionService aes = AesEncryptionService.fromConfig();
 * String plain = aes.decrypt(encrypted);
 */
public record AesEncryption(
    String algorithmMode,
    SecretKey secretKey,
    IvParameterSpec ivParameterSpec
) {

  /**
   * Factory that reads settings from {@link AesEncryptionConfiguration}.
   */
  public static AesEncryption fromConfig() {
    AesEncryptionConfiguration cfg = ConfigRegistry.get(AesEncryptionConfiguration.class);
    SecretKey key = generateSecretKey(cfg.secretKey());
    IvParameterSpec iv = Objects.nonNull(cfg.initializationVector())
        ? getIvParameterSpec(cfg.initializationVector())
        : null;
    return new AesEncryption(cfg.algorithmMode(), key, iv);
  }

  /**
   * Convenience singleton built from Owner config (optional to use).
   */
  public static final AesEncryption DEFAULT = AesEncryption.fromConfig();

  /**
   * Decrypt value according to AES algorithm mode with initialization vector if present.
   *
   * @param encryptedValue encrypted string to decrypt (Base64 or similar as used by EncryptionUtils.decodeString)
   * @return decrypted UTF-8 string
   */
  public String decrypt(String encryptedValue)
      throws NoSuchPaddingException,
      NoSuchAlgorithmException,
      InvalidKeyException,
      IllegalBlockSizeException,
      BadPaddingException,
      InvalidAlgorithmParameterException {
    Cipher cipher = Cipher.getInstance(algorithmMode);
    if (ivParameterSpec != null) {
      cipher.init(Cipher.DECRYPT_MODE, secretKey, ivParameterSpec);
    } else {
      cipher.init(Cipher.DECRYPT_MODE, secretKey);
    }
    byte[] decoded = EncryptionUtils.decodeString(encryptedValue);
    return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
  }

  private static SecretKey generateSecretKey(String keyString) {
    return new SecretKeySpec(EncryptionUtils.decodeString(keyString), EncryptionAlgorithm.AES.name());
  }

  private static IvParameterSpec getIvParameterSpec(String initializationVector) {
    return new IvParameterSpec(EncryptionUtils.decodeString(initializationVector));
  }
}
