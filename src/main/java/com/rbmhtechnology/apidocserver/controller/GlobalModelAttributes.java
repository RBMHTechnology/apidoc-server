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
package com.rbmhtechnology.apidocserver.controller;

import com.rbmhtechnology.apidocserver.service.GroupIdWhitelistService;
import com.rbmhtechnology.apidocserver.service.RepositoryService;
import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepositoryConfig;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAttributes {

  private final RepositoryService repositoryService;
  private final GroupIdWhitelistService groupIdWhitelistService;
  private final URL repositoryUrl;

  @Autowired
  public GlobalModelAttributes(RepositoryService repositoryService,
      GroupIdWhitelistService groupIdWhitelistService,
      MavenRepositoryConfig mavenConfig) {
    this.repositoryService = repositoryService;
    this.groupIdWhitelistService = groupIdWhitelistService;
    this.repositoryUrl = mavenConfig.repositoryUrl();
  }

  @ModelAttribute
  public void globalAttributes(Model model, HttpServletRequest request) {
    model.addAttribute("name", repositoryService.getName());
    model.addAttribute("baseUrl", baseUrl(request));
    model.addAttribute("applicationVersion", applicationVersion());
    model.addAttribute("expectedClassifiers", repositoryService.getExpectedClassifiers()
        .intersperse(", ")
        .foldLeft(new StringBuilder(), StringBuilder::append)
        .toString());
    model.addAttribute("repositoryUrl", repositoryUrl);
    model.addAttribute("groupIdWhitelist", groupIdWhitelistService.getGroupIdPrefixWhitelist());
  }

  private String baseUrl(HttpServletRequest request) {
    final String hostFromRequest = request.getHeader("Host");
    final String host = hostFromRequest != null ? hostFromRequest : "localhost";
    return request.getScheme() + "://" + host;
  }

  private String applicationVersion() {
    final String version = getClass().getPackage().getImplementationVersion();
    return version == null ? "undefined" : "v" + version;
  }
}
