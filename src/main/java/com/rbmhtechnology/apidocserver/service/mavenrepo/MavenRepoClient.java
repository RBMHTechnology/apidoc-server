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
package com.rbmhtechnology.apidocserver.service.mavenrepo;

import static java.lang.String.format;

import com.rbmhtechnology.apidocserver.exception.AccessNotAllowedException;
import com.rbmhtechnology.apidocserver.exception.DownloadException;
import com.rbmhtechnology.apidocserver.exception.NotFoundException;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.service.ArtifactIdentifier;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Try;
import java.io.File;
import org.apache.maven.wagon.ConnectionException;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.authentication.AuthenticationException;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.providers.http.HttpWagon;
import org.apache.maven.wagon.repository.Repository;
import org.apache.maven.wagon.shared.http.BasicAuthScope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class MavenRepoClient {

  private static final Logger LOG = LoggerFactory.getLogger(MavenRepoClient.class);

  private final HttpWagon httpWagon;
  private final Repository repository;
  private final AuthenticationInfo authenticationInfo;

  public MavenRepoClient(MavenRepositoryConfig config) {
    this.httpWagon = new HttpWagon();
    this.authenticationInfo = config.getCredentials().map(c -> {
      final AuthenticationInfo authInfo = new AuthenticationInfo();
      authInfo.setUserName(c.username());
      authInfo.setPassword(c.password());
      BasicAuthScope scope = new BasicAuthScope();
      scope.setHost(config.repositoryHost());
      scope.setPort(String.valueOf(config.repositoryPort()));
      httpWagon.setBasicAuthScope(scope);
      return authInfo;
    }).getOrElse((AuthenticationInfo) null);

    this.repository = new Repository("apidoc-source", config.repositoryUrl().toString());
  }

  public void get(String resourceName, File destination) throws RepositoryException {
    LOG.debug("Started downloading '{}' to '{}'", resourceName, destination);
    try {
      httpWagon.connect(repository, authenticationInfo);
      httpWagon.get(resourceName, destination);
    } catch (ResourceDoesNotExistException e) {
      throw new NotFoundException("No jar at '" + resourceName + "', failed with status:" + e.getMessage());
    } catch (AuthorizationException | AuthenticationException e) {
      throw new DownloadException("Access denied for " + resourceName + "', failed with status:"
          + e.getMessage());
    } catch (TransferFailedException | ConnectionException e) {
      throw new DownloadException("Transfer failed for " + resourceName + "', failed with status:"
          + e.getMessage());
    } finally {
      disconnectQuietly();
    }
  }

  private void disconnectQuietly() {
    try {
      httpWagon.disconnect();
    } catch (ConnectionException e) {
      LOG.warn("Unable to disconnect from " + repository.getUrl() + ", reason:" + e
          .getMessage());
    }
  }

  public Map<String, Boolean> exists(List<ArtifactIdentifier> resources)
      throws RepositoryException {
    try {

      httpWagon.connect(repository, authenticationInfo);
      return resources
          .toMap(ArtifactIdentifier::getClassifier, r -> existsQuietly(r.mavenLayout()));

    } catch (AuthenticationException e) {
      throw new AccessNotAllowedException(format("Access Denied to repository '%s' reason: %s",
          repository.getUrl(), e.getMessage()));
    } catch (ConnectionException e) {
      throw new RepositoryException(format("Connection problem to repository '%s' reason: %s",
          repository.getUrl(), e.getMessage()));
    } finally {
      disconnectQuietly();
    }
  }

  private boolean existsQuietly(String resourceName) {
    return Try.of(() -> httpWagon.resourceExists(resourceName))
        .onFailure(t -> LOG.warn(format("Unable to check if resource '%s' exists: reason%s",
            resourceName, t.getMessage())))
        .getOrElse(false);
  }
}
