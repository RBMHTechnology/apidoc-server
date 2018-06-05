package com.rbmhtechnology.apidocserver.service.mavenrepo;

import static com.rbmhtechnology.apidocserver.service.RepositoryService.JCENTER;

import io.vavr.control.Option;
import java.net.URL;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class MavenRepositoryConfig {

  private final URL repositoryUrl;
  private final Option<Credentials> credentials;

  public MavenRepositoryConfig(@Value("${repository.url:" + JCENTER + "}") URL repositoryUrl,
      @Value("${repository.username:#{null}}") String user,
      @Value("${repository.password:#{null}}") String password) {
    Assert.notNull(repositoryUrl, "Cannot access maven without a url");
    this.repositoryUrl = repositoryUrl;
    this.credentials = credentials(user, password);
  }

  private Option<Credentials> credentials(String username, String password) {
    if (username != null && password != null) {
      return Option.of(new Credentials(username, password));
    }
    return Option.none();
  }

  public URL repositoryUrl() {
    return repositoryUrl;
  }

  String repositoryHost() {
    return repositoryUrl.getHost();
  }

  int repositoryPort() {
    return repositoryUrl.getPort();
  }

  Option<Credentials> getCredentials() {
    return credentials;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MavenRepositoryConfig that = (MavenRepositoryConfig) o;
    return Objects.equals(repositoryUrl, that.repositoryUrl) &&
        Objects.equals(credentials, that.credentials);
  }

  @Override
  public int hashCode() {
    return Objects.hash(repositoryUrl, credentials);
  }

  static class Credentials {

    private final String username;
    private final String password;

    Credentials(String username, String password) {
      Assert.hasText(username, "Username must be provided");
      Assert.hasText(password, "Password must be provided");
      this.username = username;
      this.password = password;
    }

    String password() {
      return password;
    }

    String username() {
      return username;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Credentials that = (Credentials) o;
      return Objects.equals(username, that.username) &&
          Objects.equals(password, that.password);
    }

    @Override
    public int hashCode() {

      return Objects.hash(username, password);
    }
  }
}
