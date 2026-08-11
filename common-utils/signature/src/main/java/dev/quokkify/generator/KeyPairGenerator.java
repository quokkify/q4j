package dev.quokkify.generator;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;

/**
 * Generator for encryption key pairs.
 */
public final class KeyPairGenerator {

  private KeyPairGenerator() {
  }

  /**
   * Generate RSA key pair with given key size.
   *
   * @param keySize key size in bits (e.g. 2048, 4096)
   * @return generated RSA key pair
   * @throws NoSuchAlgorithmException if RSA algorithm is not available
   */
  public static KeyPair generateRsaKeyPair(int keySize) throws NoSuchAlgorithmException {
    return generateKeyPair("RSA", keySize);
  }

  /**
   * Generate key pair with specified algorithm and key size.
   *
   * @param algorithm algorithm name (e.g. "RSA", "EC", "Ed25519")
   * @param keySize   key size in bits
   * @return generated key pair
   * @throws NoSuchAlgorithmException if the algorithm is not available
   */
  public static KeyPair generateKeyPair(String algorithm, int keySize) throws NoSuchAlgorithmException {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance(algorithm);
    generator.initialize(keySize);
    return generator.generateKeyPair();
  }

  /**
   * Generate key pair with algorithm, provider and key size.
   *
   * @param algorithm algorithm name
   * @param provider  provider name (e.g. "BC")
   * @param keySize   key size in bits
   * @return generated key pair
   * @throws NoSuchAlgorithmException if the algorithm is not available
   * @throws NoSuchProviderException  if the provider is not available
   */
  public static KeyPair generateKeyPair(String algorithm, String provider, int keySize)
      throws NoSuchAlgorithmException, NoSuchProviderException {
    java.security.KeyPairGenerator generator = java.security.KeyPairGenerator.getInstance(algorithm, provider);
    generator.initialize(keySize);
    return generator.generateKeyPair();
  }

  /**
   * List available providers for a given algorithm.
   *
   * @param algorithm algorithm name
   * @return array of providers that support the algorithm
   */
  public static Provider[] listProviders(String algorithm) {
    return Security.getProviders("KeyPairGenerator." + algorithm);
  }
}
