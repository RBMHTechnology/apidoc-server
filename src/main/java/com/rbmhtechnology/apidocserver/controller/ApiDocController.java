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
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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

  private static final Logger LOG = LoggerFactory.getLogger(ApiDocController.class);

  private static final List<String> DEFAULT_INDEX_FILES = Arrays.asList("index.html", "index.htm");

  private final RepositoryService repositoryService;
  private final GroupIdWhitelistService groupIdWhitelistService;

  @Autowired
  public ApiDocController(RepositoryService repositoryService,
      GroupIdWhitelistService groupIdWhitelistService) {
    this.repositoryService = repositoryService;
    this.groupIdWhitelistService = groupIdWhitelistService;
  }

  @GetMapping("/{groupId}/{artifactId}")
  String versions(Model model,
      @PathVariable String groupId,
      @PathVariable String artifactId) throws RepositoryException {
    LOG.trace("groupId: {}, artifactId: {}.", groupId, artifactId, null);
    ensureValidGroupId(groupId);
    List<String> versions = repositoryService.getAvailableVersions(groupId, artifactId);
    model.addAttribute("groupId", groupId);
    model.addAttribute("artifactId", artifactId);
    model.addAttribute("versions", versions);
    return "listVersions";
  }

  private void ensureValidGroupId(String groupId) throws AccessNotAllowedException {
    if (!groupIdWhitelistService.isValidGroupId(groupId)) {
      throw new AccessNotAllowedException("Access to groupId " + groupId + " not allowed!");
    }
  }

  @GetMapping("/{groupId}/{artifactId}/{version:.*}")
  String base(@PathVariable String groupId, @PathVariable String artifactId,
      @PathVariable String version, Model model) throws RepositoryException {
    LOG.trace("groupId: {}, artifactId: {}, version: {}. redirect to index.html",
        groupId, artifactId, version);
    ensureValidGroupId(groupId);

    final List<String> classifiers = repositoryService
        .getAvailableClassifier(groupId, artifactId, version);

    if (classifiers.size() == 1) {
      return "redirect:/{groupId}/{artifactId}/{version}/" + classifiers.get(0)
          + "/index.html";
    }

    model.addAttribute("groupId", groupId);
    model.addAttribute("artifactId", artifactId);
    model.addAttribute("classifiers", classifiers);
    return "listClassifiers";
  }

  @GetMapping(value = "/{groupId}/{artifactId}/{version:.*}/{classifier}")
  String base(@PathVariable String groupId,
      @PathVariable String artifactId,
      @PathVariable String version,
      @PathVariable String classifier) throws AccessNotAllowedException {
    LOG.trace("groupId: {}, artifactId: {}, version: {}, classifier: {}. redirect to index.html",
        groupId, artifactId, version, classifier);
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
    LOG.trace("groupId: {}, artifactId: {}, version: {}, classifier: {}, subPath :{}",
        groupId, artifactId, version, classifier, subPath);
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
    if (filename.endsWith(".svg")) {
      return "image/svg+xml";
    }
    LOG.trace("resolved {} as mime type for filename {}", mimeType, filename);
    return mimeType;
  }

  private String getSubPath(String groupId,
      String artifactId,
      String version,
      String classifier,
      HttpServletRequest request) {
    String base = "/" + groupId + "/" + artifactId + "/" + version + "/" + classifier + "/";
    String path = request.getRequestURI();
    return path.substring(base.length());
  }
}
