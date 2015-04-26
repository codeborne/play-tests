package play.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.impl.Html;
import com.codeborne.selenide.junit.ScreenShooter;
import com.codeborne.selenide.logevents.PrettyReportCreator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import play.Play;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.mvc.Router;
import play.test.stats.ExecutionTimesWatcher;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;
import static play.test.PlayTestsRunner.scheduleKillPlay;

@RunWith(PlayTestsRunner.class)
public abstract class UITest extends Assert {
  public static int MAXIMUM_TEST_EXECUTION_TIME = 100 * 1000;
  public static int MAXIMUM_TEST_PREPARATION_TIME = 5 * 1000;

  @Rule public ScreenShooter makeScreenshotOnFailure = failedTests();
  @Rule public TestWatcher executionTimesWatcher = new ExecutionTimesWatcher();
  @Rule public PlayKiller playKiller = new PlayKiller();
  @Rule public TestRule prettyReportCreator = new PrettyReportCreator();

  @Before
  public void usePlayClassloader() {
    Thread.currentThread().setContextClassLoader(Play.classloader);
  }

  @Before
  public void startTransaction() {
    JPAPlugin.startTx(false);
  }

  @After
  public void closeTransaction() {
    JPAPlugin.closeTx(true);
  }

  public static String getLabel(String key) {
    return Messages.get(key);
  }

  public static String getLabel(String key, Object... args) {
    return Messages.get(key, args);
  }

  public static Condition haveLabel(final String key, final Object... args) {
    return new Condition("label") {
      @Override public boolean apply(WebElement element) {
        return Html.text.contains(element.getText(), Messages.get(key, args));
      }

      @Override public String toString() {
        return "have label " + Messages.get(key, args);
      }
    };
  }

  private static Condition action(final String action, final Map<String, String> args) {
    return new Condition("action " + action + " " + args) {
      @Override public boolean apply(WebElement element) {
        return expectedUrl().equals(actualUrl());
      }

      private String expectedUrl() {
        return Router.getFullUrl(action, new HashMap<String, Object>(args));
      }

      private String actualUrl() {
        return getWebDriver().getCurrentUrl().replaceFirst("\\?.*$", "");
      }

      @Override public String actualValue(WebElement element) {
        return "url: " + actualUrl() + ", expected url: " + expectedUrl() + ", expected action: " + action;
      }
    };
  }

  public static void assertAction(String action) {
    $("body").shouldHave(action(action, new HashMap<String, String>(0)));
  }

  public static void assertAction(String action, Map<String, String> args) {
    $("body").shouldHave(action(action, args));
  }

  public static void mockConfirm() {
    executeJavaScript("window.confirm = function() {return true;};");
  }

  public static void mockAlert() {
    getWebDriver().switchTo().alert().accept();
  }

  /**
   * Clears Play flash before opening some page
   */
  public void clearFlash() {
    String flashCookiePrefix = Play.configuration.getProperty("application.session.cookie", "PLAY");
    String flashCookie = flashCookiePrefix + "_FLASH";
    getWebDriver().manage().deleteCookieNamed(flashCookie);
  }

  private static class PlayKiller extends TestWatcher {
    @Override protected void starting(Description description) {
      if (Play.mode.isProd())
        scheduleKillPlay(description.getDisplayName(), MAXIMUM_TEST_EXECUTION_TIME);
    }

    @Override protected void finished(Description description) {
      if (Play.mode.isProd())
        scheduleKillPlay(description.getDisplayName(), MAXIMUM_TEST_PREPARATION_TIME);
    }
  }
}
