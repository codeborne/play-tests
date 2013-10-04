package play.test;

import com.google.common.base.Predicate;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class IsTestClass implements Predicate<Class> {
  @Override public boolean apply(Class c) {
    if (Modifier.isAbstract(c.getModifiers())) {
      return false;
    }

    for (Method m : c.getMethods()) {
      if (m.isAnnotationPresent(Test.class)) {
        return true;
      }
    }

    return false;
  }
}