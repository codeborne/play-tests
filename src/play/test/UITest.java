package play.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import play.Play;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.mvc.Router;

import java.util.HashMap;
import java.util.Map;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;

@RunWith(PlayTestsRunner.class)
public abstract class UITest extends Assert {
  @Rule public ScreenShooter makeScreenshotOnFailure = failedTests();

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

  protected String label(String key) {
    return Messages.get(key);
  }

  protected String label(String key, Object... args) {
    return Messages.get(key, args);
  }

  private static Condition action(final String action, final Map<String, String> args) {
    return new Condition("action " + action + " " + args) {
      @Override public boolean apply(WebElement element) {
        String expectedUrl = Router.getFullUrl(action, new HashMap<String, Object>(args));
        return expectedUrl.equals(actualUrl());
      }

      private String actualUrl() {
        return getWebDriver().getCurrentUrl().replaceFirst("\\?.*$", "");
      }

      @Override public String actualValue(WebElement element) {
        return actualUrl();
      }
    };
  }

  protected void assertAction(String action) {
    $("body").shouldHave(action(action, new HashMap<String, String>(0)));
  }

  protected void assertAction(String action, Map<String, String> args) {
    $("body").shouldHave(action(action, args));
  }

  protected void mockConfirm() {
    executeJavaScript("window.confirm = function() {return true;};");
  }

  protected void mockAlert() {
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
}
