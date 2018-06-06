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

import io.vavr.control.Option;
import java.net.URL;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@Component
public class MavenRepositoryConfig {

  private static final String JCENTER = "http://jcenter.bintray.com";

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
