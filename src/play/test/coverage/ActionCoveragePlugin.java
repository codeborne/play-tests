package play.test.coverage;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.results.Result;
import play.test.UITimeLogger;
import play.test.stats.ExecutionTimesWatcher;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.io.FileUtils.writeStringToFile;

public class ActionCoveragePlugin extends PlayPlugin {
  private boolean enabled = Play.runingInTestMode() && Play.mode.isProd();
  public static UITimeLogger timeLogger;
  
  @Override public void onApplicationStart() {
    if (!enabled)
      Play.pluginCollection.disablePlugin(this);

    for (ApplicationClasses.ApplicationClass controller : Play.classes.getAssignableClasses(Controller.class)) {
      for (Method method : controller.javaClass.getDeclaredMethods()) {
        if (isPublic(method.getModifiers()) && isStatic(method.getModifiers()) && method.getReturnType().equals(Void.TYPE)) {
          String action = controller.javaClass.getName().replaceFirst("controllers\\.", "") + '.' + method.getName();
          actionExecutions.put(action, 0L);
        }
      }
    }
  }

  private final Map<String, Long> actionExecutions = new ConcurrentHashMap<>();
  
  @Override public void onActionInvocationResult(Result result) {
    incrementActionCounter();
  }

  @Override public void onInvocationException(Throwable e) {
    if (Http.Request.current() != null) { // request is null when job execution failed
      incrementActionCounter();
    }
  }

  private void incrementActionCounter() {
    Http.Request request = Http.Request.current();

    if (request != null && request.action != null) {
      Long actionCounter = actionExecutions.get(request.action);
      actionCounter = actionCounter == null ? 1 : 1 + actionCounter;
      actionExecutions.put(request.action, actionCounter);
    }
  }

  @Override public void onApplicationStop() {
    if (enabled) {
      storeCoverageToFile("actions-coverage", actionExecutions);
      storeCoverageToFile("tests-statistics-classes", ExecutionTimesWatcher.times.getClassDurations());
      storeCoverageToFile("tests-statistics-methods", ExecutionTimesWatcher.times.getMethodDurations());
      if (timeLogger != null) {
        storeCoverageToFile("webdriver-statistics-operations", timeLogger.times.getClassDurations());
        storeCoverageToFile("webdriver-statistics-calls", timeLogger.times.getMethodDurations());
      }
    }
  }

  private void storeCoverageToFile(final String prefix, Map<String, Long> actionsStatistics) {
    File file = new File("test-result/" + prefix + "-" + ManagementFactory.getRuntimeMXBean().getName() + ".json");
    try {
      writeStringToFile(file, new Gson().toJson(actionsStatistics), "UTF-8");
      System.out.println("Store statistics to " + file.getAbsolutePath());
    }
    catch (IOException e) {
      System.err.println("Failed to store statistics to " + file.getAbsolutePath());
      e.printStackTrace();
    }
  }

  private static List<Map.Entry<String, Long>> sortCounters(Map<String, Long> counters, final int asc) {
    List<Map.Entry<String, Long>> sorted = new ArrayList<>(counters.entrySet());
    Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
      @Override public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return asc * o1.getValue().compareTo(o2.getValue());
      }
    });
    return sorted;
  }
  
  public static void main(String[] args) throws IOException {
    Map<String, Long> actionsExecutions = combineActionsCoveragesFromFiles("actions-coverage");
    System.out.println(
      "-------------------------------\n" +
      formatExecutionStatistics("Actions coverage", actionsExecutions, 1, Integer.MAX_VALUE, "times") +
      calculateCoverage(actionsExecutions) +
      formatExecutionStatistics("Longest test classes", combineActionsCoveragesFromFiles("tests-statistics-classes"), -1, 20, "s") +
      formatExecutionStatistics("Longest test methods", combineActionsCoveragesFromFiles("tests-statistics-methods"), -1, 20, "s") +
      formatExecutionStatistics("Longest webdriver operations", combineActionsCoveragesFromFiles("webdriver-statistics-operations"), -1, 20, "s") +
      formatExecutionStatistics("Longest webdriver calls", combineActionsCoveragesFromFiles("webdriver-statistics-calls"), -1, 20, "s") +
      "-------------------------------\n"
    );
  }

  private static String formatExecutionStatistics(String title, Map<String, Long> executionStatistics, int asc, int maxRecords, final String suffix) {
    StringBuilder message = new StringBuilder(2048);

    List<Map.Entry<String, Long>> sorted = sortCounters(executionStatistics, asc);

    message.append(title).append(":\n");
    int i = 0;
    for (Map.Entry<String, Long> action : sorted) {
      long execution = "s".equals(suffix) ? action.getValue() / 1000000000 : action.getValue();
      message.append("   ").append(action.getKey()).append(" - ").append(execution).append(" ").append(suffix).append("\n");
      if (i++ > maxRecords) break;
    }

    return message.toString();
  }

  private static String calculateCoverage(Map<String, Long> actionExecutions) {
    if (actionExecutions.isEmpty()) return "";

    int coveredActions = 0;
    for (Map.Entry<String, Long> entry : actionExecutions.entrySet()) {
      if (entry.getValue() > 0) coveredActions++;
    }
    return "\nActions coverage: " + 100 * coveredActions / actionExecutions.size() + "%\n\n";
  }

  private static Map<String, Long> combineActionsCoveragesFromFiles(final String prefix) throws IOException {
    Map<String, Long> totalActionExecutions = new HashMap<>();

    System.out.println("Combine statistics from");
    Gson gson = new Gson();
    File testResults = new File("test-result");
    if (!testResults.isDirectory() || testResults.listFiles() == null) {
      System.err.println("ERROR: Cannot combine statistics from " + testResults.getAbsolutePath());
    }
    else {
      for (File file : testResults.listFiles()) {
        if (file.getName().startsWith(prefix + "-")) {
          String temp = FileUtils.readFileToString(file, "UTF-8");
          Map<String, Number> actionExecutions = gson.fromJson(temp, totalActionExecutions.getClass());

          System.out.println("   - " + file.getName());

          if (actionExecutions != null) {
            for (Map.Entry<String, Number> entry : actionExecutions.entrySet()) {
              Long counter = totalActionExecutions.get(entry.getKey());
              if (counter == null) counter = 0L;
              counter += entry.getValue().longValue();
              totalActionExecutions.put(entry.getKey(), counter);
            }
          }
        }
      }
    }
    return totalActionExecutions;
  }
}
