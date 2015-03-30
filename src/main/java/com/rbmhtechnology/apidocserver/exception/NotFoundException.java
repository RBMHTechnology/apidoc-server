package com.rbmhtechnology.apidocserver.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Indicates an exception caused by a not found documentation artifact in the repository
 */
@ResponseStatus(org.springframework.http.HttpStatus.NOT_FOUND)
public class NotFoundException extends DownloadException {

	private static final long serialVersionUID = 1L;

	public NotFoundException(String message) {
		super(message);
	}

	public NotFoundException(String message, Throwable cause) {
		super(message, cause);
	}
}
