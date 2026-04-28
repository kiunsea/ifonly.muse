package com.ifonly.museagent.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ifonly.museagent.util.EncryptionUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Unit tests for SecureConfigService
 *
 * @author if-only
 * @version 0.1.0
 */
@DisplayName("SecureConfigService Tests")
class SecureConfigServiceTest {

  @TempDir Path tempDir;

  private SecureConfigService secureConfigService;
  private EncryptionUtil encryptionUtil;
  private Path configFile;

  @BeforeEach
  void setUp() {
    String key = EncryptionUtil.generateKey();
    encryptionUtil = new EncryptionUtil(key);
    configFile = tempDir.resolve("secure-config.properties");
    secureConfigService = new SecureConfigService(encryptionUtil, configFile.toString());
  }

  @AfterEach
  void tearDown() throws IOException {
    // Clean up config file
    Files.deleteIfExists(configFile);
  }

  @Test
  @DisplayName("should store and retrieve secure value")
  void testSetAndGetSecureValue() {
    // Given
    String key = "test.key";
    String value = "test-value-123";

    // When
    secureConfigService.setSecureValue(key, value);
    String retrieved = secureConfigService.getSecureValue(key);

    // Then
    assertThat(retrieved).isEqualTo(value);
  }

  @Test
  @DisplayName("should return null for non-existent key")
  void testGetNonExistentKey() {
    // When
    String value = secureConfigService.getSecureValue("non.existent.key");

    // Then
    assertThat(value).isNull();
  }

  @Test
  @DisplayName("should check key existence")
  void testHasKey() {
    // Given
    secureConfigService.setSecureValue("existing.key", "value");

    // Then
    assertThat(secureConfigService.hasKey("existing.key")).isTrue();
    assertThat(secureConfigService.hasKey("non.existent.key")).isFalse();
  }

  @Test
  @DisplayName("should remove key")
  void testRemoveKey() {
    // Given
    secureConfigService.setSecureValue("key.to.remove", "value");
    assertThat(secureConfigService.hasKey("key.to.remove")).isTrue();

    // When
    secureConfigService.removeKey("key.to.remove");

    // Then
    assertThat(secureConfigService.hasKey("key.to.remove")).isFalse();
    assertThat(secureConfigService.getSecureValue("key.to.remove")).isNull();
  }

  @Test
  @DisplayName("should get all keys")
  void testGetAllKeys() {
    // Given
    secureConfigService.setSecureValue("key1", "value1");
    secureConfigService.setSecureValue("key2", "value2");
    secureConfigService.setSecureValue("key3", "value3");

    // When
    var keys = secureConfigService.getAllKeys();

    // Then
    assertThat(keys).hasSize(3);
    assertThat(keys).contains("key1", "key2", "key3");
  }

  @Test
  @DisplayName("should store email credentials")
  void testSetEmailCredentials() {
    // Given
    String host = "smtp.example.com";
    int port = 587;
    String username = "user@example.com";
    String password = "secret-password";

    // When
    secureConfigService.setEmailCredentials(host, port, username, password);

    // Then
    Map<String, String> credentials = secureConfigService.getEmailCredentials();
    assertThat(credentials).isNotNull();
    assertThat(credentials.get("host")).isEqualTo(host);
    assertThat(credentials.get("port")).isEqualTo(String.valueOf(port));
    assertThat(credentials.get("username")).isEqualTo(username);
    assertThat(credentials.get("password")).isEqualTo(password);
  }

  @Test
  @DisplayName("should return null for missing email credentials")
  void testGetEmailCredentialsNotConfigured() {
    // When
    Map<String, String> credentials = secureConfigService.getEmailCredentials();

    // Then
    assertThat(credentials).isNull();
  }

  @Test
  @DisplayName("should store OAuth2 credentials")
  void testSetOAuth2Credentials() {
    // Given
    String clientId = "my-client-id";
    String clientSecret = "my-client-secret";

    // When
    secureConfigService.setOAuth2Credentials(clientId, clientSecret);

    // Then
    Map<String, String> credentials = secureConfigService.getOAuth2Credentials();
    assertThat(credentials).isNotNull();
    assertThat(credentials.get("clientId")).isEqualTo(clientId);
    assertThat(credentials.get("clientSecret")).isEqualTo(clientSecret);
  }

  @Test
  @DisplayName("should return null for missing OAuth2 credentials")
  void testGetOAuth2CredentialsNotConfigured() {
    // When
    Map<String, String> credentials = secureConfigService.getOAuth2Credentials();

    // Then
    assertThat(credentials).isNull();
  }

  @Test
  @DisplayName("should persist and reload config")
  void testPersistAndReload() {
    // Given
    secureConfigService.setSecureValue("persistent.key", "persistent-value");

    // When - Create new instance to reload from file
    SecureConfigService newService = new SecureConfigService(encryptionUtil, configFile.toString());

    // Then
    assertThat(newService.hasKey("persistent.key")).isTrue();
    assertThat(newService.getSecureValue("persistent.key")).isEqualTo("persistent-value");
  }

  @Test
  @DisplayName("should handle multiple values")
  void testMultipleValues() {
    // Given
    for (int i = 0; i < 10; i++) {
      secureConfigService.setSecureValue("key" + i, "value" + i);
    }

    // Then
    for (int i = 0; i < 10; i++) {
      assertThat(secureConfigService.getSecureValue("key" + i)).isEqualTo("value" + i);
    }
  }

  @Test
  @DisplayName("should overwrite existing value")
  void testOverwriteValue() {
    // Given
    secureConfigService.setSecureValue("key", "original-value");
    assertThat(secureConfigService.getSecureValue("key")).isEqualTo("original-value");

    // When
    secureConfigService.setSecureValue("key", "new-value");

    // Then
    assertThat(secureConfigService.getSecureValue("key")).isEqualTo("new-value");
  }

  @Test
  @DisplayName("should handle special characters in values")
  void testSpecialCharacters() {
    // Given
    String specialValue = "password!@#$%^&*()_+-=[]{}|;':\",./<>?";

    // When
    secureConfigService.setSecureValue("special.key", specialValue);

    // Then
    assertThat(secureConfigService.getSecureValue("special.key")).isEqualTo(specialValue);
  }

  @Test
  @DisplayName("should handle unicode characters in values")
  void testUnicodeCharacters() {
    // Given
    String unicodeValue = "password-한글-日本語-中文";

    // When
    secureConfigService.setSecureValue("unicode.key", unicodeValue);

    // Then
    assertThat(secureConfigService.getSecureValue("unicode.key")).isEqualTo(unicodeValue);
  }
}
