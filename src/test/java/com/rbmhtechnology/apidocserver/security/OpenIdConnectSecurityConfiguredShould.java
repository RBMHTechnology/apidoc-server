package com.rbmhtechnology.apidocserver.security;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("openid")
public class OpenIdConnectSecurityConfiguredShould {

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void forward_to_OIDC() throws Exception {
    mockMvc.perform(get("/"))
        .andExpect(status().isFound())
        .andExpect(redirectedUrl("http://localhost/openid_connect_login"));
  }

}
