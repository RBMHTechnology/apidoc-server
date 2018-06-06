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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class GroupIdWhitelistServiceTest {

  @Test
  public void creating_service_with_empty_string_results_in_empty_list() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService("");
    assertThat(service.getGroupIdPrefixWhitelist()).isEmpty();
  }

  @Test
  public void creating_service_with_null_string_results_in_empty_list() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService(null);
    assertThat(service.getGroupIdPrefixWhitelist()).isEmpty();
  }

  @Test
  public void creating_service_with_two_groups_results_in_list_with_two_entries() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService("com.foo,com.bar");
    assertThat(service.getGroupIdPrefixWhitelist()).hasSize(2);
  }

  @Test
  public void creating_service_with_two_groups_and_whitespace_results_in_list_with_two_entries() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService("com.foo, com.bar");
    assertThat(service.getGroupIdPrefixWhitelist()).hasSize(2);
  }

  @Test
  public void creating_service_with_empty_string_all_groups_are_allowed() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService("");
    assertThat(service.isValidGroupId("foo")).isTrue();
  }

  @Test
  public void creating_service_with_null_string_all_groups_are_allowed() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService(null);
    assertThat(service.isValidGroupId("foo")).isTrue();
  }

  @Test
  public void creating_service_with_two_groups_results_in_only_configured_groups_valid() {
    final GroupIdWhitelistService service = new GroupIdWhitelistService("com.foo,com.bar");
    assertThat(service.isValidGroupId("com.foo")).isTrue();
    assertThat(service.isValidGroupId("com.baz")).isFalse();
  }
}