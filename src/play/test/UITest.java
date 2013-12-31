package play.test;

import com.codeborne.selenide.Configuration;
import org.junit.BeforeClass;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public abstract class UITest extends BaseTest {
  private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

  public static boolean isPrecompileNeeded = Boolean.getBoolean("precompiled");
  public static boolean isPlayStartNeeded = !"false".equals(System.getProperty("selenide.play.start", "true"));

  @BeforeClass
  public static synchronized void startServer() {
    if (isPlayStartNeeded && !serverStarted.get()) {
      if (isPrecompileNeeded) {
        Play.usePrecompiled = true;
      }
      new Server(new String[]{});
      serverStarted.set(true);
      Configuration.baseUrl = "http://localhost:" + Play.configuration.getProperty("http.port");
    }
  }

  protected String label(String key) {
    return Messages.get(key);
  }

  protected String label(String key, Object... args) {
    return Messages.get(key, args);
  }

  protected void assertAction(String action) {
    assertEquals(Router.getFullUrl(action), getWebDriver().getCurrentUrl().replaceFirst("\\?.*$", ""));
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
