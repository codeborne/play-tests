package play.test.troubleshooting;

import play.Play;

import java.io.File;
import java.io.IOException;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.Date;

import static java.lang.System.currentTimeMillis;
import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.lang.management.ManagementFactory.getThreadMXBean;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static play.test.PlayTestsRunner.log;

public class ThreadDumper implements Runnable {
  private static Long timeToStartDumping;
  private static String requesterInfo;

  public static void scheduleThreadDump(String requester, long expectedStartingTimeInMilliseconds) {
    if (Play.mode.isDev()) return;

    if (timeToStartDumping == null) {
      Thread thread = new Thread(new ThreadDumper(), "Thread dumper thread");
      thread.setDaemon(true);
      thread.start();
    }

    timeToStartDumping = currentTimeMillis() + expectedStartingTimeInMilliseconds;
    requesterInfo = "Scheduled Play! thread dump by " + requester  + " at " + new Date() + " to " + new Date(timeToStartDumping);
  }

  @Override public void run() {
    while (!Thread.interrupted()) {
      if (timeToStartDumping != null && timeToStartDumping < currentTimeMillis()) {
        File threadDump = storeDumpToFile(takeThreadDump());
        if (threadDump != null)
          log("Stored play! application thread dump to " + threadDump.getAbsolutePath() + "\nRequested by: " + requesterInfo);
      }

      try {
        Thread.sleep(1000);
      }
      catch (InterruptedException e) {
        break;
      }
    }
  }

  private static String takeThreadDump() {
    StringBuilder dump = new StringBuilder();
    ThreadMXBean threadMXBean = getThreadMXBean();
    ThreadInfo[] threadInfos = threadMXBean.getThreadInfo(threadMXBean.getAllThreadIds(), 100);
    for (ThreadInfo threadInfo : threadInfos) {
      dump.append('"');
      dump.append(threadInfo.getThreadName());
      dump.append("\" ");
      Thread.State state = threadInfo.getThreadState();
      dump.append("\n   java.lang.Thread.State: ");
      dump.append(state);
      final StackTraceElement[] stackTraceElements = threadInfo.getStackTrace();
      for (final StackTraceElement stackTraceElement : stackTraceElements) {
        dump.append("\n        at ");
        dump.append(stackTraceElement);
      }
      dump.append("\n\n");
    }
    return dump.toString();
  }

  private File storeDumpToFile(String threadDump) {
    File file = new File("test-result/thread-dump-" + getRuntimeMXBean().getName() + '-' + System.currentTimeMillis() + ".dump");
    try {
      writeStringToFile(file, threadDump);
      return file;
    }
    catch (IOException e) {
      System.err.println("Failed to store statistics to " + file.getAbsolutePath() + ": " + e);
      return null;
    }
  }
}
