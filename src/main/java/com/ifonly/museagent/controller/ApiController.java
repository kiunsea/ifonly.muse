package com.ifonly.museagent.controller;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.config.DynamicReactiveClientRegistrationRepository;
import com.ifonly.museagent.config.EchoServerProperties;
import com.ifonly.museagent.dto.AliveHistoryResponse;
import com.ifonly.museagent.dto.AliveStatusResponse;
import com.ifonly.museagent.dto.CleanupPathDto;
import com.ifonly.museagent.dto.DeviceResponse;
import com.ifonly.museagent.dto.DeviceStatusInfo;
import com.ifonly.museagent.service.AppSettingService;
import com.ifonly.museagent.service.CleanupPathService;
import com.ifonly.museagent.service.ConnectionTestService;
import com.ifonly.museagent.service.ConnectionTestService.AliveStatusTestResult;
import com.ifonly.museagent.service.ConnectionTestService.ConnectionTestResult;
import com.ifonly.museagent.service.DeviceRegistrationService;
import com.ifonly.museagent.service.EchoCredentialUpdater;
import com.ifonly.museagent.service.FileCleanupService;
import com.ifonly.museagent.service.ScheduleExecutorService;
import com.ifonly.museagent.service.SecureConfigService;
import com.ifonly.museagent.service.TaskExecutionHistoryService;
import com.ifonly.museagent.service.TrashService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

/**
 * REST API Controller for communication testing
 *
 * @author if-only
 * @version 0.1.0
 */
@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

  private static final String KEY_BOUND_USERNAME = "echo.server.bound.username";
  private static final String KEY_BOUND_CLIENT_ID = "echo.server.bound.client.id";

  private final EchoServerClient echoServerClient;
  private final ConnectionTestService connectionTestService;
  private final DeviceRegistrationService deviceRegistrationService;
  private final CleanupPathService cleanupPathService;
  private final FileCleanupService fileCleanupService;
  private final AppSettingService appSettingService;
  private final SecureConfigService secureConfigService;
  private final EchoServerProperties echoServerProperties;
  private final TaskExecutionHistoryService taskExecutionHistoryService;
  private final TrashService trashService;
  private final ScheduleExecutorService scheduleExecutorService;
  private final EchoCredentialUpdater credentialUpdater;

  @Autowired
  public ApiController(
      EchoServerClient echoServerClient,
      ConnectionTestService connectionTestService,
      DeviceRegistrationService deviceRegistrationService,
      CleanupPathService cleanupPathService,
      FileCleanupService fileCleanupService,
      AppSettingService appSettingService,
      SecureConfigService secureConfigService,
      EchoServerProperties echoServerProperties,
      TaskExecutionHistoryService taskExecutionHistoryService,
      TrashService trashService,
      ScheduleExecutorService scheduleExecutorService,
      EchoCredentialUpdater credentialUpdater) {
    this.echoServerClient = echoServerClient;
    this.connectionTestService = connectionTestService;
    this.deviceRegistrationService = deviceRegistrationService;
    this.cleanupPathService = cleanupPathService;
    this.fileCleanupService = fileCleanupService;
    this.appSettingService = appSettingService;
    this.secureConfigService = secureConfigService;
    this.echoServerProperties = echoServerProperties;
    this.taskExecutionHistoryService = taskExecutionHistoryService;
    this.trashService = trashService;
    this.scheduleExecutorService = scheduleExecutorService;
    this.credentialUpdater = credentialUpdater;
  }

  /**
   * Connection test (alive status)
   *
   * @return test results
   */
  @GetMapping("/test/connection")
  public ResponseEntity<Map<String, Object>> testConnection() {
    Map<String, Object> result = new HashMap<>();
    result.put("timestamp", LocalDateTime.now().toString());

    // Test: Alive status
    Map<String, Object> aliveStatusTest = new HashMap<>();
    long startTime = System.currentTimeMillis();
    try {
      AliveStatusResponse aliveStatus = echoServerClient.getAliveStatus();
      aliveStatusTest.put("success", true);
      aliveStatusTest.put("responseTime", (System.currentTimeMillis() - startTime) + "ms");
      aliveStatusTest.put("status", aliveStatus.getStatus());
    } catch (Exception e) {
      aliveStatusTest.put("success", false);
      aliveStatusTest.put("responseTime", (System.currentTimeMillis() - startTime) + "ms");
      aliveStatusTest.put("error", e.getMessage());
    }
    result.put("aliveStatusTest", aliveStatusTest);

    boolean allSuccess = (boolean) aliveStatusTest.get("success");
    result.put("overallSuccess", allSuccess);
    result.put("message", allSuccess ? "All connection tests passed" : "Connection test failed");

    return ResponseEntity.ok(result);
  }

  /**
   * Get startup connection test result
   *
   * <p>Returns the cached result from the automatic connection test that ran during application
   * startup. If the test is still in progress, returns a status message.
   *
   * @return startup test result or in-progress message
   */
  @GetMapping("/test/startup-result")
  public ResponseEntity<Map<String, Object>> getStartupTestResult() {
    // Check if test has completed
    if (!connectionTestService.isStartupTestCompleted()) {
      Map<String, Object> result = new HashMap<>();
      result.put("completed", false);
      result.put("message", "Startup connection test in progress...");
      result.put("timestamp", LocalDateTime.now().toString());

      log.debug("Startup test not yet completed");
      return ResponseEntity.ok(result);
    }

    // Get completed result
    ConnectionTestResult testResult = connectionTestService.getStartupTestResult();
    if (testResult == null) {
      Map<String, Object> errorResult = new HashMap<>();
      errorResult.put("completed", false);
      errorResult.put("message", "Startup test result not available");
      errorResult.put("timestamp", LocalDateTime.now().toString());

      log.warn("Startup test result is null");
      return ResponseEntity.ok(errorResult);
    }

    // Convert to response format
    Map<String, Object> response = convertTestResultToMap(testResult);
    response.put("completed", true);

    log.debug("Startup test result retrieved: overallSuccess={}", testResult.isOverallSuccess());
    return ResponseEntity.ok(response);
  }

  /**
   * Convert ConnectionTestResult to Map format
   *
   * @param result connection test result
   * @return map representation
   */
  private Map<String, Object> convertTestResultToMap(ConnectionTestResult result) {
    Map<String, Object> map = new HashMap<>();
    map.put("timestamp", result.getTimestamp());
    map.put("testSource", result.getTestSource());
    map.put("overallSuccess", result.isOverallSuccess());
    map.put("message", result.getMessage());

    AliveStatusTestResult aliveStatusTest = result.getAliveStatusTest();
    if (aliveStatusTest != null) {
      Map<String, Object> aliveStatusMap = new HashMap<>();
      aliveStatusMap.put("success", aliveStatusTest.isSuccess());
      aliveStatusMap.put("responseTime", aliveStatusTest.getResponseTime());
      if (aliveStatusTest.getStatus() != null) {
        aliveStatusMap.put("status", aliveStatusTest.getStatus());
      }
      if (aliveStatusTest.getError() != null) {
        aliveStatusMap.put("error", aliveStatusTest.getError());
      }
      map.put("aliveStatusTest", aliveStatusMap);
    }

    return map;
  }

  /**
   * Test alive status query from Echo Server
   *
   * @return test result
   */
  @GetMapping("/test/alive-status")
  public ResponseEntity<Map<String, Object>> testAliveStatus() {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      AliveStatusResponse aliveStatus = echoServerClient.getAliveStatus();
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("message", "Alive status retrieved successfully");
      result.put("responseTime", duration + "ms");
      result.put("status", aliveStatus.getStatus());
      result.put("lastConfirmAt", aliveStatus.getLastConfirmAt());
      result.put("lastSource", aliveStatus.getLastSource());
      result.put("ttlValue", aliveStatus.getTtlValue());
      result.put("ttlUnit", aliveStatus.getTtlUnit());
      result.put("warnDays", aliveStatus.getWarnDays());
      result.put("nextDeadline", aliveStatus.getNextDeadline());
      result.put("statusMessage", aliveStatus.getMessage());

      log.info("Alive status retrieved in {}ms: status={}", duration, aliveStatus.getStatus());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Alive status query failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Test alive history query from Echo Server
   *
   * @param limit result limit
   * @return test result
   */
  @GetMapping("/test/alive-history")
  public ResponseEntity<Map<String, Object>> testAliveHistory(
      @RequestParam(defaultValue = "10") int limit) {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      AliveHistoryResponse history = echoServerClient.getAliveHistory(limit, 0);
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("message", "Alive history retrieved successfully");
      result.put("responseTime", duration + "ms");
      result.put("total", history.getTotal());
      result.put("events", history.getEvents());

      log.info("Alive history retrieved in {}ms", duration);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Alive history query failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Register device with Echo Server
   *
   * @param requestBody request with deviceName
   * @return registration result
   */
  @PostMapping("/device/register")
  public ResponseEntity<Map<String, Object>> registerDevice(
      @RequestBody Map<String, String> requestBody) {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      String deviceName = requestBody.get("deviceName");
      if (deviceName == null || deviceName.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "Device name is required");
        result.put("responseTime", "0ms");
        return ResponseEntity.ok(result);
      }

      DeviceResponse response = deviceRegistrationService.registerDevice(deviceName);
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("message", "Device registered successfully");
      result.put("responseTime", duration + "ms");
      result.put("data", convertDeviceResponseToMap(response));

      log.info("Device registered via API in {}ms: deviceId={}", duration, response.getId());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Device registration via API failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Get device registration status
   *
   * @return device status
   */
  @GetMapping("/device/status")
  public ResponseEntity<Map<String, Object>> getDeviceStatus() {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      DeviceStatusInfo status = deviceRegistrationService.getDeviceStatus();
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("message", "Device status retrieved successfully");
      result.put("responseTime", duration + "ms");
      result.put("data", convertDeviceStatusToMap(status));

      log.debug("Device status retrieved in {}ms", duration);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Get device status failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Re-register device with Echo Server
   *
   * @param requestBody request with deviceName
   * @return registration result
   */
  @PostMapping("/device/reregister")
  public ResponseEntity<Map<String, Object>> reregisterDevice(
      @RequestBody Map<String, String> requestBody) {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      String deviceName = requestBody.get("deviceName");
      if (deviceName == null || deviceName.trim().isEmpty()) {
        result.put("success", false);
        result.put("message", "Device name is required");
        result.put("responseTime", "0ms");
        return ResponseEntity.ok(result);
      }

      DeviceResponse response = deviceRegistrationService.reregisterDevice(deviceName);
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put("message", "Device re-registered successfully");
      result.put("responseTime", duration + "ms");
      result.put("data", convertDeviceResponseToMap(response));

      log.info("Device re-registered via API in {}ms: deviceId={}", duration, response.getId());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Device re-registration via API failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Unregister current device from Echo Server
   *
   * @return unregistration result
   */
  @PostMapping("/device/unregister")
  public ResponseEntity<Map<String, Object>> unregisterDevice() {
    Map<String, Object> result = new HashMap<>();
    long startTime = System.currentTimeMillis();

    try {
      boolean removed = deviceRegistrationService.unregisterDevice();
      long duration = System.currentTimeMillis() - startTime;

      result.put("success", true);
      result.put(
          "message", removed ? "Device unregistered successfully" : "No registered device found");
      result.put("responseTime", duration + "ms");
      result.put("unregistered", removed);

      log.info("Device unregistered via API in {}ms: removed={}", duration, removed);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      result.put("success", false);
      result.put("message", e.getMessage());
      result.put("responseTime", duration + "ms");
      result.put("error", e.getClass().getSimpleName());

      log.error("Device unregistration via API failed: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Convert DeviceResponse to Map format
   *
   * @param response device response
   * @return map representation
   */
  private Map<String, Object> convertDeviceResponseToMap(DeviceResponse response) {
    Map<String, Object> map = new HashMap<>();
    map.put("id", response.getId());
    map.put("deviceIdentifier", response.getDeviceIdentifier());
    map.put("deviceName", response.getDeviceName());
    map.put("deviceType", response.getDeviceType());
    map.put("osInfo", response.getOsInfo());
    map.put("appVersion", response.getAppVersion());
    map.put("enabled", response.getEnabled());
    if (response.getLastSeenAt() != null) {
      map.put("lastSeenAt", response.getLastSeenAt().toString());
    }
    if (response.getCreatedAt() != null) {
      map.put("createdAt", response.getCreatedAt().toString());
    }
    if (response.getUpdatedAt() != null) {
      map.put("updatedAt", response.getUpdatedAt().toString());
    }
    return map;
  }

  /**
   * Convert DeviceStatusInfo to Map format
   *
   * @param status device status info
   * @return map representation
   */
  private Map<String, Object> convertDeviceStatusToMap(DeviceStatusInfo status) {
    Map<String, Object> map = new HashMap<>();
    map.put("registered", status.isRegistered());
    if (status.isRegistered()) {
      map.put("deviceId", status.getDeviceId());
      map.put("deviceName", status.getDeviceName());
      map.put("deviceIdentifier", status.getDeviceIdentifier());
      if (status.getRegisteredAt() != null) {
        map.put("registeredAt", status.getRegisteredAt().toString());
      }
      map.put("osInfo", status.getOsInfo());
      map.put("appVersion", status.getAppVersion());
    }
    return map;
  }

  // ========== Cleanup Path CRUD Endpoints ==========

  /**
   * Get all cleanup paths
   *
   * @return list of cleanup paths
   */
  @GetMapping("/cleanup-paths")
  public ResponseEntity<Map<String, Object>> getCleanupPaths() {
    Map<String, Object> result = new HashMap<>();
    try {
      List<CleanupPathDto> paths = cleanupPathService.getAllPaths();
      result.put("success", true);
      result.put("message", "Cleanup paths retrieved successfully");
      result.put("data", paths);
      result.put("count", paths.size());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to retrieve cleanup paths: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Add a new cleanup path
   *
   * @param requestBody request with path and description
   * @return created cleanup path
   */
  @PostMapping("/cleanup-paths")
  public ResponseEntity<Map<String, Object>> addCleanupPath(
      @RequestBody Map<String, String> requestBody) {
    Map<String, Object> result = new HashMap<>();
    try {
      String path = requestBody.get("path");
      String description = requestBody.get("description");

      CleanupPathDto saved = cleanupPathService.addPath(path, description);
      result.put("success", true);
      result.put("message", "Cleanup path added successfully");
      result.put("data", saved);

      log.info("Cleanup path added via API: id={}, path={}", saved.getId(), saved.getPath());
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to add cleanup path: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Update a cleanup path
   *
   * @param id path id
   * @param requestBody request with path, description, enabled
   * @return updated cleanup path
   */
  @PutMapping("/cleanup-paths/{id}")
  public ResponseEntity<Map<String, Object>> updateCleanupPath(
      @PathVariable Long id, @RequestBody Map<String, Object> requestBody) {
    Map<String, Object> result = new HashMap<>();
    try {
      String path = (String) requestBody.get("path");
      String description = (String) requestBody.get("description");
      boolean enabled =
          requestBody.containsKey("enabled") && Boolean.TRUE.equals(requestBody.get("enabled"));

      CleanupPathDto updated = cleanupPathService.updatePath(id, path, description, enabled);
      result.put("success", true);
      result.put("message", "Cleanup path updated successfully");
      result.put("data", updated);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to update cleanup path: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Delete a cleanup path
   *
   * @param id path id
   * @return deletion result
   */
  @DeleteMapping("/cleanup-paths/{id}")
  public ResponseEntity<Map<String, Object>> deleteCleanupPath(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      boolean deleted = cleanupPathService.removePath(id);
      result.put("success", deleted);
      result.put("message", deleted ? "Cleanup path deleted" : "Cleanup path not found");
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to delete cleanup path: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Toggle cleanup path enabled/disabled
   *
   * @param id path id
   * @return updated cleanup path
   */
  @PostMapping("/cleanup-paths/{id}/toggle")
  public ResponseEntity<Map<String, Object>> toggleCleanupPath(@PathVariable Long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      CleanupPathDto toggled = cleanupPathService.toggleEnabled(id);
      result.put("success", true);
      result.put("message", toggled.isEnabled() ? "Path enabled" : "Path disabled");
      result.put("data", toggled);
      return ResponseEntity.ok(result);
    } catch (IllegalArgumentException e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to toggle cleanup path: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Verify all enabled cleanup paths
   *
   * @return verification result
   */
  @PostMapping("/cleanup-paths/verify")
  public ResponseEntity<Map<String, Object>> verifyCleanupPaths() {
    Map<String, Object> result = new HashMap<>();
    try {
      List<String> paths = cleanupPathService.getEnabledPathStrings();
      var verification = fileCleanupService.verifyPaths(paths);

      result.put("success", true);
      result.put("message", "Verification completed");
      result.put("data", verification);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to verify cleanup paths: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Get recent task execution history.
   *
   * @param limit maximum number of history rows
   * @param taskGroup optional group filter
   * @return recent task execution history
   */
  @GetMapping("/task-executions/history")
  public ResponseEntity<Map<String, Object>> getTaskExecutionHistory(
      @RequestParam(defaultValue = "20") int limit,
      @RequestParam(required = false) String taskGroup,
      @RequestParam(required = false) String taskKey,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Boolean success,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate startDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate endDate,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    Map<String, Object> result = new HashMap<>();
    try {
      var histories =
          taskExecutionHistoryService.getRecent(
              limit, taskGroup, taskKey, status, success, startDate, endDate, sortBy, sortDir);
      result.put("success", true);
      result.put("message", "Task execution history retrieved successfully");
      result.put("data", histories);
      result.put("count", histories.size());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to retrieve task execution history: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Get alive-check scheduler settings
   *
   * @return current intervalMs and thresholdDays
   */
  @GetMapping("/settings/alive-check")
  public ResponseEntity<Map<String, Object>> getAliveCheckSettings() {
    Map<String, Object> result = new HashMap<>();
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("intervalMs", appSettingService.getAliveCheckIntervalMs());
      data.put("thresholdDays", appSettingService.getAliveCheckThresholdDays());
      result.put("success", true);
      result.put("data", data);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to get alive-check settings: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Update alive-check scheduler settings
   *
   * @param requestBody map containing intervalMs (long) and/or thresholdDays (int)
   * @return updated settings
   */
  @PutMapping("/settings/alive-check")
  public ResponseEntity<Map<String, Object>> updateAliveCheckSettings(
      @RequestBody Map<String, Object> requestBody) {
    Map<String, Object> result = new HashMap<>();
    try {
      if (requestBody.containsKey("intervalMs")) {
        long intervalMs = Long.parseLong(requestBody.get("intervalMs").toString());
        appSettingService.setAliveCheckIntervalMs(intervalMs);
      }
      if (requestBody.containsKey("thresholdDays")) {
        int thresholdDays = Integer.parseInt(requestBody.get("thresholdDays").toString());
        appSettingService.setAliveCheckThresholdDays(thresholdDays);
      }
      Map<String, Object> data = new HashMap<>();
      data.put("intervalMs", appSettingService.getAliveCheckIntervalMs());
      data.put("thresholdDays", appSettingService.getAliveCheckThresholdDays());
      result.put("success", true);
      result.put("data", data);
      log.info("Alive-check settings updated via API");
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to update alive-check settings: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Get cleanup trash settings */
  @GetMapping("/settings/cleanup-trash")
  public ResponseEntity<Map<String, Object>> getCleanupTrashSettings() {
    Map<String, Object> result = new HashMap<>();
    try {
      Map<String, Object> data = new HashMap<>();
      data.put("retentionDays", appSettingService.getCleanupTrashRetentionDays());
      data.put("rootPath", appSettingService.getCleanupTrashRootPath());
      data.put("purgeBatchSize", appSettingService.getCleanupTrashPurgeBatchSize());
      result.put("success", true);
      result.put("data", data);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Update cleanup trash settings */
  @PutMapping("/settings/cleanup-trash")
  public ResponseEntity<Map<String, Object>> updateCleanupTrashSettings(
      @RequestBody Map<String, Object> requestBody) {
    Map<String, Object> result = new HashMap<>();
    try {
      if (requestBody.containsKey("retentionDays")) {
        int retentionDays = Integer.parseInt(requestBody.get("retentionDays").toString());
        appSettingService.setCleanupTrashRetentionDays(retentionDays);
      }
      if (requestBody.containsKey("rootPath")) {
        String rootPath = String.valueOf(requestBody.get("rootPath"));
        appSettingService.setCleanupTrashRootPath(rootPath);
      }
      if (requestBody.containsKey("purgeBatchSize")) {
        int purgeBatchSize = Integer.parseInt(requestBody.get("purgeBatchSize").toString());
        appSettingService.setCleanupTrashPurgeBatchSize(purgeBatchSize);
      }

      Map<String, Object> data = new HashMap<>();
      data.put("retentionDays", appSettingService.getCleanupTrashRetentionDays());
      data.put("rootPath", appSettingService.getCleanupTrashRootPath());
      data.put("purgeBatchSize", appSettingService.getCleanupTrashPurgeBatchSize());
      result.put("success", true);
      result.put("data", data);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Get trash item list */
  @GetMapping("/trash/items")
  public ResponseEntity<Map<String, Object>> getTrashItems(
      @RequestParam(defaultValue = "50") int limit, @RequestParam(required = false) String status) {
    Map<String, Object> result = new HashMap<>();
    try {
      var items = trashService.getRecentItems(limit, status);
      result.put("success", true);
      result.put("data", items);
      result.put("count", items.size());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Get trash statistics */
  @GetMapping("/trash/stats")
  public ResponseEntity<Map<String, Object>> getTrashStats() {
    Map<String, Object> result = new HashMap<>();
    try {
      result.put("success", true);
      result.put("data", trashService.getStats());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Purge expired trash items now */
  @PostMapping("/trash/purge-now")
  public ResponseEntity<Map<String, Object>> purgeTrashNow() {
    Map<String, Object> result = new HashMap<>();
    try {
      var purgeResult = scheduleExecutorService.purgeExpiredTrash();
      result.put("success", true);
      result.put("message", "Trash purge completed");
      result.put("data", purgeResult);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /** Restore a trash item to its original path */
  @PostMapping("/trash/{id}/restore")
  public ResponseEntity<Map<String, Object>> restoreTrashItem(@PathVariable long id) {
    Map<String, Object> result = new HashMap<>();
    try {
      var restoreResult = scheduleExecutorService.restoreTrashItem(id);
      result.put("success", restoreResult.isSuccess());
      result.put(
          "message",
          restoreResult.isSuccess()
              ? "Trash item restored"
              : (restoreResult.getErrorMessage() == null
                  ? "Restore failed"
                  : restoreResult.getErrorMessage()));
      result.put("data", restoreResult);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  // ========== Echo Server Credential Settings ==========

  /**
   * Get current saved Echo Server credentials from SecureConfigService.
   *
   * @return saved credentials (secret is masked)
   */
  @GetMapping("/settings/echo-credentials")
  public ResponseEntity<Map<String, Object>> getEchoCredentials() {
    Map<String, Object> result = new HashMap<>();
    try {
      boolean hasCreds =
          secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_URL)
              || secureConfigService.hasKey(
                  DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID);
      result.put("saved", hasCreds);
      if (hasCreds) {
        result.put(
            "url",
            secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_URL)
                ? secureConfigService.getSecureValue(
                    DynamicReactiveClientRegistrationRepository.KEY_URL)
                : echoServerProperties.getUrl());
        result.put(
            "clientId",
            secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID)
                ? secureConfigService.getSecureValue(
                    DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID)
                : echoServerProperties.getClientId());
        boolean hasSecret =
            secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_SECRET);
        result.put("clientSecretSaved", hasSecret);
        result.put("boundUsername", secureConfigService.getSecureValue(KEY_BOUND_USERNAME));
        result.put("boundClientId", secureConfigService.getSecureValue(KEY_BOUND_CLIENT_ID));
      } else {
        result.put("url", echoServerProperties.getUrl());
        result.put("clientId", echoServerProperties.getClientId());
        result.put("clientSecretSaved", false);
        result.put("boundUsername", null);
        result.put("boundClientId", null);
      }
      result.put("success", true);
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to get echo credentials: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  /**
   * Test Echo Server credentials without saving.
   *
   * @param requestBody map with url, clientId, clientSecret
   * @return test result
   */
  @PostMapping("/settings/echo-credentials/test")
  public ResponseEntity<Map<String, Object>> testEchoCredentials(
      @RequestBody Map<String, String> requestBody) {
    Map<String, Object> result = new HashMap<>();
    String url = requestBody.get("url");
    String clientId = requestBody.get("clientId");
    String clientSecret = requestBody.get("clientSecret");

    if (url == null
        || url.isBlank()
        || clientId == null
        || clientId.isBlank()
        || clientSecret == null
        || clientSecret.isBlank()) {
      result.put("success", false);
      result.put("message", "url, clientId, clientSecret 모두 입력해주세요.");
      return ResponseEntity.badRequest().body(result);
    }

    url = normalizeBaseUrl(url);
    TokenTestResult testResult = testTokenIssue(url, clientId, clientSecret);
    result.put("success", testResult.success());
    result.put("message", testResult.message());
    result.put("responseTime", testResult.responseTimeMs() + "ms");
    result.put("boundUsername", testResult.username());
    result.put("boundClientId", testResult.clientId());
    return ResponseEntity.ok(result);
  }

  /**
   * Save Echo Server credentials and verify them by obtaining an OAuth2 token.
   *
   * <p>Credentials are encrypted and stored in SecureConfigService. The agent must be restarted for
   * the live WebClient to pick up the new credentials.
   *
   * @param requestBody map with url, clientId, clientSecret
   * @return save + test result
   */
  @PostMapping("/settings/echo-credentials")
  public ResponseEntity<Map<String, Object>> saveEchoCredentials(
      @RequestBody Map<String, String> requestBody) {
    Map<String, Object> result = new HashMap<>();
    try {
      String url = requestBody.get("url");
      String clientIdInput = requestBody.get("clientId");
      String clientSecretInput = requestBody.get("clientSecret");

      if (!StringUtils.hasText(url)) {
        result.put("success", false);
        result.put("message", "url은 필수 입력값입니다.");
        return ResponseEntity.badRequest().body(result);
      }

      boolean hasClientIdInput = StringUtils.hasText(clientIdInput);
      boolean hasClientSecretInput = StringUtils.hasText(clientSecretInput);

      if (hasClientIdInput ^ hasClientSecretInput) {
        result.put("success", false);
        result.put("message", "Client ID/Client Secret은 둘 다 입력하거나 둘 다 비워두세요.");
        return ResponseEntity.badRequest().body(result);
      }

      String currentClientId =
          secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID)
              ? secureConfigService.getSecureValue(
                  DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID)
              : echoServerProperties.getClientId();
      String currentClientSecret =
          secureConfigService.hasKey(DynamicReactiveClientRegistrationRepository.KEY_SECRET)
              ? secureConfigService.getSecureValue(
                  DynamicReactiveClientRegistrationRepository.KEY_SECRET)
              : echoServerProperties.getClientSecret();

      String clientId = hasClientIdInput ? clientIdInput : currentClientId;
      String clientSecret = hasClientSecretInput ? clientSecretInput : currentClientSecret;

      if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
        result.put("success", false);
        result.put("message", "Client ID/Client Secret이 없어 URL만 저장할 수 없습니다. 자격증명을 먼저 입력하세요.");
        return ResponseEntity.badRequest().body(result);
      }

      url = normalizeBaseUrl(url);
      TokenTestResult testResult = testTokenIssue(url, clientId, clientSecret);
      if (!testResult.success()) {
        result.put("success", false);
        result.put("message", testResult.message());
        result.put("responseTime", testResult.responseTimeMs() + "ms");
        return ResponseEntity.ok(result);
      }

      // Step 2: 검증 성공 → 암호화하여 저장
      secureConfigService.setSecureValue(DynamicReactiveClientRegistrationRepository.KEY_URL, url);
      secureConfigService.setSecureValue(
          DynamicReactiveClientRegistrationRepository.KEY_CLIENT_ID, clientId);
      secureConfigService.setSecureValue(
          DynamicReactiveClientRegistrationRepository.KEY_SECRET, clientSecret);
      // EchoServerProperties 즉시 동기화 (재시작 전에도 EchoServerClient가 올바른 URL 사용)
      echoServerProperties.setUrl(url);
      echoServerProperties.setClientId(clientId);
      echoServerProperties.setClientSecret(clientSecret);
      if (StringUtils.hasText(testResult.username())) {
        secureConfigService.setSecureValue(KEY_BOUND_USERNAME, testResult.username());
      }
      if (StringUtils.hasText(testResult.clientId())) {
        secureConfigService.setSecureValue(KEY_BOUND_CLIENT_ID, testResult.clientId());
      }

      // v1.7.0: 자격증명 캐시 즉시 무효화 → 재시작 없이 다음 echo 호출부터 새 자격증명 적용.
      credentialUpdater.onCredentialsChanged();

      log.info("Echo Server credentials saved: url={}, clientId={}", url, clientId);

      result.put("success", true);
      result.put("message", "설정이 저장되었고 해당 자격증명 발급 계정에 자동 연결되었습니다. 다음 echo 호출부터 즉시 적용됩니다.");
      result.put("responseTime", testResult.responseTimeMs() + "ms");
      result.put("boundUsername", testResult.username());
      result.put("boundClientId", testResult.clientId());
      return ResponseEntity.ok(result);
    } catch (Exception e) {
      result.put("success", false);
      result.put("message", e.getMessage());
      log.error("Failed to save echo credentials: {}", e.getMessage());
      return ResponseEntity.ok(result);
    }
  }

  private String normalizeBaseUrl(String url) {
    String normalized = url.trim().replaceAll("/+$", "");
    if (normalized.matches("(?i).*/web($|/.*)")) {
      normalized = normalized.replaceFirst("(?i)/web(/.*)?$", "");
    }
    return normalized;
  }

  private TokenTestResult testTokenIssue(String url, String clientId, String clientSecret) {
    long startTime = System.currentTimeMillis();
    try {
      String tokenUrl = url + "/oauth2/token";
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
      String credentials =
          Base64.getEncoder()
              .encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
      headers.set("Authorization", "Basic " + credentials);

      MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
      form.add("grant_type", "client_credentials");
      form.add("scope", "read write");

      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<Map> tokenResp =
          restTemplate.exchange(
              tokenUrl, HttpMethod.POST, new HttpEntity<>(form, headers), Map.class);

      if (!tokenResp.getStatusCode().is2xxSuccessful()
          || tokenResp.getBody() == null
          || tokenResp.getBody().get("access_token") == null) {
        return new TokenTestResult(
            false,
            "자격증명 검증 실패: access_token을 받지 못했습니다.",
            System.currentTimeMillis() - startTime,
            null,
            null);
      }

      String accessToken = tokenResp.getBody().get("access_token").toString();
      AuthContextResult contextResult = fetchAuthContext(url, accessToken);
      if (!contextResult.success()) {
        return new TokenTestResult(
            false,
            "연결 테스트 실패: 인증 컨텍스트를 확인할 수 없습니다.",
            System.currentTimeMillis() - startTime,
            null,
            null);
      }

      return new TokenTestResult(
          true,
          "연결 테스트 성공: OAuth2 토큰 발급 및 계정 연결이 확인되었습니다.",
          System.currentTimeMillis() - startTime,
          contextResult.username(),
          contextResult.clientId());
    } catch (Exception e) {
      log.warn("Echo credential test failed: {}", e.getMessage());
      return new TokenTestResult(
          false,
          "연결 테스트 실패: " + e.getMessage(),
          System.currentTimeMillis() - startTime,
          null,
          null);
    }
  }

  private AuthContextResult fetchAuthContext(String url, String accessToken) {
    try {
      String endpoint = normalizeBaseUrl(url) + "/api/auth/context";
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(accessToken);
      RestTemplate restTemplate = new RestTemplate();
      ResponseEntity<Map> response =
          restTemplate.exchange(endpoint, HttpMethod.GET, new HttpEntity<>(headers), Map.class);

      if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
        return new AuthContextResult(false, null, null);
      }

      Object success = response.getBody().get("success");
      if (!(success instanceof Boolean) || !((Boolean) success)) {
        return new AuthContextResult(false, null, null);
      }

      String username =
          response.getBody().get("username") != null
              ? response.getBody().get("username").toString()
              : null;
      String clientId =
          response.getBody().get("clientId") != null
              ? response.getBody().get("clientId").toString()
              : null;
      return new AuthContextResult(true, username, clientId);
    } catch (Exception e) {
      log.warn("Failed to fetch auth context: {}", e.getMessage());
      return new AuthContextResult(false, null, null);
    }
  }

  private record TokenTestResult(
      boolean success, String message, long responseTimeMs, String username, String clientId) {}

  private record AuthContextResult(boolean success, String username, String clientId) {}
}
