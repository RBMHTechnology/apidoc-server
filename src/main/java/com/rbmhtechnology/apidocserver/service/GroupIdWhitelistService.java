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

import com.google.common.base.Splitter;
import java.util.List;
import javax.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class GroupIdWhitelistService {

  @Value("${groupid-prefix-whitelist:#{null}}")
  private String groupIdPrefixWhitelistString;
  private List<String> groupIdPrefixWhitelist;

  @PostConstruct
  void init() {
    if (!StringUtils.isEmpty(groupIdPrefixWhitelistString)) {
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
    if (groupIdPrefixWhitelist == null) {
      return true;
    }

    for (String groupIdPrefix : groupIdPrefixWhitelist) {
      if (groupId.startsWith(groupIdPrefix)) {
        return true;
      }
    }
    return false;
  }

}
