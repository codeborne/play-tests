package play.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.disappear;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static org.openqa.selenium.Keys.ESCAPE;

public abstract class TwitterBootstrapUITest extends UITest {
  protected void closeModal(By by) {
    $(by).sendKeys(ESCAPE);
    $(by).should(disappear);
  }

  protected void assertSuccessMessage(String message) {
    $(".alert-success").shouldHave(text(message));
  }

  protected void assertWarningMessage(String message) {
    $(".alert-warning").shouldHave(text(message));
  }

  protected void assertInfoMessage(String message) {
    $(".alert-info").shouldHave(text(message));
  }

  protected void assertErrorMessage(String message) {
    $(".alert-error").shouldHave(text(message));
  }

  protected void assertFieldHasError(String name, String message) {
    assertFieldHasErrorText(name, label(message));
  }

  protected void assertFieldHasError(String name, String message, Object... args) {
    assertFieldHasErrorText(name, label(message, args));
  }

  @SuppressWarnings("StatementWithEmptyBody")
  private void assertFieldHasErrorText(String name, String text) {
    WebElement element = $(By.name(name));
    while (!(element = element.findElement(By.xpath(".."))).getAttribute("class").contains("control-group"));
    if (!element.getAttribute("class").contains("error") && !element.findElement(By.className("error")).isDisplayed()) fail("No error class on " + element.getText());
    $(element).shouldHave(text(text));
  }
}
