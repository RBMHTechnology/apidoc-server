package com.rbmhtechnology.apidocserver.security;

import org.mitre.openid.connect.client.OIDCAuthenticationFilter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.preauth.AbstractPreAuthenticatedProcessingFilter;

@Configuration
@Profile("openid")
public class OpenIdConnectSecurityConfiguration extends WebSecurityConfigurerAdapter {

  private final AuthenticationEntryPoint authenticationEntryPoint;
  private final OIDCAuthenticationFilter authenticationFilter;

  public OpenIdConnectSecurityConfiguration(
      AuthenticationEntryPoint authenticationEntryPoint,
      OIDCAuthenticationFilter authenticationFilter) {
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.authenticationFilter = authenticationFilter;
  }

  @Override
  public void configure(HttpSecurity http) throws Exception {
    http
        .addFilterBefore(authenticationFilter, AbstractPreAuthenticatedProcessingFilter.class)
        .exceptionHandling().authenticationEntryPoint(authenticationEntryPoint)
        .and().authorizeRequests()
        .antMatchers("/health").permitAll()
        .anyRequest().authenticated();
  }
}
