package com.ifonly.museagent.config;

import com.ifonly.museagent.util.EncryptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Encryption configuration for sensitive data
 *
 * <p>Configures AES-256-GCM encryption for database fields and configuration values.
 *
 * @author if-only
 * @version 0.1.0
 */
@Configuration
@Slf4j
public class EncryptionConfig {

  @Value("${app.encryption.key:}")
  private String encryptionKey;

  /**
   * Create EncryptionUtil bean
   *
   * <p>If no key is provided, generates a new one and logs it (for development only).
   *
   * @return EncryptionUtil instance
   */
  @Bean
  public EncryptionUtil encryptionUtil() {
    if (encryptionKey == null || encryptionKey.isEmpty()) {
      log.warn("No encryption key provided. Generating a new key for this session.");
      log.warn("For production, set app.encryption.key in application.yml or environment variable");
      encryptionKey = EncryptionUtil.generateKey();
      log.info("Generated encryption key (save this for production): {}", encryptionKey);
    }

    EncryptionUtil util = new EncryptionUtil(encryptionKey);
    log.info("EncryptionUtil configured with AES-256-GCM");
    return util;
  }
}
