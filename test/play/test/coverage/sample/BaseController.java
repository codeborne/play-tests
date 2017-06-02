package play.test.coverage.sample;

import play.mvc.*;
import play.mvc.results.RenderText;
import play.mvc.results.Result;

public abstract class BaseController implements PlayController {
  @Before
  public void checkAccess() {}
  
  public Result home() {
    return new RenderText("hello");
  }

  @After
  public void closeTransactions() {}

  @Catch
  public void logAllErrors() {}

  @Finally
  public void closeResources() {}
}
