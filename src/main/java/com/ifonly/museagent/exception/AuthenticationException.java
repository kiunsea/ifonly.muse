package com.ifonly.museagent.exception;

/**
 * Echo Server authentication failure exception
 *
 * @author if-only
 * @version 0.1.1
 */
public class AuthenticationException extends EchoServerException {

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }
}
