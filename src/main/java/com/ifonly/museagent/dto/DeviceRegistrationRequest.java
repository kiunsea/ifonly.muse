package com.ifonly.museagent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Device Registration Request DTO
 *
 * <p>Request payload for registering a device with Echo Server.
 *
 * @author if-only
 * @version 0.1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceRegistrationRequest {

  /** Device unique identifier (UUID) */
  private String deviceIdentifier;

  /** User-provided device name */
  private String deviceName;

  /** Device type (MOBILE, DESKTOP, TABLET, AGENT) */
  private String deviceType;

  /** Operating system information */
  private String osInfo;

  /** Application version */
  private String appVersion;
}
