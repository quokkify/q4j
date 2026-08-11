package dev.quokkify.test;

import java.security.SecureRandom;
import java.util.Base64;

import dev.quokkify.annotation.TestGroup;
import dev.quokkify.config.ConfigRegistry;
import dev.quokkify.config.TestConfig;

import io.qameta.allure.TmsLink;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

public class EncryptionTest {

  private static final TestConfig CONFIG = ConfigRegistry.get(TestConfig.class);
  private static final SecureRandom RANDOM = new SecureRandom();

  // @Test
  void main() {
    byte[] key = new byte[16]; // 128 bits
    byte[] iv = new byte[16];  // 128 bits for AES block

    RANDOM.nextBytes(key);
    RANDOM.nextBytes(iv);

    String base64Key = Base64.getEncoder().encodeToString(key);
    String base64IV = Base64.getEncoder().encodeToString(iv);

    System.out.println("SECRET_KEY=" + base64Key);
    System.out.println("INITIALIZATION_VECTOR=" + base64IV);
  }

  @TmsLink("ENCRYPTION_ID_1")
  @TestGroup("Signature")
  @Test(description = "Verify decryption functionality")
  public void testDecryptionFunctionality() {
    String encryptionValue = "TEST_VALUE";
    String decryptedPropertyValue = CONFIG.testDecryptedString();

    /* @Step 1: Verify loaded data; Expected: Loaded data is as expected */
    Assertions.assertThat(encryptionValue)
        .as("Decrypted value is incorrect")
        .isEqualTo(decryptedPropertyValue);
  }
}
