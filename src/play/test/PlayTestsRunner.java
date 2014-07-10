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
import play.i18n.Lang;
import play.server.Server;

import java.io.File;

import static com.codeborne.selenide.Selenide.open;
import static java.lang.System.currentTimeMillis;
import static org.openqa.selenium.net.PortProber.findFreePort;

public class PlayTestsRunner extends Runner implements Filterable {
  private Class testClass;
  private JUnit4 jUnit4;
  private Filter filter;

  public PlayTestsRunner(Class testClass) throws InitializationError {
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
    boolean firstRun = startPlayIfNeeded();
    loadTestClassWithPlayClassloader();
    Lang.clear();
    
    if (firstRun) open("/");
    
    jUnit4.run(notifier);
  }

  private void loadTestClassWithPlayClassloader() {
    Class precompiledTestClass = Play.classloader.loadApplicationClass(testClass.getName());
    if (precompiledTestClass == null) {
      System.err.println("Warning: test classes are not precompiled. May cause problems if using JPA in tests.");
      return;
    }

    try {
      testClass = precompiledTestClass;
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

  protected boolean startPlayIfNeeded() {
    boolean isPlayStartNeeded = !"false".equalsIgnoreCase(System.getProperty("selenide.play.start", "true"));

    synchronized (Play.class) {
      if (isPlayStartNeeded && !Play.started) {
        long start = currentTimeMillis();
        
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

        long end = currentTimeMillis();
        System.out.println("Started Play! application in " + (end - start) + " ms.");
        
        return true;
      }
    }
    return false;
  }

  @Override
  public void filter(Filter filter) throws NoTestsRemainException {
    this.filter = filter;
    jUnit4.filter(filter);
  }
}
