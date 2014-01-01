package play.test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;

import java.io.*;
import java.util.*;

import static java.util.Collections.emptyList;

public class BuildFailures {
  public final TestType testType;
  final Set<String> failedTests;
  public final int problemsCount;

  public BuildFailures(TestType testType, Set<String> failedTests) {
    this.testType = testType;
    this.failedTests = failedTests;
    problemsCount = failedTests.size();
  }

  public BuildFailures(TestType testType, List<JUnitTest> results) {
    this.testType = testType;
    failedTests = new LinkedHashSet<String>();

    int problems = 0;
    for (JUnitTest result : results) {
      if (result.errorCount() != 0 || result.failureCount() != 0) {
        failedTests.add(result.getName());
        problems += result.errorCount() + result.failureCount();
      }
    }

    this.problemsCount = problems;
  }

  public boolean hasProblems() {
    return problemsCount > 0;
  }

  public boolean contains(Class testClass) {
    return failedTests.contains(testClass.getName());
  }
}

class IO {
  private static final File buildsHistory = new File(".builds");

  public static final Comparator<File> NEWEST_FILES_FIRST = new Comparator<File>() {
    @Override public int compare(File file1, File file2) {
      return file1.lastModified() > file2.lastModified() ? -1 : 1;
    }
  };

  public static List<BuildFailures> readLastFailedTests(final TestType testType) throws IOException {
    File[] builds = getBuildFiles(testType);

    if (builds == null || builds.length == 0) {
      return emptyList();
    }

    List<BuildFailures> lastFailedBuilds = new ArrayList<BuildFailures>(builds.length);
    for (File build : builds) {
      lastFailedBuilds.add(readBuildFailedTests(testType, build));
    }
    return lastFailedBuilds;
  }

  private static File[] getBuildFiles(final TestType testType) {
    if (!buildsHistory.exists()) {
      return new File[0];
    }

    File[] builds = buildsHistory.listFiles(new FilenameFilter() {
      @Override public boolean accept(File dir, String name) {
        return name.endsWith("." + testType);
      }
    });
    Arrays.sort(builds, NEWEST_FILES_FIRST);
    return builds;
  }

  public static BuildFailures readBuildFailedTests(TestType testType, File build) throws IOException {
    BufferedReader in = new BufferedReader(new FileReader(build));
    try {
      Set<String> testClasses = new LinkedHashSet<String>();
      String line;
      while ((line = in.readLine()) != null) {
        if (isExistingClass(line)) {
          testClasses.add(line);
        }
        else {
          System.out.println("Skipping test " + line + " because this class does not exist");
        }
      }
      return new BuildFailures(testType, testClasses);
    } finally {
      in.close();
    }
  }

  private static boolean isExistingClass(String className) {
    try {
      Class.forName(className);
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static void save(BuildFailures build) throws IOException {
    if (!buildsHistory.exists()) {
      buildsHistory.mkdirs();
    }

    File file = new File(buildsHistory, "" + System.currentTimeMillis() + '.' + build.testType);
    PrintWriter out = new PrintWriter(new FileWriter(file));
    try {
      for (String failedTest : build.failedTests) {
        out.println(failedTest);
      }
    }
    finally {
      out.close();
    }
  }
}
