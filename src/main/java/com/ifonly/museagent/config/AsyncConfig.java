package com.ifonly.museagent.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async Configuration
 *
 * <p>Enables asynchronous method execution in Spring using @Async annotation. Configures a thread
 * pool for handling async tasks such as startup connection testing.
 *
 * @author if-only
 * @version 0.1.0
 */
@Configuration
@EnableAsync
public class AsyncConfig {

  /**
   * Configure task executor for async operations
   *
   * @return configured executor
   */
  @Bean(name = "taskExecutor")
  public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("muse-async-");
    executor.initialize();
    return executor;
  }
}
