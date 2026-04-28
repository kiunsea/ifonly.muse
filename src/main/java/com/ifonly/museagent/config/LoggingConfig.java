package com.ifonly.museagent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Logging Configuration
 *
 * <p>Configures Log4j2 with SLF4J facade
 *
 * @author if-only
 * @version 0.1.1
 */
@Configuration
@Slf4j
public class LoggingConfig {

  /**
   * Logging initializer Bean
   *
   * @return initialization status
   */
  @Bean
  public Boolean loggingInitializer() {
    log.info("Log4j2 logging system initialized");
    return true;
  }
}
