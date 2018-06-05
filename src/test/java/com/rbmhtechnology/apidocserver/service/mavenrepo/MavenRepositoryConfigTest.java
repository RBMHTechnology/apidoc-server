package com.rbmhtechnology.apidocserver.service.mavenrepo;

import static com.rbmhtechnology.apidocserver.testhelper.OptionAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepositoryConfig.Credentials;
import java.net.MalformedURLException;
import java.net.URI;
import org.junit.Test;

public class MavenRepositoryConfigTest {

  @Test
  public void repositoryUrl_is_mandatory() {
    assertThatThrownBy(() -> new MavenRepositoryConfig(null, "foo", "bar"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void given_no_user_name_credentials_are_empty() throws MalformedURLException {
    final MavenRepositoryConfig config = new MavenRepositoryConfig(URI
        .create("http://example.com").toURL(), null, "bar");
    assertThat(config.getCredentials()).isNotDefined();
  }

  @Test
  public void given_no_password_credentials_are_empty() throws MalformedURLException {
    final MavenRepositoryConfig config = new MavenRepositoryConfig(URI
        .create("http://example.com").toURL(), "foo", null);
    assertThat(config.getCredentials()).isNotDefined();
  }

  @Test
  public void given_user_name_and_password_credentials_are_set() throws MalformedURLException {
    final MavenRepositoryConfig config = new MavenRepositoryConfig(URI
        .create("http://example.com").toURL(), "foo", "bar");
    assertThat(config.getCredentials().get()).isEqualTo(new Credentials("foo", "bar"));
  }

}