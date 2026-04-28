package com.ifonly.museagent.service;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.dto.AliveStatusResponse;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Connection Test Service
 *
 * <p>Manages connection testing to Echo Server and stores test results. Provides automatic startup
 * testing and manual test execution capabilities.
 *
 * @author if-only
 * @version 0.4.0
 */
@Service
@Slf4j
public class ConnectionTestService {

  private final EchoServerClient echoServerClient;
  private final DeviceRegistrationService deviceRegistrationService;

  // Thread-safe result storage
  private volatile ConnectionTestResult startupTestResult;
  private volatile boolean startupTestCompleted = false;

  @Autowired
  public ConnectionTestService(
      EchoServerClient echoServerClient, DeviceRegistrationService deviceRegistrationService) {
    this.echoServerClient = echoServerClient;
    this.deviceRegistrationService = deviceRegistrationService;
  }

  /**
   * Run connection test and return result
   *
   * @param testSource source of the test (e.g., "STARTUP", "MANUAL", "SCHEDULED")
   * @return connection test result
   */
  public ConnectionTestResult runConnectionTest(String testSource) {
    log.debug("Running connection test from source: {}", testSource);
    ConnectionTestResult.ConnectionTestResultBuilder resultBuilder = ConnectionTestResult.builder();
    resultBuilder.timestamp(LocalDateTime.now().toString());
    resultBuilder.testSource(testSource);

    // Test: Alive status
    AliveStatusTestResult aliveStatusTest = testAliveStatus();
    resultBuilder.aliveStatusTest(aliveStatusTest);

    // Overall status
    resultBuilder.overallSuccess(aliveStatusTest.isSuccess());
    resultBuilder.message(
        aliveStatusTest.isSuccess() ? "Connection test passed" : "Connection test failed");

    return resultBuilder.build();
  }

  /**
   * Test alive status query
   *
   * @return alive status test result
   */
  private AliveStatusTestResult testAliveStatus() {
    long startTime = System.currentTimeMillis();

    try {
      AliveStatusResponse aliveStatus = echoServerClient.getAliveStatus();
      long duration = System.currentTimeMillis() - startTime;

      return AliveStatusTestResult.builder()
          .success(true)
          .responseTime(duration + "ms")
          .status(aliveStatus.getStatus())
          .build();
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      log.warn("Alive status test failed: {}", e.getMessage());

      return AliveStatusTestResult.builder()
          .success(false)
          .responseTime(duration + "ms")
          .error(e.getMessage())
          .build();
    }
  }

  /**
   * Run startup connection test asynchronously
   *
   * <p>This method runs in background and does not block application startup. Includes a delay to
   * allow Echo Server to be ready.
   */
  @Async
  public void runStartupTest() {
    log.info("Starting startup connection test...");

    try {
      // Add delay to ensure Echo Server is ready
      Thread.sleep(2000);

      ConnectionTestResult result = runConnectionTest("STARTUP");
      this.startupTestResult = result;
      this.startupTestCompleted = true;

      if (result.isOverallSuccess()) {
        log.info(
            "✓ Startup connection test completed successfully [AliveStatus: {}ms]",
            result.getAliveStatusTest().getResponseTime());

        // Sync device registration after successful connection
        try {
          boolean synced = deviceRegistrationService.syncDeviceRegistration();
          if (synced) {
            log.info("✓ Device registration synced with Echo Server");
          }
        } catch (Exception e) {
          log.warn("Device registration sync failed: {}", e.getMessage());
        }
      } else {
        log.warn("✗ Startup connection test failed: {}", result.getMessage());
      }
    } catch (InterruptedException e) {
      log.error("✗ Startup connection test interrupted", e);
      Thread.currentThread().interrupt();
      this.startupTestResult = createErrorResult(e);
      this.startupTestCompleted = true;
    } catch (Exception e) {
      log.error("✗ Startup connection test failed with exception", e);
      this.startupTestResult = createErrorResult(e);
      this.startupTestCompleted = true;
    }
  }

  /**
   * Create error result when test fails completely
   *
   * @param e exception
   * @return error connection test result
   */
  private ConnectionTestResult createErrorResult(Exception e) {
    return ConnectionTestResult.builder()
        .timestamp(LocalDateTime.now().toString())
        .testSource("STARTUP")
        .aliveStatusTest(
            AliveStatusTestResult.builder()
                .success(false)
                .responseTime("0ms")
                .error(e.getMessage())
                .build())
        .overallSuccess(false)
        .message("Startup test failed: " + e.getMessage())
        .build();
  }

  /**
   * Get startup test result (thread-safe)
   *
   * @return startup test result, or null if not available
   */
  public synchronized ConnectionTestResult getStartupTestResult() {
    return startupTestResult;
  }

  /**
   * Check if startup test has completed
   *
   * @return true if completed, false otherwise
   */
  public boolean isStartupTestCompleted() {
    return startupTestCompleted;
  }

  /** Connection test result DTO */
  @Data
  @Builder
  public static class ConnectionTestResult {
    private String timestamp;
    private String testSource;
    private AliveStatusTestResult aliveStatusTest;
    private boolean overallSuccess;
    private String message;
  }

  /** Alive status test result DTO */
  @Data
  @Builder
  public static class AliveStatusTestResult {
    private boolean success;
    private String responseTime;
    private String status;
    private String error;
  }
}
