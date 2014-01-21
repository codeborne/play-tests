package play.test;

import java.util.Comparator;
import java.util.List;

public class TestComparatorByFailuresHistory implements Comparator<Class> {
  private final List<BuildFailures> lastFailedTests;

  public TestComparatorByFailuresHistory(List<BuildFailures> lastFailedTests) {
    this.lastFailedTests = lastFailedTests;
  }

  @Override public int compare(Class test1, Class test2) {
    for (BuildFailures build : lastFailedTests) {
      if (build.contains(test1) && !build.contains(test2)) {
        return -1;
      }
      if (!build.contains(test1) && build.contains(test2)) {
        return 1;
      }
    }
    return 0;
  }
}
