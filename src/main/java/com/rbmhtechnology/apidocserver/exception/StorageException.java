package com.rbmhtechnology.apidocserver.exception;

/**
 * Indicates an exception related to storage aspects
 */
public class StorageException extends RepositoryException {

  private static final long serialVersionUID = 1L;

  public StorageException(String message) {
    super(message);
  }

  public StorageException(String message, Throwable cause) {
    super(message, cause);
  }
}