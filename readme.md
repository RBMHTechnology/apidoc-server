[![Build Status](https://travis-ci.org/RBMHTechnology/apidoc-server.svg?branch=master)](https://travis-ci.org/RBMHTechnology/apidoc-server)

# ApiDoc Server

The ApiDoc Server is a web-application that transparently extracts api documentation artifacts from a Maven Repository and serves the content to the user. It is written in Java 8 and built on top of [Spring Boot](http://projects.spring.io/spring-boot/). The ApiDoc Server

- serves documentation artifacts from [JCenter](http://jcenter.bintray.com/), but can be configured to other repositories with optional basic authentication support
- transparently resolves the `latest` and `release` versions for artifacts using the information provided by the maven repository
- lets you browse through the list of available versions of an artifact specified by its `groupId` and `artifactId`
- supports all kinds of zip/jar packaged documentation artifacts like javadoc, groovydoc, scaladoc and others
- downloads all documentation artifacts to a temporary local storage, but can be configured to store in a dedicated location
- caches resolved and downloaded snapshot artifacts for 30 minutes, but can be configured to any other cache timeout
- serves all artifacts from the configured repository, but can be restricted by providing a whitelist of `groupId` prefixes
- can be restricted to serve release versions only.

## Usage

Using the ApiDoc Server is straight forward and very streamlined. The ApiDoc Server has following URL format specification:

```
http://<hostname>/{groupId}/{artifactId}/{version}/{classifier}
```

The URL parts have following meaning:

* `groupId` (mandatory): The group identifier of the artifact, e.g. `org.apache.commons`.
* `artifactId` (mandatory): The artifact identifier, e.g. `commons-lang3`.
* `version` (optional): The version identifier of the artifact, e.g. `3.3.2`. In case the version identifier is emitted, all available versions are listed.
* `classifier` (optional): The documentation classifier to be shown. Typical values are `javadoc`, `scaladoc`, `groovydoc` and so on. If omitted the configurable default value `javadoc` is used instead.

The `version` part supports two special version references, which are obtained from the corresponding `maven-metadata.xml`:

* `latest`: Resolves to the latest version available, this can be a snapshot or a release version.
* `release`: Resolves to the latest release version.

Leveraging `latest` or `release` you can create documentation links always pointing to the latest available API documentation.

## Configuration

As the ApiDoc Server is a Spring Boot application it makes use of its way to [externalize configuration](http://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/#boot-features-external-config). You may want to provide your property values in the command line by adding something like that`--<propertyname>=<propertyvalue>`.

### Local storage

Out of the box the ApiDoc Server will store its downloaded content in a temporary location, which is determined on system startup. With every startup you start with an empty local storage.

By setting the `localstorage` application property you can configure your persistent local storage.

```
java -jar apidoc-server-<version>.jar --localstorage=/my/apidocserver/storage
```

### Maven repository

[JCenter](http://jcenter.bintray.com/) is the default repository the ApiDoc Server will serve its content from.

By setting the `repository.url` application property you can point the ApiDoc Server to another repository, e.g. if you prefer [Maven Central](https://repo1.maven.org/maven2) you may want to start the following way

```
java -jar apidoc-server-<version>.jar --repository.url=https://repo1.maven.org/maven2
```

If the repository needs authentication (e.g. your corporate repository) you can provide the credentials by setting `repository.username` and `repository.password`.

```
java -jar apidoc-server-<version>.jar --repository.url=https://repository.your-corp.com --repository.username=user --repository.password=secret
```

### Advanced options

The ApiDoc Server will serve everything what the underlying maven repository provides.

#### Snapshot artifacts

In case that you only want to serve release artifacts, you can set the property `repository.snapshots.enabled` to false and no snapshot artifacts will be served anymore.

#### GroupId prefix whitelist

Maybe your repository serves a lot of artifacts, but you need the ApiDoc Server to serve just some artifacts belonging to special groups, you are able to specify that by providing a comma separated list of `groupId` prefixes to match against. If the requested groupId doesn't start with any of the configured prefixes it won't be served. The property is named `groupid-prefix-whitelist`.

```
java -jar apidoc-server-<version>.jar --groupid-prefix-whitelist=org.springframework
```

Starting with this option will restrict the access to groups starting with org.springframework, therefore access to org.springframework.cloud is granted.

#### Snapshot cache

Due to the nature of snapshots, the actual artifact will change frequently and the ApiDoc Server needs to check this from time to time. The default approach is to cache resolved snapshots for 30 minutes, after this time the artifact is being removed. With the next request the artifact is freshly resolved and downloaded. To tweak the cache timeout set the property `repository.snapshots.cache-timeout` to the appropriate amount of seconds.

#### Default Classifier

If no documentation classifier (e.g. like `javadoc`, `groovydoc`, `scaladoc`, and so on) is specified within the incoming request the ApiServer will use a default documentation classifier as fallback, which is `javadoc`. You can change the default documentation classifier from `javadoc` to any other value by setting the property `default.classifier`.

#### Naming

If you want to the give the ApiDoc Server a different name you can accomplish this by specifying the property `name` which defaults to ApiDoc Server.

## Building

The project makes use of [Gradle](http://gradle.org/) for building and the Gradle Wrapper for convenience and reproducibility of builds.

To build the project just execute `./gradlew build` and you will find the build artifact in the subdirectory `build/libs/apidoc-server-<version>.jar`.

To build and launch the ApiDoc Server directly from the sources you may want to execute `./gradlew bootRun` and point your browser to  [localhost:8080](http://localhost:8080/).

Unfortunately, you are not able to provide properties to the ApiDoc Server in way stated before. But you are able to provide the path to an application.properties file by setting the gradle project property `application.propeties` in the following way.

```
./gradlew bootRun -Papplication.properties=/path/to/my/application.properties
```
