package play.test;

import com.codeborne.selenide.Configuration;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.InitializationError;
import play.Play;
import play.server.Server;

import java.io.File;

import static org.openqa.selenium.net.PortProber.findFreePort;

public class PlayTestsRunner extends Runner implements Filterable {
  private Class testClass;
  private JUnit4 jUnit4;
  private Filter filter;

  public PlayTestsRunner(Class testClass) throws ClassNotFoundException, InitializationError {
    this.testClass = testClass;
    jUnit4 = new JUnit4(testClass);
  }

  private static String getPlayId() {
    String playId = System.getProperty("play.id", "test");
    if(! (playId.startsWith("test-") && playId.length() >= 6)) {
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
    startPlayIfNeeded();
    loadTestClassWithPlayClassloader();
    jUnit4.run(notifier);
  }

  private void loadTestClassWithPlayClassloader() {
    testClass = Play.classloader.loadApplicationClass(testClass.getName());
    try {
      jUnit4 = new JUnit4(testClass);
      if (filter != null) {
        jUnit4.filter(filter);
      }
    }
    catch (InitializationError initializationError) {
      throw new RuntimeException(initializationError);
    }
    catch (NoTestsRemainException itCannotHappen) {
      throw new RuntimeException(itCannotHappen);
    }
  }

  protected void startPlayIfNeeded() {
    boolean isPlayStartNeeded = !"false".equalsIgnoreCase(System.getProperty("selenide.play.start", "true"));

    synchronized (Play.class) {
      if (isPlayStartNeeded && !Play.started) {
        Play.usePrecompiled = "true".equalsIgnoreCase(System.getProperty("precompiled", "false"));
        Play.init(new File("."), getPlayId());
        Play.javaPath.add(Play.getVirtualFile("test"));
        if (!Play.started) {
          Play.start();
        }

        int port = findFreePort();
        new Server(new String[]{"--http.port=" + port});

        Configuration.baseUrl = "http://localhost:" + port;
        Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
      }
    }
  }

  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    this.filter = filter;
    jUnit4.filter(filter);
  }
}
