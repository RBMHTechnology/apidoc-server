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
