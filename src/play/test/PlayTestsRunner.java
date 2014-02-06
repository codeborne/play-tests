package play.test;

import org.junit.rules.MethodRule;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.manipulation.Filterable;
import org.junit.runner.manipulation.NoTestsRemainException;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.JUnit4;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import play.Invoker;
import play.Play;

import java.io.File;

public class PlayTestsRunner extends Runner implements Filterable {
  private Class testClass;
  private JUnit4 jUnit4;

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

    testClass = Play.classloader.loadApplicationClass(testClass.getName());
    try {
      jUnit4 = new JUnit4(testClass);
    }
    catch (InitializationError initializationError) {
      throw new RuntimeException(initializationError);
    }

    jUnit4.run(notifier);
  }

  protected void startPlayIfNeeded() {
    synchronized (Play.class) {
      if (!Play.started) {
        Play.init(new File("."), getPlayId());
        Play.javaPath.add(Play.getVirtualFile("test"));
        if (!Play.started) {
          Play.start();
        }
      }
    }
  }

  @Override
  public void filter(Filter toFilter) throws NoTestsRemainException {
    jUnit4.filter(toFilter);

  }

  public static class PlayContextTestInvoker implements MethodRule {
    @Override public Statement apply(final Statement base, FrameworkMethod method, Object target) {

      return new Statement() {

        @Override
        public void evaluate() throws Throwable {
          try {
            Invoker.invokeInThread(new Invoker.DirectInvocation() {

              @Override
              public void execute() throws Exception {
                try {
                  base.evaluate();
                }
                catch (Throwable e) {
                  throw new RuntimeException(e);
                }
              }

              @Override
              public Invoker.InvocationContext getInvocationContext() {
                return new Invoker.InvocationContext(invocationType);
              }
            });
          }
          catch (Throwable e) {
            throw getRootCause(e);
          }
        }
      };
    }

    private Throwable getRootCause(Throwable e) {
      Throwable cause = e;
      while (cause.getCause() != null && cause != cause.getCause()) cause = e.getCause();
      return cause;
    }
  }
}
