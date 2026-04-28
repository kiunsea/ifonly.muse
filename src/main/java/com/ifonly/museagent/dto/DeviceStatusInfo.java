package com.ifonly.museagent.dto;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

/**
 * Device Status Information DTO
 *
 * <p>Contains device registration status and details for UI display.
 *
 * @author if-only
 * @version 0.1.0
 */
@Data
@Builder
public class DeviceStatusInfo {

  /** Whether the device is registered */
  private boolean registered;

  /** Device ID (null if not registered) */
  private Long deviceId;

  /** Device name (null if not registered) */
  private String deviceName;

  /** Device identifier UUID (null if not registered) */
  private String deviceIdentifier;

  /** Registration timestamp (null if not registered) */
  private LocalDateTime registeredAt;

  /** Operating system information (null if not registered) */
  private String osInfo;

  /** Application version (null if not registered) */
  private String appVersion;
}
