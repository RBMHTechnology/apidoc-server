/*
 * Copyright (C) 2015 Red Bull Media House GmbH <http://www.redbullmediahouse.com> - all rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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