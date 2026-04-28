package com.ifonly.museagent.service;

import com.ifonly.museagent.client.EchoServerClient;
import com.ifonly.museagent.dto.DeviceRegistrationRequest;
import com.ifonly.museagent.dto.DeviceResponse;
import com.ifonly.museagent.dto.DeviceStatusInfo;
import com.ifonly.museagent.exception.DeviceRegistrationException;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Device Registration Service
 *
 * <p>Manages device registration with Echo Server and stores registration information securely.
 *
 * @author if-only
 * @version 0.1.0
 */
@Service
@Slf4j
public class DeviceRegistrationService {

  private final EchoServerClient echoServerClient;
  private final SecureConfigService secureConfigService;
  private final String appVersion;

  @Autowired
  public DeviceRegistrationService(
      EchoServerClient echoServerClient,
      SecureConfigService secureConfigService,
      @Value("${spring.application.name:muse-agent}") String appName) {
    this.echoServerClient = echoServerClient;
    this.secureConfigService = secureConfigService;
    this.appVersion = "0.1.0"; // TODO: Read from build configuration
  }

  /**
   * Register device with Echo Server
   *
   * @param deviceName user-provided device name
   * @return device response from Echo Server
   * @throws DeviceRegistrationException on registration failure
   */
  public DeviceResponse registerDevice(String deviceName) {
    log.info("Starting device registration: deviceName={}", deviceName);

    // Generate UUID for device identifier
    String deviceIdentifier = UUID.randomUUID().toString();

    // Detect OS information
    String osInfo = detectOsInfo();

    // Create registration request
    DeviceRegistrationRequest request = new DeviceRegistrationRequest();
    request.setDeviceIdentifier(deviceIdentifier);
    request.setDeviceName(deviceName);
    request.setDeviceType("AGENT");
    request.setOsInfo(osInfo);
    request.setAppVersion(appVersion);

    // Register with Echo Server
    DeviceResponse response = echoServerClient.registerDevice(request);

    // Store registration info securely
    secureConfigService.setDeviceRegistration(
        response.getId(),
        response.getDeviceName(),
        response.getDeviceIdentifier(),
        LocalDateTime.now());

    log.info(
        "Device registered successfully: deviceId={}, name={}",
        response.getId(),
        response.getDeviceName());

    return response;
  }

  /**
   * Get current device registration status
   *
   * @return device status information
   */
  public DeviceStatusInfo getDeviceStatus() {
    if (!secureConfigService.isDeviceRegistered()) {
      return DeviceStatusInfo.builder().registered(false).build();
    }

    Map<String, String> registration = secureConfigService.getDeviceRegistration();
    if (registration == null) {
      return DeviceStatusInfo.builder().registered(false).build();
    }

    return DeviceStatusInfo.builder()
        .registered(true)
        .deviceId(Long.parseLong(registration.get("deviceId")))
        .deviceName(registration.get("deviceName"))
        .deviceIdentifier(registration.get("deviceIdentifier"))
        .registeredAt(LocalDateTime.parse(registration.get("registeredAt")))
        .osInfo(detectOsInfo())
        .appVersion(appVersion)
        .build();
  }

  /**
   * Sync device registration with Echo Server on startup
   *
   * <p>If local registration exists, sends upsert request to Echo Server using the stored
   * identifier. Echo Server will update if device exists or create if it doesn't (e.g., after
   * in-memory DB reset). Updates local storage with the server response.
   *
   * @return true if sync was performed, false if no local registration exists
   */
  public boolean syncDeviceRegistration() {
    if (!secureConfigService.isDeviceRegistered()) {
      log.info("No local device registration found - skipping sync");
      return false;
    }

    Map<String, String> registration = secureConfigService.getDeviceRegistration();
    if (registration == null) {
      log.warn("Device registration data is corrupted - clearing local registration");
      secureConfigService.removeDeviceRegistration();
      return false;
    }

    String deviceIdentifier = registration.get("deviceIdentifier");
    String deviceName = registration.get("deviceName");
    log.info(
        "Syncing device registration with Echo Server: identifier={}, name={}",
        deviceIdentifier,
        deviceName);

    try {
      // Reuse stored identifier - Echo Server upserts (update if exists, create if not)
      DeviceRegistrationRequest request = new DeviceRegistrationRequest();
      request.setDeviceIdentifier(deviceIdentifier);
      request.setDeviceName(deviceName);
      request.setDeviceType("AGENT");
      request.setOsInfo(detectOsInfo());
      request.setAppVersion(appVersion);

      DeviceResponse response = echoServerClient.registerDevice(request);

      // Update local storage with server response (device ID may have changed)
      secureConfigService.setDeviceRegistration(
          response.getId(),
          response.getDeviceName(),
          response.getDeviceIdentifier(),
          LocalDateTime.now());

      log.info(
          "Device registration synced successfully: deviceId={}, name={}",
          response.getId(),
          response.getDeviceName());
      return true;
    } catch (Exception e) {
      log.warn("Device registration sync failed - will retry on next startup: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Re-register device (remove old registration and create new one)
   *
   * @param deviceName user-provided device name
   * @return device response from Echo Server
   * @throws DeviceRegistrationException on registration failure
   */
  public DeviceResponse reregisterDevice(String deviceName) {
    log.info("Re-registering device: deviceName={}", deviceName);

    // Unregister old device on Echo Server first, then clear local registration.
    unregisterDevice();

    // Register new device
    return registerDevice(deviceName);
  }

  /**
   * Unregister current device from Echo Server and clear local registration info.
   *
   * @return true if a registered device was removed, false if no local registration existed
   */
  public boolean unregisterDevice() {
    if (!secureConfigService.isDeviceRegistered()) {
      log.info("No local device registration found - skipping unregistration");
      return false;
    }

    Map<String, String> registration = secureConfigService.getDeviceRegistration();
    if (registration == null || registration.get("deviceId") == null) {
      log.warn("Device registration data is corrupted - clearing local registration");
      secureConfigService.removeDeviceRegistration();
      return false;
    }

    Long deviceId;
    try {
      deviceId = Long.parseLong(registration.get("deviceId"));
    } catch (NumberFormatException e) {
      secureConfigService.removeDeviceRegistration();
      throw new DeviceRegistrationException("로컬 장비 등록 정보(deviceId)가 올바르지 않습니다.", e);
    }

    String deviceName = registration.get("deviceName");
    String deviceIdentifier = registration.get("deviceIdentifier");

    echoServerClient.unregisterDevice(deviceId);
    secureConfigService.removeDeviceRegistration();
    log.info(
        "Device unregistered successfully: deviceId={}, name={}, identifier={}",
        deviceId,
        deviceName,
        deviceIdentifier);
    return true;
  }

  /**
   * Detect operating system information
   *
   * @return OS information string (max 100 characters)
   */
  private String detectOsInfo() {
    String osName = System.getProperty("os.name");
    String osVersion = System.getProperty("os.version");
    String osArch = System.getProperty("os.arch");
    String osInfo = String.format("%s %s (%s)", osName, osVersion, osArch);

    // Limit to 100 characters to match Echo Server validation
    if (osInfo.length() > 100) {
      osInfo = osInfo.substring(0, 97) + "...";
    }

    return osInfo;
  }
}
