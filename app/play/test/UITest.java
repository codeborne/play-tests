package play.test;

import com.codeborne.selenide.Navigation;
import com.codeborne.selenide.WebDriverRunner;
import org.junit.After;
import org.junit.BeforeClass;
import play.Play;
import play.i18n.Messages;
import play.mvc.Router;
import play.server.Server;

import static com.codeborne.selenide.DOM.executeJavaScript;
import static com.codeborne.selenide.WebDriverRunner.getWebDriver;

public abstract class UITest extends BaseTest {
  private static boolean serverStarted = false;

  @BeforeClass
  public static synchronized void startServer() throws Exception {
    if (!serverStarted) {
      new Server(new String[]{});
      serverStarted = true;
    }
    Navigation.baseUrl = "http://localhost:" + Play.configuration.get("http.port");
  }

  @After
  public void tearDown() throws Exception {
    if (Boolean.getBoolean("screenshots"))
      System.out.println("Saved screenshot to: " + WebDriverRunner.takeScreenShot("" + System.currentTimeMillis()));
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
    executeJavaScript("window.confirm = function() {};");
  }
}
