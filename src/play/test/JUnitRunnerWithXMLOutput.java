package play.test;

import com.google.common.base.Predicate;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.SummaryJUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import play.Play;

import java.io.*;
import java.util.*;

import static com.google.common.collect.Collections2.filter;
import static play.test.JUnitRunnerWithXMLOutput.TestType.UNIT;

public class JUnitRunnerWithXMLOutput {
  enum TestType {
    UNIT,
    UI;

    public Predicate<Class> getFilter() {
      return this == UNIT ? new IsUnitTest() : new IsUITest();
    }
  }

  private final File testResults;
  private final TestType testType;

  private boolean stackfilter = true;
  private boolean haltOnError = false;
  private boolean haltOnFail = false;
  private boolean showOutput = true;
  private boolean logTestListenerEvents = false;

  public JUnitRunnerWithXMLOutput(TestType testType) {
    this.testType = testType;
    testResults = Play.getFile("test-result");
  }

  private static Collection<Class> getTestClasses(List<Class> classes, TestType testType) {
//    List<Class> allClasses = new ClassesCollection().findTestClasses();
    List<Class> allClasses = classes;
    Collection<Class> allTests = filter(allClasses, new IsTestClass());
//    System.out.println("allTests: " + allTests);
    final Collection<Class> typeTests = filter(allTests, testType.getFilter());
//    System.out.println("filter: " + testType.getFilter());
//    System.out.println("typeTests: " + typeTests);
    return typeTests;
  }

  private static final class ClassNameComparator implements Comparator<Class> {
    @Override public int compare(Class aClass, Class bClass) {
      return aClass.getName().compareTo(bClass.getName());
    }
  }

  private static final ClassNameComparator classNameComparator = new ClassNameComparator();

  private static Collection<Class> findTestClasses(TestType testType) throws IOException, ClassNotFoundException {
    JavaSourcesCollection sources = new JavaSourcesCollection().scan();
    return getTestClasses(sources.getClasses(), testType);
  }

  private static File createTestsFile(TestType testType, List<Class> testClasses) throws IOException {
    File file = new File("test-result/" + testType + "-tests.txt");
    PrintWriter out = new PrintWriter(new FileWriter(file));
    try {
      for (Class testClass : testClasses) {
        out.println(testClass.getName() + ",test-result," + testClass.getName());
      }
    }
    finally {
      out.close();
    }
    return file;
  }

  private int runTest(String testClass) throws Exception {
    JUnitTest jUnitTest = executeTest(Class.forName(testClass));
    return (int) (jUnitTest.errorCount() + jUnitTest.failureCount());
  }

  private int runTests() throws Exception {
    List<Class> classes = sort(findTestClasses(testType));

    System.out.println("Run " + classes.size() + " " + testType + " tests");

    List<JUnitTest> results = new ArrayList<JUnitTest>(classes.size());
    for (Class testClass : classes) {
      results.add(executeTest(testClass));
    }

    printLongestTests(results);
    return printSummary(results);
  }

  private List<Class> sort(Collection<Class> classes) {
    List<Class> sorted = new ArrayList<Class>(classes);
    Collections.sort(sorted, classNameComparator);
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
            result.errorCount() + " errors, " +
            result.failureCount() + " failures, " +
            result.runCount() + " runs");
        errors += result.errorCount() + result.failureCount();
      }
    }

    System.out.println();
    return errors;
  }

  private JUnitTest executeTest(Class testClass) throws FileNotFoundException {
    JUnitTest t = new JUnitTest(testClass.getName());
    t.setTodir(testResults);
    t.setOutfile(new File(testResults, testClass.getName() + ".xml").getAbsolutePath());
    t.setProperties(System.getProperties());
    t.setFork(true);

    JUnitTestRunner runner = new JUnitTestRunner(t, null, haltOnError, stackfilter, haltOnFail, showOutput, logTestListenerEvents);
    XMLJUnitResultFormatter resultFormatter = new XMLJUnitResultFormatter();
    resultFormatter.setOutput(new BufferedOutputStream(new FileOutputStream(t.getOutfile())));
    runner.addFormatter(resultFormatter);

    final SummaryJUnitResultFormatter summary = new SummaryJUnitResultFormatter();
    summary.setOutput(System.out);
    runner.addFormatter(summary);

//      final PlainJUnitResultFormatter plain = new PlainJUnitResultFormatter();
//      plain.setOutput(System.out);
//      runner.addFormatter(plain);

    runner.run();
    return t;
  }

  private static void useAntRunnerToOutputResultsIntoXml(TestType testType, String[] args) throws Exception {
//    File testsFile = args.length == 1 ? prepareTestsFile(testType) : new File(args[1]);
//    JUnitTestRunner.main(new String[]{"testsfile=" + testsFile, "formatter=" + XMLJUnitResultFormatter.class.getName() + ",xml", "showoutput=true"});
  }

  public static void main(String[] args) throws Exception {
    TestType testType = args.length > 0 ? TestType.valueOf(args[0]) : UNIT;
    JUnitRunnerWithXMLOutput runner = new JUnitRunnerWithXMLOutput(testType);
    int exitCode = args.length > 1 ? runner.runTest(args[1]) : runner.runTests();
    System.exit(exitCode);
  }
}
