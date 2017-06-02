package play.test.coverage.sample;

import play.mvc.Util;
import play.mvc.results.RenderJson;
import play.mvc.results.Result;

public class UsersController extends BaseController {
  public Result users() {
    return new RenderJson("{}");
  }
  
  @Util public static void debug() {}
  @Util public void warn() {}
}
