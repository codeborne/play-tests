package play.test;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static com.codeborne.selenide.Condition.hasText;
import static com.codeborne.selenide.Condition.hidden;
import static com.codeborne.selenide.DOM.*;
import static org.openqa.selenium.Keys.ESCAPE;

public abstract class TwitterBootstrapUITest extends UITest {
  protected void closeModal(By by) {
    getElement(by).sendKeys(ESCAPE);
    waitUntil(by, hidden);
  }

  protected void assertSuccessMessage(String message) {
    assertElement(By.className("alert-success"), hasText(message));
  }

  protected void assertWarningMessage(String message) {
    assertElement(By.className("alert-warning"), hasText(message));
  }

  protected void assertInfoMessage(String message) {
    assertElement(By.className("alert-info"), hasText(message));
  }

  protected void assertErrorMessage(String message) {
    assertElement(By.className("alert-error"), hasText(message));
  }

  @SuppressWarnings("StatementWithEmptyBody")
  protected void assertFieldHasError(String name, String message) {
    WebElement element = getElement(By.name(name));
    while (!(element = element.findElement(By.xpath(".."))).getAttribute("class").contains("control-group"));
    if (!element.getAttribute("class").contains("error") && !element.findElement(By.className("error")).isDisplayed()) fail("No error class on " + element.getText());
    assertElement(element, hasText(label(message)));
  }
}
