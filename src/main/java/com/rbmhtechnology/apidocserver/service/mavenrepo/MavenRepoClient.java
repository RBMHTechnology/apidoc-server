package com.rbmhtechnology.apidocserver.service.mavenrepo;

import com.google.common.io.ByteStreams;
import com.rbmhtechnology.apidocserver.exception.DownloadException;
import com.rbmhtechnology.apidocserver.exception.NotFoundException;
import com.rbmhtechnology.apidocserver.exception.RepositoryException;
import com.rbmhtechnology.apidocserver.exception.StorageException;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import org.springframework.stereotype.Component;

@Component
public class MavenRepoClient {

  private static final Logger LOG = LoggerFactory.getLogger(MavenRepoClient.class);

  private final CloseableHttpClient httpClient;

  public MavenRepoClient(MavenRepositoryConfig config) {
    final CredentialsProvider credsProvider = new BasicCredentialsProvider();
    config.getCredentials().peek(credentials -> credsProvider.setCredentials(
        new AuthScope(config.repositoryHost(), config.repositoryPort()),
        new UsernamePasswordCredentials(credentials.username(), credentials.password())
    ));
    this.httpClient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
  }

  public void download(String downloadUrl, File file) throws RepositoryException {
    LOG.debug("Started downloading '{}' to '{}'", downloadUrl, file);

    // executing HTTP get
    try (CloseableHttpResponse response = httpClient.execute(new HttpGet(downloadUrl))) {
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
}
