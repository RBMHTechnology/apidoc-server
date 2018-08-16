package com.rbmhtechnology.apidocserver.security;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import org.mitre.oauth2.model.ClientDetailsEntity.AuthMethod;
import org.mitre.oauth2.model.RegisteredClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

@Profile("openid")
@ConfigurationProperties(prefix = "client")
public class OpenIdConnectClient {

  private String id;
  private String secret;
  private String scope;
  private String redirectUri;
  private String tokenEndpointAuthMethod;

  public RegisteredClient registeredClient() {
    final RegisteredClient client = new RegisteredClient();
    client.setClientId(id);
    client.setClientSecret(secret);
    client.setScope(new HashSet<>(Arrays.asList("openid", scope)));
    client.setRedirectUris(new HashSet<>(Arrays.asList(redirectUri)));
    client.setTokenEndpointAuthMethod(AuthMethod.valueOf(tokenEndpointAuthMethod));
    return client;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getSecret() {
    return secret;
  }

  public void setSecret(String secret) {
    this.secret = secret;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getTokenEndpointAuthMethod() {
    return tokenEndpointAuthMethod;
  }

  public void setTokenEndpointAuthMethod(String tokenEndpointAuthMethod) {
    this.tokenEndpointAuthMethod = tokenEndpointAuthMethod;
  }

  @Override
  public String toString() {
    return "OpenIdConnectClient{" +
        "id='" + id + '\'' +
        ", secret='" + secret + '\'' +
        ", scope='" + scope + '\'' +
        ", redirectUri='" + redirectUri + '\'' +
        ", tokenEndpointAuthMethod='" + tokenEndpointAuthMethod + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OpenIdConnectClient that = (OpenIdConnectClient) o;
    return Objects.equals(id, that.id) &&
        Objects.equals(secret, that.secret) &&
        Objects.equals(scope, that.scope) &&
        Objects.equals(redirectUri, that.redirectUri) &&
        Objects.equals(tokenEndpointAuthMethod, that.tokenEndpointAuthMethod);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, secret, scope, redirectUri, tokenEndpointAuthMethod);
  }
}
