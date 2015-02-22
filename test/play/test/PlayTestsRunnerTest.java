package play.test;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.model.InitializationError;
import play.Play;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static play.Play.Mode.PROD;

public class PlayTestsRunnerTest {
  @Before
  public void setUp() {
    Play.configuration = new Properties();
    Play.configuration.setProperty("application.name", "my-app");
  }

  @Test
  public void useDifferentTmpFoldersForTestsRunningSimultaneouslyToAvoidConflictsBetweenProcesses() throws InitializationError {
    Play.id = "test";
    Play.mode = PROD;

    new PlayTestsRunner(PlayTestsRunnerTest.class).makeUniqueCachePath();
    File tmp = new File("tmp/my-app/test/" + ManagementFactory.getRuntimeMXBean().getName());
    assertEquals(tmp.getAbsolutePath(), System.getProperty("java.io.tmpdir"));
    assertTrue(tmp.exists());
    assertTrue(tmp.isDirectory());
  }

  @Test
  public void shouldNotModifyPlayTmpToAssureReusingOfCompiledClasses() throws InitializationError {
    Play.configuration.setProperty("play.tmp", "temp");
    Play.tmpDir = new File("temp");

    new PlayTestsRunner(PlayTestsRunnerTest.class).makeUniqueCachePath();
    assertEquals(new File("temp"), Play.tmpDir);
    assertEquals("temp", Play.configuration.getProperty("play.tmp"));
  }
}