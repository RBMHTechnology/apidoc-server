package com.rbmhtechnology.apidocserver.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ArtifactIdentifierTest {

  @Test
  public void mavenCoordinate() {
    final ArtifactIdentifier identifier = new ArtifactIdentifier("com.foo", "bar-baz", "1.0.0",
        "javadoc");
    assertThat(identifier.mavenLayout())
        .isEqualTo("com/foo/bar-baz/1.0.0/bar-baz-1.0.0-javadoc.jar");
  }

  @Test
  public void mavenCoordinate_with_type() {
    final ArtifactIdentifier identifier = new ArtifactIdentifier("com.foo", "bar-baz", "1.0.0",
        "protobuf", "zip");
    assertThat(identifier.mavenLayout())
        .isEqualTo("com/foo/bar-baz/1.0.0/bar-baz-1.0.0-protobuf.zip");
  }

  @Test
  public void mavenCoordinate_without_classifier_without_type_without_classifier() {
    final ArtifactIdentifier identifier = new ArtifactIdentifier("com.foo", "bar-baz", "1.0.0");
    assertThat(identifier.mavenLayout())
        .isEqualTo("com/foo/bar-baz/1.0.0/bar-baz-1.0.0.jar");
  }
}