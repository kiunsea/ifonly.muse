package com.ifonly.museagent.converter;

import com.ifonly.museagent.util.EncryptionUtil;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA AttributeConverter for transparent encryption/decryption of String fields
 *
 * <p>Use @Convert(converter = EncryptedStringConverter.class) on entity fields to automatically
 * encrypt when saving and decrypt when loading.
 *
 * @author if-only
 * @version 0.1.0
 */
@Converter
@Component
@Slf4j
public class EncryptedStringConverter implements AttributeConverter<String, String> {

  private static EncryptionUtil encryptionUtil;

  @Autowired
  public void setEncryptionUtil(EncryptionUtil util) {
    EncryptedStringConverter.encryptionUtil = util;
    log.debug("EncryptedStringConverter initialized with EncryptionUtil");
  }

  @Override
  public String convertToDatabaseColumn(String attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return attribute;
    }

    if (encryptionUtil == null) {
      log.warn("EncryptionUtil not initialized, storing plaintext");
      return attribute;
    }

    try {
      String encrypted = encryptionUtil.encrypt(attribute);
      log.trace("Encrypted field for database storage");
      return encrypted;
    } catch (Exception e) {
      log.error("Failed to encrypt field for database", e);
      throw new RuntimeException("Encryption failed", e);
    }
  }

  @Override
  public String convertToEntityAttribute(String dbData) {
    if (dbData == null || dbData.isEmpty()) {
      return dbData;
    }

    if (encryptionUtil == null) {
      log.warn("EncryptionUtil not initialized, returning raw data");
      return dbData;
    }

    try {
      // Check if data is encrypted
      if (!EncryptionUtil.isEncrypted(dbData)) {
        log.trace("Data appears to be plaintext, returning as-is");
        return dbData;
      }

      String decrypted = encryptionUtil.decrypt(dbData);
      log.trace("Decrypted field from database");
      return decrypted;
    } catch (Exception e) {
      log.error("Failed to decrypt field from database", e);
      // Return raw data if decryption fails (might be plaintext)
      return dbData;
    }
  }
}
