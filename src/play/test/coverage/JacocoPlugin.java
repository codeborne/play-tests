package play.test.coverage;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.jacoco.core.analysis.*;
import org.jacoco.core.data.*;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import org.jacoco.report.DirectorySourceFileLocator;
import org.jacoco.report.FileMultiReportOutput;
import org.jacoco.report.IReportVisitor;
import org.jacoco.report.MultiSourceFileLocator;
import org.jacoco.report.html.HTMLFormatter;
import play.Logger;
import play.Play;
import play.PlayPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static play.classloading.ApplicationClasses.ApplicationClass;

public class JacocoPlugin extends PlayPlugin {
//  public static final String DEFAULT_IGNORE = "DocViewerPlugin,controllers.TestRunner,controllers.Cobertura,controllers.PlayDocumentation,controllers.Secure,controllers.Secure$Security,controllers.Check";
//  public static final String DEFAULT_REGEX_IGNORE = "*Test**,helpers.CheatSheetHelper*,controllers.CRUD*";

  private final boolean enabled = Play.runningInTestMode() && Boolean.getBoolean("jacoco.enabled");
  private final String executionDataFile = System.getProperty("jacoco.executionData", "build/jacoco/uitest.exec");
  private final String reportFolder = System.getProperty("jacoco.report", "build/reports/jacoco");

  static String[] excludes() {return System.getProperty("jacoco.excludes", "").split(", ");}
  static String[] sources() {return System.getProperty("jacoco.sources", "").replace("[", "").replace("]", "").split(", ");}

  private final PrintStream out = System.out;

  private Instrumenter instr;
  private RuntimeData runtimeData;
  private IRuntime runtime;
  private final Map<ApplicationClass, byte[]> instrumentedClasses = new HashMap<ApplicationClass, byte[]>(1280);

  private Instrumenter instrumenter() {
    if (instr == null) {
      runtime = new LoggerRuntime();

      try {
        Instrumenter instrumenter = new Instrumenter(runtime);

        runtimeData = new RuntimeData();
        runtime.startup(runtimeData);

        instr = instrumenter;
      }
      catch (Exception e) {
        Logger.error(e, "Failed to init Jacoco code coverage");
      }
    }
    return instr;
  }

  @Override public void onApplicationStart() {
    if (!enabled)
      Play.pluginCollection.disablePlugin(this);
    else {
      System.out.println("---------------------------------------");
      System.out.println("---------------------------------------");
      System.out.println("---------------------------------------");
      System.out.println("JACOCO: excludes=" + Arrays.toString(excludes()));
      System.out.println("JACOCO: sources=" + Arrays.toString(sources()));
      System.out.println("---------------------------------------");
      System.out.println("---------------------------------------");
      System.out.println("---------------------------------------");
    }
  }

  @Override
  public void enhance(ApplicationClass applicationClass) throws IOException, ClassNotFoundException {
    if (enabled && !isExcluded(applicationClass.name)) {
//      Logger.info("JacocoPlugin: enhance " + applicationClass);

      byte[] originalBytecode = applicationClass.enhancedByteCode;
      File classFile = new File(Play.tmpDir, "orig-classes/" + applicationClass.name.replace('.', '/') + ".class");
      classFile.getParentFile().mkdirs();
      IOUtils.write(originalBytecode, new FileOutputStream(classFile));
      applicationClass.enhancedByteCode = instrumenter().instrument(originalBytecode, applicationClass.name);
      instrumentedClasses.put(applicationClass, originalBytecode);
    }
  }

  private boolean isExcluded(String name) {
    for (String exclude : excludes()) {
      if (StringUtils.isNotBlank(exclude) && name.startsWith(exclude))
        return true;
    }
    return false;
  }

  @Override public void onApplicationStop() {
    if (!enabled) {
      return;
    }

    if (instr == null) {
      Logger.error("JacocoPlugin: onApplicationStop: skip generating report: instrumetator not initialized");
      return;
    }

    Logger.info("JacocoPlugin: onApplicationStop");
    ExecutionDataStore executionData = new ExecutionDataStore();
    SessionInfoStore sessionInfos = new SessionInfoStore();
    runtimeData.collect(executionData, sessionInfos, false);
    runtime.shutdown();

    try {
      IBundleCoverage bundleCoverage = analyze(executionData);

      writeExecutionDataTo(executionData, sessionInfos);

      printTotalCoverage(bundleCoverage);

      generateReport(bundleCoverage, executionData, sessionInfos);
    }
    catch (IOException e) {
      Logger.error(e, "Failed to generate coverage report", e);
    }
  }
  
  private MultiSourceFileLocator sourceFolders() {
    MultiSourceFileLocator multiSourceFileLocator = new MultiSourceFileLocator(2);
    for (String sourceFolder : sources()) {
      multiSourceFileLocator.add(new DirectorySourceFileLocator(new File(sourceFolder), "utf-8", 2));
    }
    return multiSourceFileLocator;
  }

  private void generateReport(IBundleCoverage bundleCoverage, ExecutionDataStore executionData, SessionInfoStore sessionInfos) throws IOException {
    File target = new File(reportFolder);

    System.out.println("Generating coverage report to " + target.getAbsolutePath() + " ...");
    // Create a concrete report visitor based on some supplied configuration. In this case we use the defaults
    final HTMLFormatter htmlFormatter = new HTMLFormatter();
    final IReportVisitor visitor = htmlFormatter.createVisitor(new FileMultiReportOutput(target));

    // Initialize the report with all of the execution and session
    // information. At this point the report doesn't know about the structure of the report being created
    visitor.visitInfo(sessionInfos.getInfos(), executionData.getContents());
//    visitor.visitInfo(execFileLoader.getSessionInfoStore().getInfos(), execFileLoader.getExecutionDataStore().getContents());

    // Populate the report structure with the bundle coverage information.
    // Call visitGroup if you need groups in your report.
    visitor.visitBundle(bundleCoverage, sourceFolders());

    // Signal end of structure information to allow report to write all information out
    visitor.visitEnd();

    System.out.println("Report generated: " + target.getAbsolutePath());
  }

  private IBundleCoverage analyze(ExecutionDataStore executionData) throws IOException {
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

    analyzer.analyzeAll(new File(Play.tmpDir, "orig-classes"));

    String title = new File(System.getProperty("user.dir")).getName();
    return coverageBuilder.getBundle(title);
  }

  private IBundleCoverage analyzeAndPrint(ExecutionDataStore executionData) {// Together with the original class definition we can calculate coverage information:
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

    for (ApplicationClass applicationClass : instrumentedClasses.keySet()) {
      try {
        byte[] originalBytecode = instrumentedClasses.get(applicationClass);
        analyzer.analyzeClass(originalBytecode, applicationClass.name);
      }
      catch (IOException e) {
        Logger.error(e, "Failed to analyze class " + applicationClass.name);
      }
    }

    int totalLOC = 0;
    int totalNotCoveredLOC = 0;

    // Let's dump some metrics and line coverage information:
    for (final IClassCoverage cc : coverageBuilder.getClasses()) {
//      if (cc.getName().equals("controllers/Bank")) {
      out.printf("Coverage of class %s%n", cc.getName());

//        printCounter("instructions", cc.getInstructionCounter());
//        printCounter("branches", cc.getBranchCounter());
//        printCounter("lines", cc.getLineCounter());
//        printCounter("methods", cc.getMethodCounter());
//        printCounter("complexity", cc.getComplexityCounter());

      totalLOC += cc.getLineCounter().getTotalCount();
      totalNotCoveredLOC += cc.getLineCounter().getMissedCount();

      for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
        if (ICounter.NOT_COVERED == cc.getLine(i).getStatus()) {
          out.printf("Line %s: %s%n", i, getColor(cc.getLine(i).getStatus()));
        }
      }
//      }
    }

    double codeCoverage = 100.0 * (totalLOC - totalNotCoveredLOC) / totalLOC;
    System.out.println("-------------------------------");
    System.out.println("-------------------------------");
    System.out.println("Total code coverage: " + codeCoverage + "%");
    System.out.println("-------------------------------");
    System.out.println("-------------------------------");

    String title = new File(System.getProperty("user.dir")).getName();
    return coverageBuilder.getBundle(title);
  }
  
  private void printTotalCoverage(IBundleCoverage bundle) {
    System.out.println("-------------------------------");
    System.out.println("Total code coverage:");
    System.out.println("Line coverage: " + str(bundle.getLineCounter()));
    System.out.println("Complexity coverage: " + str(bundle.getComplexityCounter()));
    System.out.println("-------------------------------");

  }

  private static String str(ICounter counter) {
    return 100*counter.getCoveredRatio() + "% (" + counter.getCoveredCount() + " / " + counter.getTotalCount() + ")";
  }

  private void writeExecutionDataTo(ExecutionDataStore executionData, SessionInfoStore sessionInfos) throws IOException {
    File executionFile = new File(executionDataFile);
    executionFile.getParentFile().mkdirs();
    FileOutputStream out = new FileOutputStream(executionFile);
    try {
      final ExecutionDataWriter writer = new ExecutionDataWriter(out);
      for (ExecutionData data : executionData.getContents()) {
        writer.visitClassExecution(data);
      }
      for (SessionInfo sessionInfo : sessionInfos.getInfos()) {
        writer.visitSessionInfo(sessionInfo);
      }
    }
    finally {
      out.close();
    }
    System.out.println("Stored execution data to " + executionFile.getAbsolutePath());
  }

  private void printCounter(final String unit, final ICounter counter) {
    Integer missed = counter.getMissedCount();
    Integer total = counter.getTotalCount();
    out.printf("%s of %s %s missed%n", missed, total, unit);
  }

  private String getColor(final int status) {
    switch (status) {
      case ICounter.NOT_COVERED:
        return "red";
      case ICounter.PARTLY_COVERED:
        return "yellow";
      case ICounter.FULLY_COVERED:
        return "green";
    }
    return "";
  }
}
