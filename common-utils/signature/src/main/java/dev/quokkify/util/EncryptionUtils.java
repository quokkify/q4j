package dev.quokkify.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Utility class for encoding and decoding data using Base64 encoding.
 */
public class EncryptionUtils {

  private static final Base64.Decoder DECODER = Base64.getDecoder();
  private static final Base64.Encoder ENCODER = Base64.getEncoder();

  private EncryptionUtils() {
  }

  public static byte[] decodeString(String string) {
    return DECODER.decode(string);
  }

  public static String encodeBytes(byte[] bytes) {
    return new String(ENCODER.encode(bytes), StandardCharsets.UTF_8);
  }
}
