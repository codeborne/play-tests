package play.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.openqa.selenium.WebElement;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;
import static org.openqa.selenium.net.PortProber.findFreePort;

public abstract class UITest extends BaseTest {
  private static final AtomicBoolean serverStarted = new AtomicBoolean(false);

  public static boolean isPrecompileNeeded = Boolean.getBoolean("precompiled");
  public static boolean isPlayStartNeeded = !"false".equals(System.getProperty("selenide.play.start", "true"));

  static {
    if (isPrecompileNeeded) {
      System.out.println("Precompiled static -------------------");
      Play.usePrecompiled = true;
    }
  }

  @BeforeClass
  public static synchronized void startServer() throws IOException {
    if (isPlayStartNeeded && !serverStarted.get()) {
      if (isPrecompileNeeded) {
        System.out.println("Precompiled before class -------------------");
        Play.usePrecompiled = true;
      }

      System.out.println("Setup play server -------------------");

      int port = findFreePort();
      Configuration.baseUrl = "http://localhost:" + port;
      new Server(new String[]{"--http.port=" + port});

//      (Experimental)
//      Make cookie unique to avoid clashes between parallel Chrome instances:
//      String sessionCookie = Play.configuration.getProperty("application.session.cookie");
//      Play.configuration.setProperty("application.session.cookie", sessionCookie + "_" + ManagementFactory.getRuntimeMXBean().getName());

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

  private static Condition action(final String action) {
    return new Condition("action") {
      @Override public boolean apply(WebElement element) {
        String expectedUrl = Router.getFullUrl(action);
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
    $("body").shouldHave(action(action));
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
