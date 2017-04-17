package play.test;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import org.apache.log4j.Appender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.PatternLayout;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import org.slf4j.bridge.SLF4JBridgeHandler;
import play.Logger;
import play.Play;
import play.i18n.Lang;
import play.server.Server;
import play.test.coverage.ActionCoveragePlugin;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.google.common.io.Resources.toByteArray;
import static java.lang.System.currentTimeMillis;
import static org.openqa.selenium.net.PortProber.findFreePort;
import static play.Play.Mode.DEV;
import static play.Play.Mode.PROD;
import static play.test.troubleshooting.PlayKiller.EXPECTED_FIRST_TEST_EXECUTION_TIME;
import static play.test.troubleshooting.ThreadDumper.scheduleThreadDump;

public class PlayTestsRunner extends Runner implements Filterable {
  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  private JUnit4 jUnit4;

  public PlayTestsRunner(Class testClass) throws InitializationError {
    jUnit4 = new JUnit4(testClass);
  }

  private static String getPlayId() {
    String playId = System.getProperty("play.id", "test");
    if (!(playId.startsWith("test-") && playId.length() >= 6)) {
      playId = "test";
    }
    return playId;
  }

  @Override
  public Description getDescription() {
    return jUnit4.getDescription();
  }

  @Override
  public void run(final RunNotifier notifier) {
    try {
      boolean firstRun = startPlayIfNeeded();
      Lang.clear();

      if (firstRun) {
        warmupApplication();
        addTimesLogger();
      }
    }
    catch (Throwable failedToStartPlay) {
      log("Failed to start play:");
      failedToStartPlay.printStackTrace();
      log("Stop tests.");
      notifier.pleaseStop();
      System.exit(101);
    }

    jUnit4.run(notifier);
  }

  public static void log(String message) {
    System.out.println("-------------------------------\n" +
        new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date()) + " " + message + "\n" +
        "-------------------------------\n");
  }

  private void addTimesLogger() {
    if (Play.mode.isProd() && "true".equals(System.getProperty("selenide.play.calculate-webdriver-statistics", "false"))) {
      ActionCoveragePlugin.timeLogger = new UITimeLogger();
      WebDriverRunner.addListener(ActionCoveragePlugin.timeLogger);
    }
  }

  private void warmupApplication() {
    try {
      toByteArray(new URL(Configuration.baseUrl));
    } catch (IOException e) {
      System.err.println("Failed to load URL " + Configuration.baseUrl + ":");
      e.printStackTrace();
    }
  }

  @SuppressWarnings("CallToNativeMethodWhileLocked")
  protected boolean startPlayIfNeeded() throws InterruptedException {
    synchronized (PlayTestsRunner.class) {
      if (isPlayStartNeeded() && !Play.started) {
        TimeZone.setDefault(TimeZone.getTimeZone(System.getProperty("selenide.play.timeZone", "Asia/Krasnoyarsk")));

        long start = currentTimeMillis();
        makeUniqueCachePath();

        Play.usePrecompiled = "true".equalsIgnoreCase(System.getProperty("precompiled", "false"));
        Play.mode = Play.usePrecompiled ? PROD : DEV;

        scheduleThreadDump("PlayTestRunner.startPlayIfNeeded", EXPECTED_FIRST_TEST_EXECUTION_TIME);

        Thread playStarter = new Thread(new Runnable() {
          @Override public void run() {
            Play.init(new File(System.getProperty("application.path", ".")), getPlayId());
            if (!Play.started) {
              Play.start();
            }

            int port = findFreePort();
            new Server(new String[]{"--http.port=" + port});

            Configuration.baseUrl = "http://localhost:" + port;
            Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
          }
        }, "Play! starter thread");
        playStarter.start();
        playStarter.join();

        duplicateLogsOfEveryTestProcessToSeparateFile();

        long end = currentTimeMillis();
        log("Started Play! application in " + (end - start) + " ms.");
        
        return true;
      }
    }
    return false;
  }

  private boolean isPlayStartNeeded() {
    return !"false".equalsIgnoreCase(System.getProperty("selenide.play.start", "true"));
  }

  void makeUniqueCachePath() {
    try {
      Path tmp = Files.createTempDirectory("cache");
      System.setProperty("java.io.tmpdir", tmp.toString());
    }
    catch (IOException e) {
      throw new RuntimeException("Failed to create unique directory for cache", e);
    }
  }

  private void duplicateLogsOfEveryTestProcessToSeparateFile() {
    Logger.log4j = org.apache.log4j.Logger.getLogger("play");
    String logFileName = "test-result/" + ManagementFactory.getRuntimeMXBean().getName() + ".log";
    org.apache.log4j.Logger rootLogger = org.apache.log4j.Logger.getRootLogger();
    try {
      Appender testLog = new FileAppender(new PatternLayout("%d{DATE} %-5p ~ %m%n"), Play.getFile(logFileName).getAbsolutePath(), false);
      rootLogger.addAppender(testLog);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    jUnit4.filter(filter);
  }
}
