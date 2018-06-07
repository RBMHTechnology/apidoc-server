package com.rbmhtechnology.apidocserver.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.service.mavenrepo.MavenRepoClient;
import io.vavr.collection.HashMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class RepositoryServiceTest {

  private RepositoryService repoService;

  @Rule
  public TemporaryFolder tmp = new TemporaryFolder();

  @Mock
  private MavenRepoClient client;

  @Before
  public void setup() throws IOException {
    final List<String> expectedClassifier = Arrays.asList("javadoc", "groovydoc");
    this.repoService = new RepositoryService("name", true, 0, tmp.newFolder(), expectedClassifier,
        client);
  }

  @Test
  public void classifiers() throws RepositoryException {
    when(client.exists(any())).thenReturn(HashMap.of(
        "javadoc", true,
        "groovydoc", false
    ));

    final List<String> classifier = repoService
        .getAvailableClassifier("foo", "bar", "1.0.0");

    assertThat(classifier).containsExactlyInAnyOrder("javadoc");
  }
}