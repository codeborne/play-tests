package play.test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;

import java.io.*;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static java.lang.Boolean.parseBoolean;
import static play.test.TestType.UNIT;

public class JUnitRunnerWithXMLOutput {
  private final File testResults = new File("test-result");
  private final TestType testType;
  private final List<BuildFailures> lastFailedTests;
  private final boolean randomOrder;

  private boolean stackFilter = true;
  private boolean haltOnError = false;
  private boolean haltOnFail = false;
  private boolean showOutput = true;
  private boolean logTestListenerEvents = false;

  public JUnitRunnerWithXMLOutput(TestType testType, boolean randomOrder) throws IOException {
    this.testType = testType;
    this.randomOrder = randomOrder;
    lastFailedTests = IO.readLastFailedTests(testType);
  }

  private static Collection<Class> getTestClasses(List<Class> classes, TestType testType) {
    Collection<Class> allTests = filter(classes, new IsTestClass());
    return filter(allTests, testType.getFilter());
  }

  private static Collection<Class> findTestClasses(TestType testType) throws IOException, ClassNotFoundException {
    JavaSourcesCollection sources = new JavaSourcesCollection().scan();
    return getTestClasses(sources.getClasses(), testType);
  }

  private File saveTestsOrder(List<Class> testClasses) throws IOException {
    File file = new File(testResults, testType + "-tests.txt");
    PrintWriter out = new PrintWriter(new FileWriter(file));
    try {
      for (Class testClass : testClasses) {
        out.println(testClass.getName());
      }
    }
    finally {
      out.close();
    }
    return file;
  }

  private int runSingleTest(String testClass) throws Exception {
    JUnitTest jUnitTest = executeTest(testClass);
    return (int) (jUnitTest.errorCount() + jUnitTest.failureCount());
  }

  private int runAllTests() throws Exception {
    List<Class> classes = sort(findTestClasses(testType));

    saveTestsOrder(classes);

    System.out.println("Run " + classes.size() + " " + testType + " tests");

    List<JUnitTest> results = new ArrayList<JUnitTest>(classes.size());

    if (!lastFailedTests.isEmpty()) {
      BuildFailures lastFailedBuild = lastFailedTests.get(0);
      for (String test : lastFailedBuild.failedTests) {
        results.add(executeTest(test));
      }

      int failures = countFailures(results);
      if (failures > 0) {
        printSummary(results);
        System.out.println();
        System.out.println("NB! Stop execution of tests because some of recently failed tests failed again");
        return failures;
      }

      removeAll(classes, lastFailedBuild.failedTests);
    }

    for (Class testClass : classes) {
      results.add(executeTest(testClass));
    }

    printLongestTests(results);
    printSummary(results);

    BuildFailures build = new BuildFailures(testType, results);
    IO.save(build);
    return build.problemsCount;
  }

  private void removeAll(Collection<Class> classes, Collection<String> failedTests) throws ClassNotFoundException {
    for (String test : failedTests) {
      classes.remove(Class.forName(test));
    }
  }

  private int countFailures(List<JUnitTest> results) {
    int errors = 0;
    for (JUnitTest testResult : results) {
      errors += testResult.errorCount() + testResult.failureCount();
    }
    return errors;
  }

  private static final class ClassNameComparator implements Comparator<Class> {
    @Override public int compare(Class aClass, Class bClass) {
      return aClass.getName().compareTo(bClass.getName());
    }
  }

  private List<Class> sort(Collection<Class> classes) {
    List<Class> sorted = new ArrayList<Class>(classes);
    if (randomOrder) {
      Collections.shuffle(sorted);
    }
    else {
      Collections.sort(sorted, new ClassNameComparator());
    }

    Collections.sort(sorted, new TestComparatorByFailuresHistory(lastFailedTests));
    return sorted;
  }

  private void printLongestTests(List<JUnitTest> results) {
    Collections.sort(results, new Comparator<JUnitTest>() {
      @Override public int compare(JUnitTest o1, JUnitTest o2) {
        return Long.compare(o2.getRunTime(), o1.getRunTime());
      }
    });

    System.out.println();
    System.out.println();
    System.out.println("Longest tests:");

    final List<JUnitTest> top = results.size() < 10 ? results : results.subList(0, 10);
    for (JUnitTest result : top) {
      System.out.println(result.getName() + " -> " + result.getRunTime() + " ms.");
    }
  }

  private int printSummary(List<JUnitTest> results) {
    int errors = 0;
    for (JUnitTest result : results) {
      if (result.errorCount() != 0 || result.failureCount() != 0) {
        if (errors == 0) {
          System.out.println();
          System.out.println();
          System.out.println("Failed tests:");
        }

        System.out.println(result.getName() + " " +
            result.runCount() + " runs, " +
            result.errorCount() + " errors, " +
            result.failureCount() + " failures");
        errors += result.errorCount() + result.failureCount();
      }
    }

    System.out.println();
    return errors;
  }

  private JUnitTest executeTest(Class testClass) throws FileNotFoundException {
    return executeTest(testClass.getName());
  }

  private JUnitTest executeTest(String testClass) throws FileNotFoundException {
    JUnitTest t = new JUnitTest(testClass);
    t.setTodir(testResults);
    t.setOutfile(new File(testResults, testClass + ".xml").getAbsolutePath());
    t.setProperties(System.getProperties());
    t.setFork(true);

    JUnitTestRunner runner = new JUnitTestRunner(t, null, haltOnError, stackFilter, haltOnFail, showOutput, logTestListenerEvents);
    XMLJUnitResultFormatter resultFormatter = new XMLJUnitResultFormatter();
    resultFormatter.setOutput(new BufferedOutputStream(new FileOutputStream(t.getOutfile())));
    runner.addFormatter(resultFormatter);

    SummaryJUnitResultFormatter summary = new SummaryJUnitResultFormatter();
    summary.setOutput(System.out);
    runner.addFormatter(summary);

    runner.run();
    return t;
  }

  public static void main(String[] args) throws Exception {
    TestType testType = args.length > 0 ? TestType.valueOf(args[0]) : UNIT;
    boolean randomOrder = args.length > 1 && parseBoolean(args[1]);
    JUnitRunnerWithXMLOutput runner = new JUnitRunnerWithXMLOutput(testType, randomOrder);
    int exitCode = args.length > 2 ? runner.runSingleTest(args[2]) : runner.runAllTests();
    System.exit(exitCode);
  }
}
