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

import java.util.Objects;

/**
 * Class containing the coordinates to a maven repository artifact
 */
public class ArtifactIdentifier {

  public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String classifier;
  private final String type;

  /**
   * constructs an ArtifactIdentifier for the given coordinates
   *
   * @param groupId group id of the artifact
   * @param artifactId artifact id of the artifact
   * @param version version of the artifact
   * @param classifier classifier of the artifact
   */
  public ArtifactIdentifier(String groupId, String artifactId, String version, String classifier) {
    this(groupId, artifactId, version, classifier, null);
  }


  public ArtifactIdentifier(String groupId, String artifactId, String version) {
    this(groupId, artifactId, version, null, null);
  }

  public ArtifactIdentifier(String groupId, String artifactId, String version, String classifier,
      String type) {
    Objects.requireNonNull(groupId);
    Objects.requireNonNull(artifactId);
    Objects.requireNonNull(version);
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.classifier = classifier;
    this.type = type == null ? "jar" : type;
  }

  @Override
  public String toString() {
    return groupId + ":" + artifactId + ":" + version + ":" + classifier;
  }

  public String getGroupId() {
    return groupId;
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getVersion() {
    return version;
  }

  public String getClassifier() {
    return classifier;
  }

  /**
   * @return true if the version is a snapshot version
   */
  public boolean isSnapshot() {
    return version.endsWith(SNAPSHOT_SUFFIX);
  }

  public String mavenLayout() {
    return groupId.replace('.', '/') + "/"
        + artifactId + "/"
        + version + "/"
        + artifactId + "-" + version + (classifier == null ? "" : ("-" + classifier)) + "." + type;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
    result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ArtifactIdentifier other = (ArtifactIdentifier) obj;
    if (groupId == null) {
      if (other.groupId != null) {
        return false;
      }
    } else if (!groupId.equals(other.groupId)) {
      return false;
    }
    if (artifactId == null) {
      if (other.artifactId != null) {
        return false;
      }
    } else if (!artifactId.equals(other.artifactId)) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    if (classifier == null) {
      if (other.classifier != null) {
        return false;
      }
    } else if (!classifier.equals(other.classifier)) {
      return false;
    }
    return true;
  }
}
