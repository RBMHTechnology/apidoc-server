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

import static com.rbmhtechnology.apidocserver.service.RepositoryService.MavenVersionRef.RELEASE;
import static java.util.Comparator.reverseOrder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.rbmhtechnology.apidocserver.exception.DownloadException;
import com.rbmhtechnology.apidocserver.exception.NotFoundException;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.exception.StorageException;
import com.rbmhtechnology.apidocserver.exception.VersionNotFoundException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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

  private static final String JCENTER = "http://jcenter.bintray.com";

  private static final Logger LOG = LoggerFactory.getLogger(RepositoryService.class);

  @Value("${name:ApiDoc Server}")
  private String name;

  @Value("${default.classifier:javadoc}")
  private String defaultClassifier;

  @Value("${repository.url:" + JCENTER + "}")
  private URL repositoryUrl;

  @Value("${repository.username:#{null}}")
  private String repositoryUser;

  @Value("${repository.password:#{null}}")
  private String repositoryPassword;

  @Value("${repository.snapshots.enabled:true}")
  private boolean snapshotsEnabled;

  @Value("${repository.snapshots.cache-timeout:1800}")
  private int snapshotsCacheTimeoutSeconds;

  @Value("${localstorage:#{null}}")
  private File localJarStorage;

  private LoadingCache<ArtifactIdentifier, File> snapshotDownloadUrlCache;
  private LoadingCache<ArtifactIdentifier, File> releaseDownloadUrlCache;

  private LoadingCache<GroupArtifactCacheKey, String> latestVersionCache;
  private LoadingCache<GroupArtifactCacheKey, String> releaseVersionCache;

  private CloseableHttpClient httpclient;

  public enum MavenVersionRef {
    LATEST("latest"), RELEASE("release");

    String xmlElementName;

    private MavenVersionRef(String xmlElementName) {
      this.xmlElementName = xmlElementName;
    }

    public String getXmlElementName() {
      return xmlElementName;
    }

  }

  @PostConstruct
  void init() {
    ArtifactLoader cacheLoader = new ArtifactLoader();
    SnapshotRemovalListener removalListener = new SnapshotRemovalListener();
    // snapshots will expire 30 minutes after their last construction (same for all)
    snapshotDownloadUrlCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(snapshotsCacheTimeoutSeconds, TimeUnit.SECONDS)
        .removalListener(removalListener)
        .build(cacheLoader);
    latestVersionCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .expireAfterWrite(snapshotsCacheTimeoutSeconds, TimeUnit.SECONDS)
        .build(new MavenXmlVersionRefResolver(MavenVersionRef.LATEST));

    releaseDownloadUrlCache = CacheBuilder.newBuilder().maximumSize(1000).build(cacheLoader);
    releaseVersionCache = CacheBuilder.newBuilder()
        .maximumSize(1000)
        .build(new MavenXmlVersionRefResolver(RELEASE));

    if (localJarStorage == null) {
      localJarStorage = Files.createTempDir();
    }

    // http client
    CredentialsProvider credsProvider = new BasicCredentialsProvider();
    if (!StringUtils.isEmpty(repositoryUser) && !StringUtils.isEmpty(repositoryPassword)) {
      credsProvider.setCredentials(new AuthScope(repositoryUrl.getHost(), repositoryUrl.getPort()),
          new UsernamePasswordCredentials(repositoryUser, repositoryPassword));
    }
    httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
  }

  @PreDestroy
  void destroy() {
    if (httpclient != null) {
      try {
        httpclient.close();
      } catch (IOException e) {
        LOG.warn("Error closing httpclient on destroy", e);
      }
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
    String downloadUrl = repositoryUrl + "/" + groupId.replace(".", "/") + "/"
        + artifactId + "/" + "maven-metadata.xml";

    download(downloadUrl, mavenMetadataXmlFile);
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
    String downloadUrl = repositoryUrl + "/"
        + artifactIdentifier.getGroupId().replace(".", "/") + "/"
        + artifactIdentifier.getArtifactId() + "/" + artifactIdentifier.getVersion()
        + "/" + "maven-metadata.xml";

    download(downloadUrl, mavenMetadataXmlFile);

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

  private void download(String downloadUrl, File file) throws RepositoryException {
    LOG.debug("Started downloading '{}' to '{}'", downloadUrl, file);

    // executing HTTP get
    try (CloseableHttpResponse response = httpclient.execute(new HttpGet(downloadUrl))) {
      // checking status
      StatusLine statusLine = response.getStatusLine();
      switch (statusLine.getStatusCode()) {
        case HttpStatus.SC_OK:
          storeResponseIntoFile(response, file);
          break;

        case HttpStatus.SC_NOT_FOUND:
          throw new NotFoundException(
              "No jar at '" + downloadUrl + "', failed with status:" + statusLine);
        case HttpStatus.SC_UNAUTHORIZED:
          throw new DownloadException("Access denied for " + downloadUrl + "', failed with status:"
              + statusLine);
        default:
          throw new DownloadException(
              "Downloading '" + downloadUrl + "' failed with status: " + statusLine);
      }
    } catch (IOException e) {
      throw new DownloadException("Error downloading '" + downloadUrl + "' failed", e);
    }

    LOG.debug("Finished downloading '{}' to '{}'", downloadUrl, file);
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

  private void storeResponseIntoFile(CloseableHttpResponse response, File file)
      throws RepositoryException {
    // create parent directory for downloaded artifact jar
    File parentDirectory = file.getParentFile();
    if (!parentDirectory.exists()) {
      if (!parentDirectory.mkdirs()) {
        throw new StorageException(
            "Could not create parent directory '" + parentDirectory.getAbsolutePath()
                + "'");
      }
    }
    HttpEntity entity = response.getEntity();

    try (InputStream in = entity.getContent();
        OutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
      ByteStreams.copy(in, out);
    } catch (IllegalStateException | IOException e) {
      throw new StorageException("Error storing ", e);
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

      String downloadUrl = repositoryUrl + "/"
          + artifactIdentifier.getGroupId().replace(".", "/") + "/"
          + artifactIdentifier.getArtifactId() + "/" + artifactIdentifier.getVersion()
          + "/" + documentationFilename;

      LOG.debug("Resolved download url for '{}' to '{}'", artifactIdentifier, downloadUrl);

      File file = constructJarFileLocation(artifactIdentifier);
      download(downloadUrl, file);

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
