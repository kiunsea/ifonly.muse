package com.ifonly.museagent.exception;

/**
 * Echo Server base exception
 *
 * @author if-only
 * @version 0.1.1
 */
public class EchoServerException extends RuntimeException {

  public EchoServerException(String message) {
    super(message);
  }

  public EchoServerException(String message, Throwable cause) {
    super(message, cause);
  }
}
