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

package com.rbmhtechnology.apidocserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class ApiDocServer {

  private static final Logger LOG = LoggerFactory.getLogger(ApiDocServer.class);

  public static void main(String[] args) throws UnknownHostException {
    final SpringApplication app = new SpringApplication(ApiDocServer.class);
    final Environment env = app.run(args).getEnvironment();
    final String hostAddress = InetAddress.getLocalHost().getHostAddress();
    final String serverPort = env.getProperty("server.port", "8080");
    LOG.info(
        "Application URLs:\n----------------------------------------------------------\n\t"
            + "Local: \t\thttp://127.0.0.1:{}\n\t"
            + "External: \thttp://{}:{}\n----------------------------------------------------------",
        serverPort,
        hostAddress,
        serverPort);

    final String managementPort = env.getProperty("management.port", "8080");
    LOG.info(
        "Management URLs:\n----------------------------------------------------------\n\t"
            + "Local: \t\thttp://127.0.0.1:{}/health\n\t"
            + "External: \thttp://{}:{}/health\n----------------------------------------------------------",
        managementPort,
        hostAddress,
        managementPort);
  }
}
