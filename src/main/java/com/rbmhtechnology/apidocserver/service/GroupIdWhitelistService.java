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

import static java.util.Collections.emptyList;

import com.google.common.base.Splitter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GroupIdWhitelistService {

  private final List<String> groupIdPrefixWhitelist;

  public GroupIdWhitelistService(
      @Value("${groupid-prefix-whitelist:#{null}}") String groupIdPrefixWhitelistString) {
    if (groupIdPrefixWhitelistString == null) {
      groupIdPrefixWhitelist = emptyList();
    } else {
      groupIdPrefixWhitelist = Splitter.on(",")
          .trimResults()
          .omitEmptyStrings()
          .splitToList(groupIdPrefixWhitelistString);
    }
  }

  public List<String> getGroupIdPrefixWhitelist() {
    return groupIdPrefixWhitelist;
  }

  public boolean isValidGroupId(String groupId) {
    return groupIdPrefixWhitelist.isEmpty() ||
        groupIdPrefixWhitelist.stream()
            .anyMatch(groupId::startsWith);
  }
}
