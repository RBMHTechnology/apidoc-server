package com.rbmhtechnology.apidocserver.exception;

/**
 * Indicates an exception related to downloading from the repository
 */
public class DownloadException extends RepositoryException {

  private static final long serialVersionUID = 1L;

  public DownloadException(String message) {
    super(message);
  }

  public DownloadException(String message, Throwable cause) {
    super(message, cause);
  }
}