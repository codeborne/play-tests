package play.test.mock;

import play.cache.Cache;
import play.cache.CacheImpl;
import play.data.validation.Validation;
import play.mvc.Controller;

import static org.mockito.Mockito.mock;
import static play.test.mock.Reflection.*;

public class Mock {
  public static void cache() {
    Cache.cacheImpl = mock(CacheImpl.class);
  }

  public static Validation validation() {
    Validation validation = newInstanceOf(Validation.class);
    setThreadLocalField(Validation.class, "current", validation);
    setField(Controller.class, "validation", validation);
    return validation;
  }
}
