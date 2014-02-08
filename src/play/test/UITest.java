package play.test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.junit.ScreenShooter;
import org.junit.*;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebElement;
import play.Play;
import play.db.jpa.JPAPlugin;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.codeborne.selenide.Selenide.*;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.junit.ScreenShooter.failedTests;
import static org.openqa.selenium.net.PortProber.findFreePort;

@RunWith(PlayTestsRunner.class)
public abstract class UITest extends Assert {
  @Rule public ScreenShooter makeScreenshotOnFailure = failedTests();

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

      warmupServer();
      Play.configuration.setProperty("application.baseUrl", Configuration.baseUrl);
      serverStarted.set(true);
    }
  }

  private static void warmupServer() {
    open("/");
  }

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
        String actualUrl = actualUrl();
        if (!expectedUrl.equals(actualUrl)) {
          System.out.println("Actual url: " + actualUrl + ", expected url: " + expectedUrl);
        }
        return expectedUrl.equals(actualUrl);
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
