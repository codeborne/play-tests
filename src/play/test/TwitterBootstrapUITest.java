package play.test;

import com.codeborne.selenide.SelenideElement;
import org.openqa.selenium.By;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;
import static org.openqa.selenium.Keys.ESCAPE;

public abstract class TwitterBootstrapUITest extends UITest {
  public static void closeModal(By by) {
    $(by).sendKeys(ESCAPE);
    $(by).should(disappear);
  }

  public static SelenideElement assertSuccessMessage(String message) {
    return $$(".alert-success").findBy(text(message)).shouldBe(visible);
  }

  public static SelenideElement assertWarningMessage(String message) {
    return $$(".alert-warning").findBy(text(message)).shouldBe(visible);
  }

  public static SelenideElement assertInfoMessage(String message) {
    return $$(".alert-info").findBy(text(message)).shouldBe(visible);
  }

  public static SelenideElement assertErrorMessage(String message) {
    return $$(".alert-error").findBy(text(message)).shouldBe(visible);
  }

  public static SelenideElement assertFieldHasError(String name, String message) {
    return assertFieldHasErrorText(name, message);
  }

  public static SelenideElement assertFieldHasError(String name, String message, Object... args) {
    return assertFieldHasErrorText(name, message, args);
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private static SelenideElement assertFieldHasErrorText(String name, String message, Object... args) {
    SelenideElement element = $(By.name(name)).closest(".control-group");
    element.should(haveLabel(message, args));
    
    if (!element.has(cssClass("error")) && !element.find(".error").isDisplayed())
      fail("No error class on " + element);
    
    return element;
  }
}
