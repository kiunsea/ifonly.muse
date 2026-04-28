package com.ifonly.museagent.util;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;

/**
 * AES-256-GCM encryption utility for sensitive data
 *
 * <p>Provides encryption and decryption for sensitive configuration data such as passwords,
 * credentials, and other confidential information.
 *
 * @author if-only
 * @version 0.1.0
 */
@Slf4j
public class EncryptionUtil {

  private static final String ALGORITHM = "AES";
  private static final String TRANSFORMATION = "AES/GCM/NoPadding";
  private static final int KEY_SIZE = 256;
  private static final int GCM_IV_LENGTH = 12;
  private static final int GCM_TAG_LENGTH = 128;

  private final SecretKey secretKey;

  /**
   * Create encryption utility with the given key
   *
   * @param base64Key Base64 encoded secret key
   */
  public EncryptionUtil(String base64Key) {
    byte[] decodedKey = Base64.getDecoder().decode(base64Key);
    this.secretKey = new SecretKeySpec(decodedKey, 0, decodedKey.length, ALGORITHM);
    log.debug("EncryptionUtil initialized with provided key");
  }

  /**
   * Create encryption utility with the given secret key
   *
   * @param secretKey secret key
   */
  public EncryptionUtil(SecretKey secretKey) {
    this.secretKey = secretKey;
    log.debug("EncryptionUtil initialized with SecretKey");
  }

  /**
   * Generate a new AES-256 secret key
   *
   * @return Base64 encoded secret key
   */
  public static String generateKey() {
    try {
      KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
      keyGenerator.init(KEY_SIZE, new SecureRandom());
      SecretKey key = keyGenerator.generateKey();
      String base64Key = Base64.getEncoder().encodeToString(key.getEncoded());
      log.info("New AES-256 key generated");
      return base64Key;
    } catch (Exception e) {
      log.error("Failed to generate encryption key", e);
      throw new EncryptionException("Failed to generate encryption key", e);
    }
  }

  /**
   * Encrypt plaintext data
   *
   * @param plaintext data to encrypt
   * @return Base64 encoded encrypted data (IV + ciphertext)
   */
  public String encrypt(String plaintext) {
    if (plaintext == null || plaintext.isEmpty()) {
      return plaintext;
    }

    try {
      // Generate random IV
      byte[] iv = new byte[GCM_IV_LENGTH];
      new SecureRandom().nextBytes(iv);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

      // Encrypt
      byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

      // Combine IV and ciphertext
      byte[] combined = new byte[iv.length + ciphertext.length];
      System.arraycopy(iv, 0, combined, 0, iv.length);
      System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

      String encrypted = Base64.getEncoder().encodeToString(combined);
      log.trace("Data encrypted successfully");
      return encrypted;

    } catch (Exception e) {
      log.error("Encryption failed", e);
      throw new EncryptionException("Encryption failed", e);
    }
  }

  /**
   * Decrypt encrypted data
   *
   * @param encryptedData Base64 encoded encrypted data (IV + ciphertext)
   * @return decrypted plaintext
   */
  public String decrypt(String encryptedData) {
    if (encryptedData == null || encryptedData.isEmpty()) {
      return encryptedData;
    }

    try {
      // Decode Base64
      byte[] combined = Base64.getDecoder().decode(encryptedData);

      // Extract IV and ciphertext
      byte[] iv = new byte[GCM_IV_LENGTH];
      byte[] ciphertext = new byte[combined.length - GCM_IV_LENGTH];
      System.arraycopy(combined, 0, iv, 0, iv.length);
      System.arraycopy(combined, iv.length, ciphertext, 0, ciphertext.length);

      // Initialize cipher
      Cipher cipher = Cipher.getInstance(TRANSFORMATION);
      GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
      cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

      // Decrypt
      byte[] plaintext = cipher.doFinal(ciphertext);

      String decrypted = new String(plaintext, StandardCharsets.UTF_8);
      log.trace("Data decrypted successfully");
      return decrypted;

    } catch (Exception e) {
      log.error("Decryption failed", e);
      throw new EncryptionException("Decryption failed", e);
    }
  }

  /**
   * Check if a string appears to be encrypted (Base64 encoded with proper length)
   *
   * @param data data to check
   * @return true if data appears to be encrypted
   */
  public static boolean isEncrypted(String data) {
    if (data == null || data.isEmpty()) {
      return false;
    }

    try {
      byte[] decoded = Base64.getDecoder().decode(data);
      // Minimum length: IV (12) + tag (16) = 28 bytes
      return decoded.length >= 28;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

  /**
   * Encrypt if not already encrypted
   *
   * @param data data to encrypt
   * @return encrypted data
   */
  public String encryptIfNeeded(String data) {
    if (isEncrypted(data)) {
      log.trace("Data already encrypted, skipping");
      return data;
    }
    return encrypt(data);
  }

  /** Custom exception for encryption/decryption errors */
  public static class EncryptionException extends RuntimeException {
    public EncryptionException(String message) {
      super(message);
    }

    public EncryptionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
