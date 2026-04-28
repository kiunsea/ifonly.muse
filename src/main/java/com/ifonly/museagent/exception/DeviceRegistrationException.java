package com.ifonly.museagent.exception;

/**
 * Device Registration Exception
 *
 * <p>Thrown when device registration operations fail.
 *
 * @author if-only
 * @version 0.1.0
 */
public class DeviceRegistrationException extends EchoServerException {

  /**
   * Constructor with message
   *
   * @param message error message
   */
  public DeviceRegistrationException(String message) {
    super(message);
  }

  /**
   * Constructor with message and cause
   *
   * @param message error message
   * @param cause underlying cause
   */
  public DeviceRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }
}
