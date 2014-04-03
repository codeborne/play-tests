package play.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.Keys.ESCAPE;

public abstract class TwitterBootstrapUITest extends UITest {
  public static void closeModal(By by) {
    $(by).sendKeys(ESCAPE);
    $(by).should(disappear);
  }

  public static void assertSuccessMessage(String message) {
    $$(".alert-success").find(visible).shouldHave(text(message));
  }

  public static void assertWarningMessage(String message) {
    $(".alert-warning").shouldHave(text(message));
  }

  public static void assertInfoMessage(String message) {
    $(".alert-info").shouldHave(text(message));
  }

  public static void assertErrorMessage(String message) {
    $(".alert-error").shouldHave(text(message));
  }

  public static void assertFieldHasError(String name, String message) {
    assertFieldHasErrorText(name, message);
  }

  public static void assertFieldHasError(String name, String message, Object... args) {
    assertFieldHasErrorText(name, message, args);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private static void assertFieldHasErrorText(String name, String message, Object... args) {
    WebElement element = $(By.name(name));
    while (!(element = element.findElement(By.xpath(".."))).getAttribute("class").contains("control-group"));
    if (!element.getAttribute("class").contains("error") && !element.findElement(By.className("error")).isDisplayed())
      fail("No error class on " + element.getText());
    $(element).should(haveLabel(message, args));
  }
}
