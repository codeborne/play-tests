package play.test.coverage;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;

public class JacocoPluginTest {
  @Test
  public void parseExcludes() {
    System.setProperty("jacoco.excludes", "util.geoip., test.xxx.");
    assertArrayEquals(new String[]{"util.geoip.", "test.xxx."}, JacocoPlugin.excludes());
  }

  @Test
  public void parseSources() {
    System.setProperty("jacoco.sources", "[app, src, test]");
    assertArrayEquals(new String[]{"app", "src", "test"}, JacocoPlugin.sources());
  }
}