package com.ifonly.museagent.listener;

import com.ifonly.museagent.service.ConnectionTestService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Connection Test Listener
 *
 * <p>Listens for application ready event and triggers automatic connection testing to Echo Server.
 * This ensures that connection status is verified immediately after application startup.
 *
 * @author if-only
 * @version 0.1.0
 */
@Component
@Slf4j
public class ConnectionTestListener {

  private final ConnectionTestService connectionTestService;

  @Autowired
  public ConnectionTestListener(ConnectionTestService connectionTestService) {
    this.connectionTestService = connectionTestService;
  }

  /**
   * Run connection test when application is fully ready
   *
   * <p>Uses ApplicationReadyEvent (not ApplicationStartedEvent) to ensure all beans are fully
   * initialized and the application is ready to serve requests.
   *
   * <p>The test runs asynchronously to avoid blocking application startup.
   */
  @EventListener(ApplicationReadyEvent.class)
  public void runStartupConnectionTest() {
    log.info("Application ready - triggering startup connection test");
    connectionTestService.runStartupTest();
  }
}
