package com.ifonly.museagent.service;

import com.ifonly.museagent.util.EncryptionUtil;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for managing encrypted configuration values
 *
 * <p>Provides secure storage and retrieval of sensitive configuration data such as email
 * credentials, API keys, and other secrets.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class SecureConfigService {

  private final EncryptionUtil encryptionUtil;
  private final Path configFilePath;
  private final Map<String, String> configCache = new HashMap<>();

  @Autowired
  public SecureConfigService(
      EncryptionUtil encryptionUtil,
      @Value("${app.secure-config.file:data/secure-config.properties}") String configFile) {
    this.encryptionUtil = encryptionUtil;
    this.configFilePath = Paths.get(configFile);
    loadConfig();
    log.info("SecureConfigService initialized with file: {}", configFilePath);
  }

  /**
   * Store an encrypted value
   *
   * @param key configuration key
   * @param value plaintext value to encrypt and store
   */
  public void setSecureValue(String key, String value) {
    String encrypted = encryptionUtil.encrypt(value);
    configCache.put(key, encrypted);
    saveConfig();
    log.debug("Secure value stored for key: {}", key);
  }

  /**
   * Retrieve and decrypt a value
   *
   * @param key configuration key
   * @return decrypted value or null if not found
   */
  public String getSecureValue(String key) {
    String encrypted = configCache.get(key);
    if (encrypted == null) {
      return null;
    }

    try {
      String decrypted = encryptionUtil.decrypt(encrypted);
      log.trace("Secure value retrieved for key: {}", key);
      return decrypted;
    } catch (Exception e) {
      log.error("Failed to decrypt value for key: {}", key, e);
      return null;
    }
  }

  /**
   * Check if a key exists
   *
   * @param key configuration key
   * @return true if key exists
   */
  public boolean hasKey(String key) {
    return configCache.containsKey(key);
  }

  /**
   * Remove a key
   *
   * @param key configuration key
   */
  public void removeKey(String key) {
    configCache.remove(key);
    saveConfig();
    log.debug("Secure value removed for key: {}", key);
  }

  /**
   * Get all keys (without values)
   *
   * @return list of all configuration keys
   */
  public java.util.Set<String> getAllKeys() {
    return new java.util.HashSet<>(configCache.keySet());
  }

  /** Load configuration from file */
  private void loadConfig() {
    if (!Files.exists(configFilePath)) {
      log.info("Secure config file does not exist, starting with empty config");
      return;
    }

    try (FileInputStream fis = new FileInputStream(configFilePath.toFile())) {
      Properties props = new Properties();
      props.load(fis);

      configCache.clear();
      for (String key : props.stringPropertyNames()) {
        configCache.put(key, props.getProperty(key));
      }

      log.info("Loaded {} secure config entries from file", configCache.size());
    } catch (IOException e) {
      log.error("Failed to load secure config from file", e);
    }
  }

  /** Save configuration to file */
  private void saveConfig() {
    try {
      // Create parent directories if needed
      Path parentDir = configFilePath.getParent();
      if (parentDir != null && !Files.exists(parentDir)) {
        Files.createDirectories(parentDir);
      }

      Properties props = new Properties();
      for (Map.Entry<String, String> entry : configCache.entrySet()) {
        props.setProperty(entry.getKey(), entry.getValue());
      }

      try (FileOutputStream fos = new FileOutputStream(configFilePath.toFile())) {
        props.store(fos, "Muse Agent Secure Configuration - DO NOT EDIT MANUALLY");
      }

      log.debug("Saved {} secure config entries to file", configCache.size());
    } catch (IOException e) {
      log.error("Failed to save secure config to file", e);
    }
  }

  /**
   * Store email SMTP credentials
   *
   * @param host SMTP host
   * @param port SMTP port
   * @param username SMTP username
   * @param password SMTP password
   */
  public void setEmailCredentials(String host, int port, String username, String password) {
    setSecureValue("email.smtp.host", host);
    setSecureValue("email.smtp.port", String.valueOf(port));
    setSecureValue("email.smtp.username", username);
    setSecureValue("email.smtp.password", password);
    log.info("Email credentials stored securely");
  }

  /**
   * Get email SMTP credentials
   *
   * @return map with host, port, username, password or null if not configured
   */
  public Map<String, String> getEmailCredentials() {
    String host = getSecureValue("email.smtp.host");
    if (host == null) {
      return null;
    }

    Map<String, String> credentials = new HashMap<>();
    credentials.put("host", host);
    credentials.put("port", getSecureValue("email.smtp.port"));
    credentials.put("username", getSecureValue("email.smtp.username"));
    credentials.put("password", getSecureValue("email.smtp.password"));
    return credentials;
  }

  /**
   * Store Echo Server OAuth2 credentials
   *
   * @param clientId OAuth2 client ID
   * @param clientSecret OAuth2 client secret
   */
  public void setOAuth2Credentials(String clientId, String clientSecret) {
    setSecureValue("oauth2.client.id", clientId);
    setSecureValue("oauth2.client.secret", clientSecret);
    log.info("OAuth2 credentials stored securely");
  }

  /**
   * Get Echo Server OAuth2 credentials
   *
   * @return map with clientId, clientSecret or null if not configured
   */
  public Map<String, String> getOAuth2Credentials() {
    String clientId = getSecureValue("oauth2.client.id");
    if (clientId == null) {
      return null;
    }

    Map<String, String> credentials = new HashMap<>();
    credentials.put("clientId", clientId);
    credentials.put("clientSecret", getSecureValue("oauth2.client.secret"));
    return credentials;
  }

  /**
   * Store device registration information
   *
   * @param deviceId device ID from Echo Server
   * @param deviceName device name
   * @param deviceIdentifier device UUID
   * @param registeredAt registration timestamp
   */
  public void setDeviceRegistration(
      Long deviceId,
      String deviceName,
      String deviceIdentifier,
      java.time.LocalDateTime registeredAt) {
    setSecureValue("device.id", String.valueOf(deviceId));
    setSecureValue("device.name", deviceName);
    setSecureValue("device.identifier", deviceIdentifier);
    setSecureValue("device.registered.at", registeredAt.toString());
    log.info("Device registration info stored securely: deviceId={}", deviceId);
  }

  /**
   * Get device registration information
   *
   * @return map with deviceId, deviceName, deviceIdentifier, registeredAt or null if not registered
   */
  public Map<String, String> getDeviceRegistration() {
    String deviceId = getSecureValue("device.id");
    if (deviceId == null) {
      return null;
    }

    Map<String, String> registration = new HashMap<>();
    registration.put("deviceId", deviceId);
    registration.put("deviceName", getSecureValue("device.name"));
    registration.put("deviceIdentifier", getSecureValue("device.identifier"));
    registration.put("registeredAt", getSecureValue("device.registered.at"));
    return registration;
  }

  /**
   * Check if device is registered
   *
   * @return true if device registration exists
   */
  public boolean isDeviceRegistered() {
    return hasKey("device.id");
  }

  /**
   * Remove device registration information
   *
   * <p>Used for re-registration scenarios.
   */
  public void removeDeviceRegistration() {
    removeKey("device.id");
    removeKey("device.name");
    removeKey("device.identifier");
    removeKey("device.registered.at");
    log.info("Device registration info removed");
  }
}
