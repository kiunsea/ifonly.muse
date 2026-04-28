package com.ifonly.museagent.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for EncryptionUtil
 *
 * @author if-only
 * @version 0.1.0
 */
@DisplayName("EncryptionUtil Tests")
class EncryptionUtilTest {

  private EncryptionUtil encryptionUtil;
  private String testKey;

  @BeforeEach
  void setUp() {
    testKey = EncryptionUtil.generateKey();
    encryptionUtil = new EncryptionUtil(testKey);
  }

  @Test
  @DisplayName("should generate valid AES-256 key")
  void testGenerateKey() {
    String key = EncryptionUtil.generateKey();

    assertThat(key).isNotNull();
    assertThat(key).isNotEmpty();
    // AES-256 key is 32 bytes, Base64 encoded = 44 characters
    assertThat(key.length()).isEqualTo(44);
  }

  @Test
  @DisplayName("should encrypt and decrypt plaintext successfully")
  void testEncryptDecrypt() {
    String plaintext = "Hello, World! 안녕하세요!";

    String encrypted = encryptionUtil.encrypt(plaintext);
    String decrypted = encryptionUtil.decrypt(encrypted);

    assertThat(encrypted).isNotEqualTo(plaintext);
    assertThat(decrypted).isEqualTo(plaintext);
  }

  @Test
  @DisplayName("should produce different ciphertext for same plaintext")
  void testEncryptionRandomness() {
    String plaintext = "Test message";

    String encrypted1 = encryptionUtil.encrypt(plaintext);
    String encrypted2 = encryptionUtil.encrypt(plaintext);

    // Due to random IV, same plaintext should produce different ciphertext
    assertThat(encrypted1).isNotEqualTo(encrypted2);

    // But both should decrypt to the same plaintext
    assertThat(encryptionUtil.decrypt(encrypted1)).isEqualTo(plaintext);
    assertThat(encryptionUtil.decrypt(encrypted2)).isEqualTo(plaintext);
  }

  @Test
  @DisplayName("should handle empty string")
  void testEmptyString() {
    String result = encryptionUtil.encrypt("");
    assertThat(result).isEmpty();

    result = encryptionUtil.decrypt("");
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("should handle null value")
  void testNullValue() {
    String result = encryptionUtil.encrypt(null);
    assertThat(result).isNull();

    result = encryptionUtil.decrypt(null);
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("should encrypt long text successfully")
  void testLongText() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 1000; i++) {
      sb.append("This is a long test message. ");
    }
    String longText = sb.toString();

    String encrypted = encryptionUtil.encrypt(longText);
    String decrypted = encryptionUtil.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(longText);
  }

  @Test
  @DisplayName("should detect encrypted data correctly")
  void testIsEncrypted() {
    String plaintext = "Not encrypted";
    String encrypted = encryptionUtil.encrypt("Test data");

    assertThat(EncryptionUtil.isEncrypted(plaintext)).isFalse();
    assertThat(EncryptionUtil.isEncrypted(encrypted)).isTrue();
    assertThat(EncryptionUtil.isEncrypted(null)).isFalse();
    assertThat(EncryptionUtil.isEncrypted("")).isFalse();
  }

  @Test
  @DisplayName("should fail decryption with wrong key")
  void testDecryptWithWrongKey() {
    String plaintext = "Secret message";
    String encrypted = encryptionUtil.encrypt(plaintext);

    // Create new util with different key
    String differentKey = EncryptionUtil.generateKey();
    EncryptionUtil differentUtil = new EncryptionUtil(differentKey);

    assertThatThrownBy(() -> differentUtil.decrypt(encrypted))
        .isInstanceOf(EncryptionUtil.EncryptionException.class);
  }

  @Test
  @DisplayName("should encrypt if needed")
  void testEncryptIfNeeded() {
    String plaintext = "Plain text";
    String encrypted = encryptionUtil.encrypt(plaintext);

    // Should encrypt plaintext
    String result1 = encryptionUtil.encryptIfNeeded(plaintext);
    assertThat(result1).isNotEqualTo(plaintext);
    assertThat(encryptionUtil.decrypt(result1)).isEqualTo(plaintext);

    // Should not double-encrypt
    String result2 = encryptionUtil.encryptIfNeeded(encrypted);
    assertThat(result2).isEqualTo(encrypted);
  }

  @Test
  @DisplayName("should handle special characters")
  void testSpecialCharacters() {
    String specialChars = "!@#$%^&*()_+-=[]{}|;':\",./<>?`~\n\t\r";

    String encrypted = encryptionUtil.encrypt(specialChars);
    String decrypted = encryptionUtil.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(specialChars);
  }

  @Test
  @DisplayName("should handle unicode characters")
  void testUnicodeCharacters() {
    String unicode = "日本語 中文 한국어 العربية עברית";

    String encrypted = encryptionUtil.encrypt(unicode);
    String decrypted = encryptionUtil.decrypt(encrypted);

    assertThat(decrypted).isEqualTo(unicode);
  }
}
