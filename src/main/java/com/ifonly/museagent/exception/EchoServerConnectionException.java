package com.ifonly.museagent.exception;

/**
 * Echo Server connection failure exception
 *
 * @author if-only
 * @version 0.1.1
 */
public class EchoServerConnectionException extends EchoServerException {

  public EchoServerConnectionException(String message) {
    super(message);
  }

  public EchoServerConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
