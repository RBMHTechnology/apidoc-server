package com.rbmhtechnology.apidocserver.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates an exception caused by a not found artifact version in the repository
 */
@ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
public class VersionNotFoundException extends RepositoryException {

  private static final long serialVersionUID = 1L;

  public VersionNotFoundException(String message) {
    super(message);
  }

  public VersionNotFoundException(String message, Throwable cause) {
    super(message, cause);
  }
}