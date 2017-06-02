package play.test.coverage.sample;

import play.mvc.Controller;

public class StaticController extends Controller {
  public static void home() {
    render();
  }

  public static String someJson() {
    return "{some: json}";
  }
}
