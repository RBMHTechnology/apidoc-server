package com.rbmhtechnology.apidocserver.security;

import java.util.Objects;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;

@Profile("openid")
@ConfigurationProperties(prefix = "idp")
public class OpenIdConnectServer {

  private String url;
  private String issuer;
  private String authorizationEndpointUri;
  private String tokenEndpointUri;
  private String userInfoUri;
  private String jwksUri;

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getAuthorizationEndpointUri() {
    return authorizationEndpointUri;
  }

  public void setAuthorizationEndpointUri(String authorizationEndpointUri) {
    this.authorizationEndpointUri = authorizationEndpointUri;
  }

  public String getTokenEndpointUri() {
    return tokenEndpointUri;
  }

  public void setTokenEndpointUri(String tokenEndpointUri) {
    this.tokenEndpointUri = tokenEndpointUri;
  }

  public String getUserInfoUri() {
    return userInfoUri;
  }

  public void setUserInfoUri(String userInfoUri) {
    this.userInfoUri = userInfoUri;
  }

  public String getJwksUri() {
    return jwksUri;
  }

  public void setJwksUri(String jwksUri) {
    this.jwksUri = jwksUri;
  }

  public ServerConfiguration serverConfiguration() {
    final ServerConfiguration serverConfig = new ServerConfiguration();
    serverConfig.setIssuer(this.getIssuer());
    serverConfig.setAuthorizationEndpointUri(this.getAuthorizationEndpointUri());
    serverConfig.setTokenEndpointUri(this.getTokenEndpointUri());
    serverConfig.setUserInfoUri(this.getUserInfoUri());
    serverConfig.setJwksUri(this.getJwksUri());
    return serverConfig;
  }

  @Override
  public String toString() {
    return "Idp{" +
        "url='" + url + '\'' +
        ", issuer='" + issuer + '\'' +
        ", authorizationEndpointUri='" + authorizationEndpointUri + '\'' +
        ", tokenEndpointUri='" + tokenEndpointUri + '\'' +
        ", userInfoUri='" + userInfoUri + '\'' +
        ", jwksUri='" + jwksUri + '\'' +
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
    OpenIdConnectServer openIdConnectServer = (OpenIdConnectServer) o;
    return Objects.equals(url, openIdConnectServer.url) &&
        Objects.equals(issuer, openIdConnectServer.issuer) &&
        Objects.equals(authorizationEndpointUri, openIdConnectServer.authorizationEndpointUri) &&
        Objects.equals(tokenEndpointUri, openIdConnectServer.tokenEndpointUri) &&
        Objects.equals(userInfoUri, openIdConnectServer.userInfoUri) &&
        Objects.equals(jwksUri, openIdConnectServer.jwksUri);
  }

  @Override
  public int hashCode() {
    return Objects
        .hash(url, issuer, authorizationEndpointUri, tokenEndpointUri, userInfoUri, jwksUri);
  }
}
