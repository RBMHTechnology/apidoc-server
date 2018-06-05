package com.rbmhtechnology.apidocserver.service.mavenrepo;

import com.rbmhtechnology.apidocserver.exception.DownloadException;
import com.rbmhtechnology.apidocserver.exception.NotFoundException;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
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

import java.io.File;

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
      httpWagon.disconnect();
    } catch (ResourceDoesNotExistException e) {
      throw new NotFoundException("No jar at '" + resourceName + "', failed with status:" + e.getMessage());
    } catch (AuthorizationException | AuthenticationException e) {
      throw new DownloadException("Access denied for " + resourceName + "', failed with status:"
              + e.getMessage());
    } catch (TransferFailedException | ConnectionException e) {
      throw new DownloadException("Transfer failed for " + resourceName + "', failed with status:"
              + e.getMessage());
    }
  }
}
