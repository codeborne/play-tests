package play.test.stats;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ExecutionTimesTest {
  ExecutionTimes times = new ExecutionTimes();

  @Test
  public void calculatesExecutionTimesByMethod() {
    times.add("com.Test1", "method1", 100000000L);
    times.add("com.Test1", "method2", 200000000L);
    times.add("com.Test1", "method3", 800000000L);
    times.add("com.Test1", "method4", 800000000L);
    times.add("com.Test2", "method1", 300000000L);

    assertEquals("Longest methods:\n" +
        "     800 ms com.Test1.method3\n" +
        "     800 ms com.Test1.method4\n" +
        "     300 ms com.Test2.method1\n" +
        "     200 ms com.Test1.method2\n" +
        "     100 ms com.Test1.method1\n",
        times.longestMethods());
  }

  @Test
  public void calculatesExecutionTimesByClass() {
    times.add("com.Test1", "method1", 100000000L);
    times.add("com.Test1", "method2", 200000000L);
    times.add("com.Test1", "method3", 800000000L);
    times.add("com.Test1", "method4", 800000000L);
    times.add("com.Test2", "method1", 300000000L);

    assertEquals("Longest classes:\n" +
        "    1900 ms com.Test1\n" +
        "     300 ms com.Test2\n",
        times.longestClasses());
  }
}