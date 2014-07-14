package play.test.stats;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ExecutionTimesWatcher extends TestWatcher {
  public static final ExecutionTimes times = new ExecutionTimes();
  
  private long startTimeNs;
  
  @Override protected void starting(Description d) {
    startTimeNs = System.nanoTime();
  }

  @Override protected void finished(Description d) {
    times.add(d.getClassName(), d.getMethodName(), System.nanoTime() - startTimeNs);
  }
}
