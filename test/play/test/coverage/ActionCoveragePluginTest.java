package play.test.coverage;

import org.junit.Test;
import play.test.coverage.sample.BaseController;
import play.test.coverage.sample.StaticController;
import play.test.coverage.sample.UsersController;

import static org.junit.Assert.*;

public class ActionCoveragePluginTest {
  ActionCoveragePlugin plugin = new ActionCoveragePlugin();
  
  @Test
  public void findsAllControllerActionsInProject() {
  }

  @Test
  public void everyPublicMethodIsAction() throws NoSuchMethodException {
    assertTrue(plugin.isActionMethod(BaseController.class.getMethod("home")));
    assertTrue(plugin.isActionMethod(UsersController.class.getMethod("home")));
    assertTrue(plugin.isActionMethod(UsersController.class.getMethod("users")));
  }

  @Test
  public void staticPublicMethodIsAction() throws NoSuchMethodException {
    assertTrue(plugin.isActionMethod(StaticController.class.getMethod("home")));
    assertTrue(plugin.isActionMethod(StaticController.class.getMethod("someJson")));
  }

  @Test
  public void beforeIsNotAction() throws NoSuchMethodException {
    assertFalse(plugin.isActionMethod(BaseController.class.getMethod("checkAccess")));
  }

  @Test
  public void afterIsNotAction() throws NoSuchMethodException {
    assertFalse(plugin.isActionMethod(BaseController.class.getMethod("closeTransactions")));
  }

  @Test
  public void utilIsNotAction() throws NoSuchMethodException {
    assertFalse(plugin.isActionMethod(UsersController.class.getMethod("debug")));
    assertFalse(plugin.isActionMethod(UsersController.class.getMethod("warn")));
  }

  @Test
  public void catchIsNotAction() throws NoSuchMethodException {
    assertFalse(plugin.isActionMethod(BaseController.class.getMethod("logAllErrors")));
    assertFalse(plugin.isActionMethod(UsersController.class.getMethod("logAllErrors")));
  }

  @Test
  public void finallyIsNotAction() throws NoSuchMethodException {
    assertFalse(plugin.isActionMethod(BaseController.class.getMethod("closeResources")));
    assertFalse(plugin.isActionMethod(UsersController.class.getMethod("closeResources")));
  }
}