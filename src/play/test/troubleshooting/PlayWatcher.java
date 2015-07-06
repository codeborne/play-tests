package play.test.troubleshooting;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import static play.test.troubleshooting.PlayKiller.*;
import static play.test.troubleshooting.ThreadDumper.scheduleThreadDump;

public class PlayWatcher extends TestWatcher {
  private static final int EXPECTED_TEST_EXECUTION_TIME = 20 * 1000;
  private static final int MAXIMUM_TEST_PREPARATION_TIME = 5 * 1000;

  private boolean firstTest = true;

  @Override protected void starting(Description description) {
    scheduleThreadDump(description.getDisplayName(), firstTest ? EXPECTED_FIRST_TEST_EXECUTION_TIME : EXPECTED_TEST_EXECUTION_TIME);
    scheduleKillPlay(description.getDisplayName(), MAXIMUM_TEST_EXECUTION_TIME);
    firstTest = false;
  }

  @Override protected void finished(Description description) {
    scheduleKillPlay(description.getDisplayName(), MAXIMUM_TEST_PREPARATION_TIME);
  }
}