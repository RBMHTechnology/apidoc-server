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
    model.addAttribute("defaultClassifier", repositoryService.getDefaultClassifier());
    model.addAttribute("applicationVersion", applicationVersion());
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
