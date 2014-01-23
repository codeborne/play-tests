package play.test;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.BeforeClass;
import org.junit.Rule;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.Selenide.open;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;
import static org.openqa.selenium.net.PortProber.findFreePort;

public abstract class UITest extends BaseTest {
  private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

  public static boolean isPrecompileNeeded = Boolean.getBoolean("precompiled");
  public static boolean isPlayStartNeeded = !"false".equals(System.getProperty("selenide.play.start", "true"));

  @BeforeClass
  public static synchronized void startServer() throws IOException {
    if (isPlayStartNeeded && !serverStarted.get()) {
      if (isPrecompileNeeded) {
        Play.usePrecompiled = true;
      }

      int port = findFreePort();
      Configuration.baseUrl = "http://localhost:" + port;
      new Server(new String[]{"--http.port=" + port});
      warmupServer();
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
      serverStarted.set(true);
    }
  }

  private static void warmupServer() {
    open("/");
  }

  @Rule public ScreenShooter makeScreenshotOnFailure = failedTests();

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
