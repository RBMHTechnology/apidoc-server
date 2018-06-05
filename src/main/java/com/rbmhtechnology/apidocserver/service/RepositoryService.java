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
package com.rbmhtechnology.apidocserver.service;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.exception.VersionNotFoundException;
import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepoClient;
import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepositoryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.rbmhtechnology.apidocserver.service.RepositoryService.MavenVersionRef.LATEST;
import static com.rbmhtechnology.apidocserver.service.RepositoryService.MavenVersionRef.RELEASE;
import static java.util.Comparator.reverseOrder;
import static java.util.concurrent.TimeUnit.SECONDS;

@Service
public class RepositoryService {

  /**
   * shortcut for the version indicating the latest snapshot version
   */
  public static final String LATEST_VERSION_SHORTCUT = "latest";

  /**
   * shortcut for the version indicating the latest release version
   */
  public static final String RELEASE_VERSION_SHORTCUT = "release";

  public static final String JCENTER = "http://jcenter.bintray.com";

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

  private final String name;
  private final String defaultClassifier;
  private final URL repositoryUrl;
  private final boolean snapshotsEnabled;
  private final File localJarStorage;
  private final MavenRepoClient mavenClient;

  private LoadingCache<ArtifactIdentifier, File> snapshotDownloadUrlCache;
  private LoadingCache<ArtifactIdentifier, File> releaseDownloadUrlCache;

  private LoadingCache<GroupArtifactCacheKey, String> latestVersionCache;
  private LoadingCache<GroupArtifactCacheKey, String> releaseVersionCache;

  public RepositoryService(
      @Value("${name:ApiDoc Server}") String name,
      @Value("${default.classifier:javadoc}") String defaultClassifier,
      @Value("${repository.snapshots.enabled:true}") boolean snapshotsEnabled,
      @Value("${repository.snapshots.cache-timeout:1800}") int cacheTimeoutSeconds,
      @Value("${localstorage:#{null}}") File localstoragePath,
      MavenRepositoryConfig repositoryConfig,
      MavenRepoClient mavenClient) {
    this.name = name;
    this.defaultClassifier = defaultClassifier;
    this.snapshotsEnabled = snapshotsEnabled;
    this.localJarStorage = localStorageOrTempFile(localstoragePath);
    this.repositoryUrl = repositoryConfig.repositoryUrl();

    this.snapshotDownloadUrlCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(cacheTimeoutSeconds, SECONDS)
        .removalListener(new SnapshotRemovalListener())
        .build(new ArtifactLoader());

    this.latestVersionCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(cacheTimeoutSeconds, SECONDS)
        .build(new MavenXmlVersionRefResolver(LATEST));
    this.mavenClient = mavenClient;

    this.releaseDownloadUrlCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new ArtifactLoader());

    this.releaseVersionCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new MavenXmlVersionRefResolver(RELEASE));
  }

  private File localStorageOrTempFile(@Value("${localstorage:#{null}}") File localJarStorage) {
    if (localJarStorage != null) {
      return localJarStorage;
    }
    return Files.createTempDir();
  }


  public enum MavenVersionRef {
    LATEST("latest"), RELEASE("release");

    String xmlElementName;

    MavenVersionRef(String xmlElementName) {
      this.xmlElementName = xmlElementName;
    }

    public String getXmlElementName() {
      return xmlElementName;
    }
  }

  private String getVersionRefFromMetadataXML(String groupId, String artifactId,
      MavenVersionRef versionRef) throws RepositoryException {
    LOG.info("getVersionRefFromMetadataXML('{}','{}','{}')", groupId, artifactId, versionRef);
    String version;

    File mavenMetadataXmlFile = downloadMavenMetadataXml(groupId, artifactId);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    // Using factory get an instance of document builder
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();

      // parse using builder to get DOM representation of the XML file
      Document dom = db.parse(mavenMetadataXmlFile);

      // get the root element
      Element rootElement = dom.getDocumentElement();
      NodeList latestNodeList = rootElement.getElementsByTagName(versionRef.getXmlElementName());
      if (latestNodeList.getLength() > 1) {
        throw new RepositoryException(
            "Found more than one element '" + versionRef.getXmlElementName()
                + "' in maven-metadata.xml");
      } else if (latestNodeList.getLength() == 1) {
        Node versionNode = latestNodeList.item(0);
        version = versionNode.getTextContent();
      } else {
        throw new VersionNotFoundException(
            "No " + versionRef + " version could be found for groupId: '"
                + groupId + "' and artifactId: '" + artifactId + "'");
      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      throw new RepositoryException("Could not parse maven-metadata.xml for groupId: '" + groupId
          + "' and artifactId: '" + artifactId + "'", e);
    }

    return version;
  }

  private File downloadMavenMetadataXml(String groupId, String artifactId)
      throws RepositoryException {
    File mavenMetadataXmlFile;
    try {
      mavenMetadataXmlFile = File.createTempFile(groupId + "-" + artifactId, "version");
    } catch (IOException e) {
      throw new RepositoryException("Could not create temp file for maven-metadata.xml download",
          e);
    }

    // download
    String downloadUrl = groupId.replace(".", "/") + "/"
        + artifactId + "/" + "maven-metadata.xml";

    mavenClient.get(downloadUrl, mavenMetadataXmlFile);
    return mavenMetadataXmlFile;
  }

  public List<String> getAvailableVersions(String groupId, String artifactId)
      throws RepositoryException {
    LOG.info("getAvailableVersions('{}','{}')", groupId, artifactId);

    File mavenMetadataXmlFile = downloadMavenMetadataXml(groupId, artifactId);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    // Using factory get an instance of document builder
    DocumentBuilder db;

    List<String> versions = Lists.newArrayList();
    try {
      db = dbf.newDocumentBuilder();

      // parse using builder to get DOM representation of the XML file
      Document dom = db.parse(mavenMetadataXmlFile);

      // get the root element
      Element rootElement = dom.getDocumentElement();
      NodeList versionNodeList = rootElement.getElementsByTagName("version");

      for (int i = 0; i < versionNodeList.getLength(); i++) {
        Node item = versionNodeList.item(i);
        if (!versions.contains(item.getTextContent())) {
          versions.add(item.getTextContent());
        }
      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      throw new RepositoryException("Could not parse maven-metadata.xml for groupId: '" + groupId
          + "' and artifactId: '" + artifactId + "'", e);
    }

    versions.sort(reverseOrder());

    return versions;
  }

  private String getApidocFileNameFromMetadataXML(ArtifactIdentifier artifactIdentifier)
      throws RepositoryException {
    if (!artifactIdentifier.isSnapshot()) {
      return artifactIdentifier.getArtifactId() + "-" + artifactIdentifier.getVersion() + "-"
          + artifactIdentifier.getClassifier() + ".jar";
    }

    File mavenMetadataXmlFile;
    try {
      mavenMetadataXmlFile = File.createTempFile(artifactIdentifier.toString(), ".xml");
    } catch (IOException e) {
      throw new RepositoryException("Could not create temp file for maven-metadata.xml download",
          e);
    }

    // download
    String downloadUrl = artifactIdentifier.getGroupId().replace(".", "/") + "/"
        + artifactIdentifier.getArtifactId() + "/" + artifactIdentifier.getVersion()
        + "/" + "maven-metadata.xml";

    mavenClient.get(downloadUrl, mavenMetadataXmlFile);

    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    // Using factory get an instance of document builder
    DocumentBuilder db;
    try {
      db = dbf.newDocumentBuilder();

      // parse using builder to get DOM representation of the XML file
      Document dom = db.parse(mavenMetadataXmlFile);

      // get the root element
      Element rootElement = dom.getDocumentElement();
      NodeList snapshotVersionsNodeList = rootElement.getElementsByTagName("snapshotVersion");
      for (int i = 0; i < snapshotVersionsNodeList.getLength(); i++) {
        Node snapshotVersionsNode = snapshotVersionsNodeList.item(i);

        Node classifierNode = null;
        Node extensionNode = null;
        Node valueNode = null;

        NodeList childNodes = snapshotVersionsNode.getChildNodes();
        for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
          Node _node = childNodes.item(childIndex);
          String nodeName = _node.getNodeName();
          if ("classifier".equals(nodeName)) {
            classifierNode = _node;
          } else if ("extension".equals(nodeName)) {
            extensionNode = _node;
          } else if ("value".equals(nodeName)) {
            valueNode = _node;
          }
        }
        if (classifierNode != null && extensionNode != null && valueNode != null) {
          if (artifactIdentifier.getClassifier().equals(classifierNode.getTextContent())
              && "jar".equals(extensionNode.getTextContent())) {
            // this is the documentation artifact
            return artifactIdentifier.getArtifactId() + "-" + valueNode.getTextContent() + "-"
                + classifierNode.getTextContent() + "." + extensionNode.getTextContent();
          }
        }
      }

      // did not find a proper version, try to use "snapshot"
      NodeList snapshotElements = rootElement.getElementsByTagName("snapshot");
      if (snapshotElements.getLength() == 1) {
        Node snapshotNode = snapshotElements.item(0);

        NodeList childNodes = snapshotNode.getChildNodes();
        Node timestampNode = null;
        Node buildNumberNode = null;
        for (int childIndex = 0; childIndex < childNodes.getLength(); childIndex++) {
          Node _node = childNodes.item(childIndex);
          String nodeName = _node.getNodeName();
          if ("timestamp".equals(nodeName)) {
            timestampNode = _node;
          } else if ("buildNumber".equals(nodeName)) {
            buildNumberNode = _node;
          }
        }

        if (timestampNode != null && buildNumberNode != null) {
          return artifactIdentifier.getArtifactId() + "-"
              + artifactIdentifier.getVersion().replace(ArtifactIdentifier.SNAPSHOT_SUFFIX, "")
              + "-"
              + timestampNode.getTextContent() + "-" + buildNumberNode.getTextContent() + "-"
              + artifactIdentifier.getClassifier() + ".jar";
        }
      }

    } catch (SAXException | IOException | ParserConfigurationException e) {
      throw new RepositoryException(
          "Could not parse maven-metadata.xml for '" + artifactIdentifier + "'", e);
    }

    // in case of a snapshot which has no reliable information coming from
    // maven-metadata.xml simply use the pattern for releases
    return artifactIdentifier.getArtifactId() + "-" + artifactIdentifier.getVersion() + "-"
        + artifactIdentifier.getClassifier() + ".jar";
  }

  /**
   * Resolves the version, if the version is a version reference like LATEST or RELEASE
   *
   * @param groupId the group id of the artifact
   * @param artifactId the artifact id
   * @param _version the version
   * @param classifier the classifier
   * @return the resolved version or <code>_version</code> if this is no reference
   * @throws RepositoryException if the identifier could not be created
   */
  public ArtifactIdentifier resolveArtifactIdentfier(String groupId,
      String artifactId,
      String _version,
      String classifier) throws RepositoryException {

    String version = _version;
    try {
      if (LATEST_VERSION_SHORTCUT.equalsIgnoreCase(_version)) {
        version = latestVersionCache.get(new GroupArtifactCacheKey(groupId, artifactId));
      } else if (RELEASE_VERSION_SHORTCUT.equalsIgnoreCase(_version)) {
        version = releaseVersionCache.get(new GroupArtifactCacheKey(groupId, artifactId));
      }
    } catch (ExecutionException e) {
      throw new RepositoryException("Could not resolve version", e);
    }

    return new ArtifactIdentifier(groupId, artifactId, version, classifier);
  }

  private File provideFileForArtifact(ArtifactIdentifier artifactIdentifier)
      throws RepositoryException {
    try {
      if (artifactIdentifier.isSnapshot()) {
        if (snapshotsEnabled) {
          return snapshotDownloadUrlCache.get(artifactIdentifier);
        } else {
          throw new RepositoryException("Snapshots not enabled");
        }
      } else {
        return releaseDownloadUrlCache.get(artifactIdentifier);
      }
    } catch (ExecutionException e) {
      throw new RepositoryException("Could not construct download url", e);
    }
  }


  public URL getRepositoryUrl() {
    return repositoryUrl;
  }

  public String getName() {
    return name;
  }

  public String getDefaultClassifier() {
    return defaultClassifier;
  }

  private final class ArtifactLoader extends CacheLoader<ArtifactIdentifier, File> {

    @Override
    public File load(ArtifactIdentifier artifactIdentifier) throws Exception {
      String documentationFilename = getApidocFileNameFromMetadataXML(artifactIdentifier);

      String downloadUrl = artifactIdentifier.getGroupId().replace(".", "/") + "/"
          + artifactIdentifier.getArtifactId() + "/" + artifactIdentifier.getVersion()
          + "/" + documentationFilename;

      LOG.debug("Resolved download url for '{}' to '{}'", artifactIdentifier, downloadUrl);

      File file = constructJarFileLocation(artifactIdentifier);

      mavenClient.get(downloadUrl, file);

      return file;
    }


    private File constructJarFileLocation(ArtifactIdentifier artifactIdentifier) {
      File file = new File(
          new File(new File(new File(localJarStorage, artifactIdentifier.getGroupId()),
              artifactIdentifier.getArtifactId()), artifactIdentifier.getVersion()),
          artifactIdentifier.getClassifier() + ".jar");
      LOG.debug("location for  {}: is {}", artifactIdentifier, file);
      return file;
    }
  }

  private final class MavenXmlVersionRefResolver extends
      CacheLoader<GroupArtifactCacheKey, String> {

    private MavenVersionRef mavenVersionRef;

    public MavenXmlVersionRefResolver(MavenVersionRef mavenVersionRef) {
      this.mavenVersionRef = mavenVersionRef;
    }

    @Override
    public String load(GroupArtifactCacheKey key) throws Exception {
      String version = getVersionRefFromMetadataXML(key.getGroupId(), key.getArtifactId(),
          mavenVersionRef);
      LOG.debug("Resolved {} for '{}' to '{}'", mavenVersionRef, key, version);
      return version;
    }
  }

  private final class SnapshotRemovalListener implements RemovalListener<ArtifactIdentifier, File> {

    @Override
    public void onRemoval(RemovalNotification<ArtifactIdentifier, File> notification) {
      ArtifactIdentifier artifactIdentifier = notification.getKey();
      File file = notification.getValue();
      if (file != null && file.exists()) {
        if (file.delete()) {
          LOG.debug("Removed downloaded jar '{}' for '{}'", file, artifactIdentifier);
        } else {
          LOG.warn("Could not remove downloaded jar '{}' for '{}'", file, artifactIdentifier);
        }
      } else {
        LOG.warn("Downloaded jar does not exists, so it cannot be cleaned up '{}' for '{}'", file,
            artifactIdentifier);
      }
    }
  }

  /**
   * Special version "latest" and "release" are supported
   *
   * @param groupId the maven group id
   * @param artifactId the maven artifact id
   * @param _version the version of the artifact
   * @param classifier this
   * @return the jar file in the local storage
   * @throws RepositoryException if the jar could not be downloaded or the desired version does not
   * exist
   */
  public File retrieveJarFile(String groupId, String artifactId, String _version, String classifier)
      throws RepositoryException {
    ArtifactIdentifier artifactIdentifier = resolveArtifactIdentfier(groupId, artifactId, _version,
        classifier);

    return provideFileForArtifact(artifactIdentifier);
  }

}
