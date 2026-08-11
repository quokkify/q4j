package dev.quokkify.config;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import dev.quokkify.util.AesEncryption;

import org.aeonbits.owner.crypto.AbstractDecryptor;

/**
 * AES decryptor for Owner configs.
 */
public class ConfigAesDecryptor extends AbstractDecryptor {

  @Override
  public String decrypt(String string) {
    try {
      return string.isEmpty() ? string : AesEncryption.DEFAULT.decrypt(string);
    } catch (NoSuchPaddingException | NoSuchAlgorithmException | InvalidKeyException
             | IllegalBlockSizeException | BadPaddingException | InvalidAlgorithmParameterException e) {
      throw new RuntimeException("Failed to decrypt config value", e);
    }
  }
}
