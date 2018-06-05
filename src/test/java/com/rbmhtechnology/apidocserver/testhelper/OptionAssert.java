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
