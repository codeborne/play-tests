package play.test;

import org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner;
import org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter;

import java.io.IOException;

public class JUnitRunnerWithXMLOutput {
  public static void main(String[] args) throws IOException {
    // use ant's runner to output results into xml
    JUnitTestRunner.main(new String[]{"testsfile=" + args[0], "formatter=" + XMLJUnitResultFormatter.class.getName() + ",xml", "showoutput=true"});
  }
}
