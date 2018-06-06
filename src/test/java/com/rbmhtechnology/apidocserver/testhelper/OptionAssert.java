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
package com.rbmhtechnology.apidocserver.testhelper;

import io.vavr.control.Option;
import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Condition;

public class OptionAssert<T> extends AbstractAssert<OptionAssert<T>, Option<T>> {

  public Condition<Option<T>> isDefined = new Condition<>(
      Option::isDefined,
      "Option must be defined"
  );

  private OptionAssert(Option option) {
    super(option, OptionAssert.class);
  }

  public static <T> OptionAssert<T> assertThat(Option<T> option) {
    return new OptionAssert<>(option);
  }

  public OptionAssert<T> isDefined() {
    isNotNull();
    is(isDefined);
    return this;
  }

  public OptionAssert<T> isNotDefined() {
    isNotNull();
    isNot(isDefined);
    return this;
  }

}
