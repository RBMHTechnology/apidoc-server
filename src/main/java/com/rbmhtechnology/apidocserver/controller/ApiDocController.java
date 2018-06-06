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

import com.google.common.io.ByteStreams;
import com.rbmhtechnology.apidocserver.exception.AccessNotAllowedException;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.service.GroupIdWhitelistService;
import com.rbmhtechnology.apidocserver.service.RepositoryService;
import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepositoryConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.activation.MimetypesFileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ApiDocController {

  private Logger logger = LoggerFactory.getLogger(getClass());

  private static final List<String> DEFAULT_INDEX_FILES = Arrays.asList("index.html", "index.htm");

  private final RepositoryService repositoryService;
  private final GroupIdWhitelistService groupIdWhitelistService;
  private final URL repositoryUrl;

  @Autowired
  public ApiDocController(RepositoryService repositoryService,
          GroupIdWhitelistService groupIdWhitelistService,
          MavenRepositoryConfig mavenConfig) {
    this.repositoryService = repositoryService;
    this.groupIdWhitelistService = groupIdWhitelistService;
    this. repositoryUrl = mavenConfig.repositoryUrl();
  }

  private void ensureValidGroupId(String groupId) throws AccessNotAllowedException {
    if (!groupIdWhitelistService.isValidGroupId(groupId)) {
      throw new AccessNotAllowedException("Access to groupId " + groupId + " not allowed!");
    }
  }

  private void addBasicAttributes(Model model, HttpServletRequest request) {

    String baseUrl = request.getScheme() + "://"
        + (request.getHeader("Host") != null ? request.getHeader("Host") : "localhost");

    model.addAttribute("name", repositoryService.getName());
    model.addAttribute("baseUrl", baseUrl);
    model.addAttribute("defaultClassifier", repositoryService.getDefaultClassifier());
    model.addAttribute("applicationVersion", getApplicationVersion());
    model.addAttribute("repositoryUrl", repositoryUrl);
    model.addAttribute("groupIdWhitelist", groupIdWhitelistService.getGroupIdPrefixWhitelist());
  }

  @GetMapping("/")
  String home(Model model, HttpServletRequest request) {

    addBasicAttributes(model, request);

    return "home";
  }

  @GetMapping("/{groupId}/{artifactId}")
  String versions(Model model,
      HttpServletRequest request,
      @PathVariable String groupId,
      @PathVariable String artifactId) throws RepositoryException {
    logger.trace("groupId: {}, artifactId: {}.", groupId, artifactId, null);
    ensureValidGroupId(groupId);
    List<String> versions = repositoryService.getAvailableVersions(groupId, artifactId);
    model.addAttribute("groupId", groupId);
    model.addAttribute("artifactId", artifactId);
    model.addAttribute("versions", versions);
    addBasicAttributes(model, request);

    return "listVersions";
  }

  @GetMapping("/{groupId}/{artifactId}/{version:.*}")
  String base(@PathVariable String groupId, @PathVariable String artifactId,
      @PathVariable String version) throws AccessNotAllowedException {
    logger.trace("groupId: {}, artifactId: {}, version: {}. rediret to index.html",
        groupId,
        artifactId,
        version,
        null);
    ensureValidGroupId(groupId);
    return "redirect:/{groupId}/{artifactId}/{version}/" + repositoryService.getDefaultClassifier()
        + "/index.html";
  }

  @GetMapping(value = "/{groupId}/{artifactId}/{version:.*}/{classifier}")
  String base(@PathVariable String groupId,
      @PathVariable String artifactId,
      @PathVariable String version,
      @PathVariable String classifier) throws AccessNotAllowedException {
    logger.trace("groupId: {}, artifactId: {}, version: {}, classifier: {}. rediret to index.html",
        groupId,
        artifactId,
        version,
        classifier);
    ensureValidGroupId(groupId);
    return "redirect:/{groupId}/{artifactId}/{version}/{classifier}/index.html";
  }

  // see https://thecruskit.com/spring-pathvariable-and-truncation-after-dot-period/
  @GetMapping(value = "/{groupId}/{artifactId}/{version:.*}/{classifier}/**")
  @ResponseBody
  void serve(@PathVariable String groupId,
      @PathVariable String artifactId,
      @PathVariable String version,
      @PathVariable String classifier,
      HttpServletRequest request,
      HttpServletResponse response) throws RepositoryException {

    String subPath = getSubPath(classifier, groupId, artifactId, version, request);
    logger.trace("groupId: {}, artifactId: {}, version: {}, classifier: {}, subPath :{}",
        groupId,
        artifactId,
        version,
        classifier,
        subPath);
    ensureValidGroupId(groupId);

    File jar = repositoryService.retrieveJarFile(groupId, artifactId, version, classifier);

    if (jar == null) {
      throw new RuntimeException(
          "No documentation artifact file available for group:" + groupId + ", artifact:"
              + artifactId + " and version:" + version + "");
    }
    try {
      serveFileFromJarFile(response, jar, subPath);
    } catch (IOException e) {
      throw new RuntimeException(
          "Error serving '" + subPath + "' for group:" + groupId + ", artifact:"
              + artifactId + " and version:" + version);
    }
  }

  private void serveFileFromJarFile(HttpServletResponse response, File jar, String subPath)
      throws IOException {
    try (JarFile jarFile = new JarFile(jar)) {
      JarEntry entry = jarFile.getJarEntry(subPath);

      if (entry == null) {
        response.sendError(404);
        return;
      }

      // fallback for requesting a directory without a trailing /
      // this leads to a jarentry which is not null, and not a directory and of size 0
      // this shouldn't be
      if (!entry.isDirectory() && entry.getSize() == 0) {
        if (!subPath.endsWith("/")) {
          JarEntry entryWithSlash = jarFile.getJarEntry(subPath + "/");
          if (entryWithSlash != null && entryWithSlash.isDirectory()) {
            entry = entryWithSlash;
          }
        }
      }

      if (entry.isDirectory()) {
        for (String indexFile : DEFAULT_INDEX_FILES) {
          entry = jarFile
              .getJarEntry((subPath.endsWith("/") ? subPath : subPath + "/") + indexFile);
          if (entry != null) {
            break;
          }
        }
      }

      if (entry == null) {
        response.sendError(404);
        return;
      }

      response.setContentLength((int) entry.getSize());
      String mimetype = getMimeType(entry.getName());
      response.setContentType(mimetype);
      try (InputStream input = jarFile.getInputStream(entry)) {
        ByteStreams.copy(input, response.getOutputStream());
      }
    }
  }

  private String getMimeType(String filename) {
    String mimeType = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(filename);
    if (filename.endsWith(".css")) {
      return "text/css";
    }
    if (filename.endsWith(".js")) {
      return "application/javascript";
    }
    logger.trace("resolved {} as mime type for filename {}", mimeType, filename);
    return mimeType;
  }

  private String getSubPath(String groupId,
      String artifactId,
      String version,
      String classifier,
      HttpServletRequest request) {
    String base = "/" + groupId + "/" + artifactId + "/" + version + "/" + classifier + "/";
    String path = request.getRequestURI();
    String subPath = path.substring(base.length());
    return subPath;
  }

  private String getApplicationVersion() {
    String implementationVersion = getClass().getPackage().getImplementationVersion();
    return implementationVersion == null ? "undefined" : "v" + implementationVersion;
  }
}
