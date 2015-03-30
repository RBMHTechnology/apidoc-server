package com.rbmhtechnology.apidocserver.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown if access to a resource is not allowed
 */
@ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
public class AccessNotAllowedException extends RepositoryException {

	private static final long serialVersionUID = 1L;

	public AccessNotAllowedException(String message) {
		super(message);
	}

	public AccessNotAllowedException(String message, Throwable cause) {
		super(message, cause);
	}
}