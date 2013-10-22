package play.test;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;
import play.Play;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.google.common.collect.Collections2.filter;

public class JUnitRunnerWithXMLOutput {
  enum TestType {
    UNIT,
    UI;

    public Predicate<Class> getFilter() {
      return this == UNIT ? new IsUnitTest() : Predicates.not(new IsUnitTest());
    }
  }

  private static void prepareTestResult() throws IOException {
    File testResults = Play.getFile("test-result");
    if (testResults.exists()) {
      FileUtils.deleteDirectory(testResults);
    }
    testResults.mkdir();
  }

  private static Collection<Class> getTestClasses(List<Class> classes, TestType testType) throws ClassNotFoundException {
//    List<Class> allClasses = new ClassesCollection().findTestClasses();
    List<Class> allClasses = classes;
    Collection<Class> allTests = filter(allClasses, new IsTestClass());
//    System.out.println("allTests: " + allTests);
    final Collection<Class> typeTests = filter(allTests, testType.getFilter());
//    System.out.println("filter: " + testType.getFilter());
//    System.out.println("typeTests: " + typeTests);
    return typeTests;
  }

  private final static class ClassNameComparator implements Comparator<Class> {
    @Override public int compare(Class aClass, Class bClass) {
      return aClass.getName().compareTo(bClass.getName());
    }
  }

  private final static ClassNameComparator classNameComparator = new ClassNameComparator();

  private static File prepareTestsFile(TestType testType, List<Class> testClasses) throws IOException {
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

  public static void main(String[] args) throws Exception {
    TestType testType = TestType.valueOf(args[0]);

    JavaSourcesCollection sources = new JavaSourcesCollection().scan();
//    Compiler.compile(sources.getSourceRoots(), sources.getSourceFiles(), "test-classes");

    Collection<Class> classes = getTestClasses(sources.getClasses(), testType);
    List<Class> sorted = new ArrayList<Class>(classes);
    Collections.sort(sorted, classNameComparator);

    File testsFile = prepareTestsFile(testType, sorted);

    // use ant's runner to output results into xml
    JUnitTestRunner.main(new String[]{"testsfile=" + testsFile, "formatter=" + XMLJUnitResultFormatter.class.getName() + ",xml", "showoutput=true"});
  }
}
