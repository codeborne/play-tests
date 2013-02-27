package play.test;

import com.codeborne.selenide.Configuration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import static com.codeborne.selenide.Selenide.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;
import static com.codeborne.selenide.WebDriverRunner.takeScreenShot;

public abstract class UITest extends BaseTest {
  private static boolean serverStarted = false;

  @BeforeClass
  public static synchronized void startServer() {
    if (!serverStarted) {
      new Server(new String[]{});
      serverStarted = true;
    }
    Configuration.baseUrl = "http://localhost:" + Play.configuration.get("http.port");
  }

  @Rule public TestWatcher makeScreenshotOnFailure = new TestWatcher() {
    @Override protected void failed(Throwable e, Description description) {
      System.err.println("Saved failing screenshot to: " + takeScreenShot(description.getClassName() + "." + description.getMethodName()));
    }
  };

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
    executeJavaScript("window.confirm = function() {};");
  }
}
