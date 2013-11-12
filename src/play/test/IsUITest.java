package play.test;

import com.google.common.base.Predicate;

public class IsUITest implements Predicate<Class> {
  @Override public boolean apply(Class c) {
    return UITest.class.isAssignableFrom(c);
  }

  @Override public String toString() {
    return "UI tests";
  }
}