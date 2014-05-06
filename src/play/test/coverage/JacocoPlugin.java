package play.test.coverage;

import org.jacoco.core.analysis.Analyzer;
import org.jacoco.core.analysis.CoverageBuilder;
import org.jacoco.core.analysis.IClassCoverage;
import org.jacoco.core.analysis.ICounter;
import org.jacoco.core.data.*;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.IRuntime;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.runtime.RuntimeData;
import play.Logger;
import play.Play;
import play.PlayPlugin;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static play.classloading.ApplicationClasses.ApplicationClass;

public class JacocoPlugin extends PlayPlugin {
//  public static final String DEFAULT_IGNORE = "DocViewerPlugin,controllers.TestRunner,controllers.Cobertura,controllers.PlayDocumentation,controllers.Secure,controllers.Secure$Security,controllers.Check";
//  public static final String DEFAULT_REGEX_IGNORE = "*Test**,helpers.CheatSheetHelper*,controllers.CRUD*";

  private final boolean enabled = Play.runingInTestMode() && Boolean.getBoolean("jacoco.enabled");
  private final PrintStream out = System.out;

  private Instrumenter instr;
  private RuntimeData runtimeData;
  private IRuntime runtime;
  private final List<ApplicationClass> instrumentedClasses = new ArrayList<ApplicationClass>();

  private Instrumenter instrumenter() {
    if (instr == null) {
      // For instrumentation and runtime we need a IRuntime instance
      // to collect execution data:
      runtime = new LoggerRuntime();

      try {

        // The Instrumenter creates a modified version of our test target class
        // that contains additional probes for execution data recording:
//        Instrumenter instrumenter = (Instrumenter) jacocoClassLoader
//            .loadClass("org.jacoco.core.instr.Instrumenter")
//            .getConstructor(IExecutionDataAccessorGenerator.class).newInstance(runtime);
      Instrumenter instrumenter = new Instrumenter(runtime);

        // Now we're ready to run our instrumented class and need to startup the
        // runtime first:
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
  }

  @Override
  public void enhance(ApplicationClass applicationClass) throws IOException, ClassNotFoundException {
//    if (applicationClass.name.startsWith("util.geoip."))
//      return;

    if (enabled && !applicationClass.name.startsWith("util.geoip.")) {
//    if (enabled && (applicationClass.name.startsWith("util.") ||
//                    applicationClass.name.startsWith("validation.") ||
//                    applicationClass.name.startsWith("services.") ||
//                    applicationClass.name.startsWith("play.db.") ||
//                    applicationClass.name.startsWith("navigation.") ||
//                    applicationClass.name.startsWith("jobs.")
//    )) {
      Logger.info("JacocoPlugin: enhance " + applicationClass);

//      Logger.info("org.objectweb.asm.ClassVisitor: " + getSource("org.objectweb.asm.ClassVisitor"));
//      Logger.info("org.jacoco.core.instr.Instrumenter: " + getSource("org.jacoco.core.instr.Instrumenter"));
//      Logger.info("org.jacoco.core.internal.flow.ClassProbesAdapter: " + getSource("org.jacoco.core.internal.flow.ClassProbesAdapter"));

      applicationClass.enhancedByteCode = instrumenter()
          .instrument(applicationClass.enhancedByteCode, applicationClass.name);

      instrumentedClasses.add(applicationClass);
    }
  }

  @Override public void onApplicationStop() {
    Logger.info("JacocoPlugin: onApplicationStop");

    if (!enabled || instr == null) {
      return;
    }

    ExecutionDataStore executionData = new ExecutionDataStore();
    SessionInfoStore sessionInfos = new SessionInfoStore();
    runtimeData.collect(executionData, sessionInfos, false);
    runtime.shutdown();

    try {
      writeExecutionDataTo(executionData, sessionInfos, "build/jacoco/uitest2.exec");
    }
    catch (IOException e) {
      Logger.error(e, "Failed to write execution data", e);
    }
    // printAnalysis(executionData);
  }

  private void printAnalysis(ExecutionDataStore executionData) {// Together with the original class definition we can calculate coverage information:
    CoverageBuilder coverageBuilder = new CoverageBuilder();
    Analyzer analyzer = new Analyzer(executionData, coverageBuilder);

    for (ApplicationClass applicationClass : instrumentedClasses) {
      try {
        analyzer.analyzeClass(applicationClass.javaByteCode, applicationClass.name);
      }
      catch (IOException e) {
        Logger.error(e, "Failed to analyze class " + applicationClass.name);
      }
    }

    // Let's dump some metrics and line coverage information:
    for (final IClassCoverage cc : coverageBuilder.getClasses()) {
      out.printf("Coverage of class %s%n", cc.getName());

      printCounter("instructions", cc.getInstructionCounter());
      printCounter("branches", cc.getBranchCounter());
      printCounter("lines", cc.getLineCounter());
      printCounter("methods", cc.getMethodCounter());
      printCounter("complexity", cc.getComplexityCounter());

      for (int i = cc.getFirstLine(); i <= cc.getLastLine(); i++) {
        out.printf("Line %s: %s%n", i, getColor(cc.getLine(i).getStatus()));
      }
    }
  }

  private void writeExecutionDataTo(ExecutionDataStore executionData, SessionInfoStore sessionInfos, String fileName) throws IOException {
    File executionFile = new File(fileName);
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
