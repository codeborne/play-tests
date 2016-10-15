package play.test;

import org.openqa.selenium.*;
import org.openqa.selenium.support.events.AbstractWebDriverEventListener;
import play.test.stats.ExecutionTimes;

import java.util.Arrays;

import static org.apache.commons.lang.StringUtils.substring;

public class UITimeLogger extends AbstractWebDriverEventListener {
  
  public final ExecutionTimes times = new ExecutionTimes();
  
  private long startNs;
  private String command;
  private String arg;
  
  @Override public void beforeFindBy(By by, WebElement context, WebDriver driver) {
    before("find", by.toString());
  }

  @Override public void afterFindBy(By by, WebElement context, WebDriver driver) {
    after("find", by.toString());
  }

  @Override public void beforeClickOn(WebElement element, WebDriver driver) {
    before("click", toString("click", element));
  }

  @Override public void afterClickOn(WebElement element, WebDriver driver) {
    after("click", toString("click", element));
  }

  @Override public void beforeChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {
    before("changeValue", toString("changeValue", element) + " (" + Arrays.toString(keysToSend) + ")");
  }

  @Override public void afterChangeValueOf(WebElement element, WebDriver driver, CharSequence[] keysToSend) {
    after("changeValue",toString("changeValue", element) + " (" + Arrays.toString(keysToSend) + ")");
  }

  @Override public void beforeScript(String script, WebDriver driver) {
    before("script", substring(script, 0, 50));
  }

  @Override public void afterScript(String script, WebDriver driver) {
    after("script", substring(script, 0, 50));
  }

  @Override public void beforeNavigateTo(String url, WebDriver driver) {
    before("navigate", url);
  }

  @Override public void afterNavigateTo(String url, WebDriver driver) {
    after("navigate", url);
  }

  private void before(String command, String arg) {
    startNs = System.nanoTime();
    this.command = command;
    this.arg = arg;
  }

  private void after(String command, String arg) {
    long duration = System.nanoTime() - startNs;

    if (!command.equals(this.command) || !arg.equals(this.arg) && !"?".equals(arg)) {
      System.out.println("info: started " + this.command + "(" + this.arg + "), ended " + command + "(" + arg + ")");
    }

    times.add(command, arg, duration);
  }

  private String toString(String command, WebElement element) {
    try {
      return element.getTagName() + '#' + element.getAttribute("id") + '.' + element.getAttribute("class");
    }
    catch (StaleElementReferenceException disappeared) {
      return command.equals(this.command) ? this.arg : "?";
    }
    catch (UnhandledAlertException ignoreAlert) {
      return command.equals(this.command) ? this.arg : "?";
    }
    catch (Exception e) {
      return e.getClass().getName() + ": "+ e.getMessage();
    }
  }
}
