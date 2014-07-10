package play.test.stats;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

public class ExecutionTimesWatcher extends TestWatcher {
  public static final ExecutionTimes times = new ExecutionTimes();
  
  private long startTime;
  
  @Override protected void starting(Description d) {
    startTime = System.currentTimeMillis();
  }

  @Override protected void finished(Description d) {
    times.add(d.getClassName(), d.getMethodName(), System.currentTimeMillis() - startTime);
  }
}
