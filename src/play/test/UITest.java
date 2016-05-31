package play.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.impl.Html;
import com.codeborne.selenide.junit.ScreenShooter;
import com.codeborne.selenide.junit.TextReport;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.test.stats.ExecutionTimesWatcher;
import play.test.troubleshooting.PlayWatcher;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.WebDriverRunner.hasWebDriverStarted;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;

@RunWith(PlayTestsRunner.class)
public abstract class UITest extends Assert {
  @Rule public ScreenShooter makeScreenshotOnFailure = failedTests();
  @Rule public TestWatcher executionTimesWatcher = new ExecutionTimesWatcher();
  @Rule public PlayWatcher playWatcher = new PlayWatcher();
  @Rule public TestRule prettyReportCreator = new TextReport();
  
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
    if (hasWebDriverStarted()) {
      String flashCookiePrefix = Play.configuration.getProperty("application.session.cookie", "PLAY");
      String flashCookie = flashCookiePrefix + "_FLASH";
      getWebDriver().manage().deleteCookieNamed(flashCookie);
    }
  }
}
