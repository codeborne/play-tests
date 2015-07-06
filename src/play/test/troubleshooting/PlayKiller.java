package play.test.troubleshooting;

import play.Play;

import java.util.Date;

import static java.lang.System.currentTimeMillis;
import static play.test.PlayTestsRunner.log;

public class PlayKiller implements Runnable {
  public static final int EXPECTED_FIRST_TEST_EXECUTION_TIME = 40 * 1000;
  public static final int MAXIMUM_TEST_EXECUTION_TIME = 100 * 1000;

  private static Long timeToKillPlay;
  private static String requesterInfo;

  public static void scheduleKillPlay(String requester, long killAfterNMilliseconds) {
    if (Play.mode.isDev()) return;

    if (timeToKillPlay == null) {
      Thread thread = new Thread(new PlayKiller(), "Play killer thread");
      thread.setDaemon(true);
      thread.start();
    }

    timeToKillPlay = currentTimeMillis() + killAfterNMilliseconds;
    requesterInfo = "Scheduled Play! kill by " + requester + " at " + new Date() + " to " + new Date(timeToKillPlay);
  }

  @Override public void run() {
    while (!Thread.interrupted()) {
      if (timeToKillPlay != null && timeToKillPlay < currentTimeMillis()) {
        if (Play.started) {
          log("Stopping play! application \nRequested by: " + requesterInfo);
          Play.stop();
          log("Stopped play! application.");
          log("Stop tests.");
          System.exit(102);
        }
        else {
          log("Play! application is not started. Nothing to stop.");
        }
        break;
      }
      else {
        try {
          Thread.sleep(3000);
        }
        catch (InterruptedException e) {
          break;
        }
      }
    }
  }
}

