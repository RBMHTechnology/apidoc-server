package com.rbmhtechnology.apidocserver.security;

import static org.mitre.openid.connect.client.OIDCAuthenticationFilter.FILTER_PROCESSES_URL;

import java.util.HashMap;
import java.util.Map;
import org.mitre.oauth2.model.RegisteredClient;
import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.mitre.openid.connect.client.OIDCAuthenticationProvider;
import org.mitre.openid.connect.client.service.AuthRequestOptionsService;
import org.mitre.openid.connect.client.service.AuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.ClientConfigurationService;
import org.mitre.openid.connect.client.service.IssuerService;
import org.mitre.openid.connect.client.service.ServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.PlainAuthRequestUrlBuilder;
import org.mitre.openid.connect.client.service.impl.StaticAuthRequestOptionsService;
import org.mitre.openid.connect.client.service.impl.StaticClientConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticServerConfigurationService;
import org.mitre.openid.connect.client.service.impl.StaticSingleIssuerService;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
@Profile("openid")
@EnableConfigurationProperties({OpenIdConnectServer.class, OpenIdConnectClient.class})
@EnableWebSecurity
public class OpenIdConnectMitreConfiguration {

  private static final String LOGIN_ENDPOINT = FILTER_PROCESSES_URL;

  @Bean
  public AuthenticationEntryPoint authenticationEntryPoint() {
    return new LoginUrlAuthenticationEntryPoint(LOGIN_ENDPOINT);
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    return new OIDCAuthenticationProvider();
  }

  @Bean
  public OIDCAuthenticationFilter openIdConnectAuthenticationFilter(AuthenticationManager manager,
      IssuerService issuerService,
      ServerConfigurationService serverConfigurationService,
      ClientConfigurationService clientConfigurationService,
      AuthRequestOptionsService authRequestOptionsService,
      AuthRequestUrlBuilder authRequestUrlBuilder) {
    final OIDCAuthenticationFilter filter = new OIDCAuthenticationFilter();
    filter.setAuthenticationManager(manager);
    filter.setIssuerService(issuerService);
    filter.setServerConfigurationService(serverConfigurationService);
    filter.setClientConfigurationService(clientConfigurationService);
    filter.setAuthRequestOptionsService(authRequestOptionsService);
    filter.setAuthRequestUrlBuilder(authRequestUrlBuilder);
    return filter;
  }

  @Bean
  public IssuerService issuerService(OpenIdConnectServer idp) {
    final StaticSingleIssuerService issuerService = new StaticSingleIssuerService();
    issuerService.setIssuer(idp.getIssuer());
    return issuerService;
  }

  @Bean
  public ServerConfigurationService serverConfigurationService(OpenIdConnectServer idp) {
    final Map<String, ServerConfiguration> singleServer = new HashMap<>();
    singleServer.put(idp.getUrl(), idp.serverConfiguration());

    final StaticServerConfigurationService configService = new StaticServerConfigurationService();
    configService.setServers(singleServer);

    return configService;
  }

  @Bean
  public ClientConfigurationService clientConfigurationService(
      OpenIdConnectServer idp, OpenIdConnectClient client) {
    final Map<String, RegisteredClient> singleClient = new HashMap<>();
    singleClient.put(idp.getUrl(), client.registeredClient());

    final StaticClientConfigurationService configService = new StaticClientConfigurationService();
    configService.setClients(singleClient);
    return configService;
  }

  @Bean
  public AuthRequestOptionsService authRequestOptionsService() {
    final Map<String, String> options = new HashMap<>();
    options.put("display", "page");
    final StaticAuthRequestOptionsService authRequestOptionsService = new StaticAuthRequestOptionsService();
    authRequestOptionsService.setOptions(options);
    return authRequestOptionsService;
  }

  @Bean
  public AuthRequestUrlBuilder authRequestUrlBuilder() {
    return new PlainAuthRequestUrlBuilder();
  }
}
