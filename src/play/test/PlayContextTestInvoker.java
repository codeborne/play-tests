package play.test;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import play.Invoker;
import play.exceptions.UnexpectedException;

public class PlayContextTestInvoker implements MethodRule {
  @Override public Statement apply(final Statement base, final FrameworkMethod method, Object target) {

    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        try {
          Invoker.invokeInThread(new Invoker.DirectInvocation() {

            @Override
            public void execute() throws Exception {
              try {
                base.evaluate();
              } catch (Throwable e) {
                throw new RuntimeException(e);
              }
            }

            @Override
            public Invoker.InvocationContext getInvocationContext() {
              return new Invoker.InvocationContext(invocationType);
            }
          });
        }
        catch (UnexpectedException e) {
          throw e.getCause();
        }
      }
    };
  }
}