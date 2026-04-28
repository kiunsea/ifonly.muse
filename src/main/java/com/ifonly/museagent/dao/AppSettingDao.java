package com.ifonly.museagent.dao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * App Setting DAO
 *
 * <p>Data access layer for app_setting table using JdbcTemplate.
 *
 * @author if-only
 * @version 0.18.0
 */
@Repository
@Slf4j
public class AppSettingDao {

  private final JdbcTemplate jdbcTemplate;

  private static final DateTimeFormatter SQLITE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  @Autowired
  public AppSettingDao(JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = jdbcTemplate;
  }

  public Optional<String> getValue(String key) {
    try {
      String value =
          jdbcTemplate.queryForObject(
              "SELECT setting_value FROM app_setting WHERE setting_key = ?", String.class, key);
      return Optional.ofNullable(value);
    } catch (EmptyResultDataAccessException e) {
      return Optional.empty();
    }
  }

  public void setValue(String key, String value, String description) {
    String now = LocalDateTime.now().format(SQLITE_FORMATTER);
    int updated =
        jdbcTemplate.update(
            "UPDATE app_setting SET setting_value = ?, description = ?, updated_at = ? WHERE setting_key = ?",
            value,
            description,
            now,
            key);
    if (updated == 0) {
      jdbcTemplate.update(
          "INSERT INTO app_setting (setting_key, setting_value, description, updated_at) VALUES (?, ?, ?, ?)",
          key,
          value,
          description,
          now);
    }
    log.debug("App setting saved: key={}, value={}", key, value);
  }

  public void setValue(String key, String value) {
    setValue(key, value, null);
  }
}
