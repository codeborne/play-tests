package play.test.mock;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

public class Reflection {
  public static <T> T newInstanceOf(Class<T> clazz) {
    try {
      Constructor<T> constructor = clazz.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static void setField(Class<?> clazz, String name, Object value) {
    try {
      Field field = clazz.getDeclaredField(name);
      field.setAccessible(true);
      field.set(null, value);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @SuppressWarnings("unchecked")
  public static void setThreadLocalField(Class<?> clazz, String name, Object value) {
    try {
      Field threadLocal = clazz.getDeclaredField(name);
      threadLocal.setAccessible(true);
      ((ThreadLocal) threadLocal.get(null)).set(value);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
