package play.test;

import com.google.common.base.Predicate;

public enum TestType {
  UNIT,
  UI;

  public Predicate<Class> getFilter() {
    return this == UNIT ? new IsUnitTest() : new IsUITest();
  }
}