package play.test.coverage;

import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.results.Result;

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

  private final Map<String, Long> actionExecutions = new ConcurrentHashMap<String, Long>();
  
  @Override public void onActionInvocationResult(Result result) {
    incrementActionCounter();
  }

  @Override public void onInvocationException(Throwable e) {
    incrementActionCounter();
  }

  private void incrementActionCounter() {
    Http.Request request = Http.Request.current();

    Long actionCounter = actionExecutions.get(request.action);
    actionCounter = actionCounter == null ? 1 : 1 + actionCounter;
    actionExecutions.put(request.action, actionCounter);
  }

  @Override public void onApplicationStop() {
    if (enabled) 
      storeCoverageToFile();
  }

  private void storeCoverageToFile() {
    File file = new File("test-result/actions-coverage-" + ManagementFactory.getRuntimeMXBean().getName() + ".json");
    try {
      writeStringToFile(file, new Gson().toJson(actionExecutions));
    }
    catch (IOException e) {
      System.err.println("Failed to store actions coverage to " + file.getAbsolutePath());
      e.printStackTrace();
    }
  }

  private static List<Map.Entry<String, Long>> sortCounters(Map<String, Long> counters) {
    List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(counters.entrySet());
    Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
      @Override public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return o1.getValue().compareTo(o2.getValue());
      }
    });
    return sorted;
  }
  
  public static void main(String[] args) throws IOException {
    System.out.println(
        formatActionsCoverage(
            combineActionsCoveragesFromFiles()
        )
    );
  }

  private static StringBuilder formatActionsCoverage(Map<String, Long> totalActionExecutions) {
    StringBuilder message = new StringBuilder(2048);

    message.append("ActionCoveragePlugin\n");

    List<Map.Entry<String, Long>> sorted = sortCounters(totalActionExecutions);
    message.append("-------------------------------\n");
    message.append("Actions coverage:\n");
    for (Map.Entry<String, Long> action : sorted) {
      message.append("   ").append(action.getKey()).append(" - tested ").append(action.getValue()).append(" times\n");
    }
    message.append("-------------------------------\n");
    return message;
  }

  private static Map<String, Long> combineActionsCoveragesFromFiles() throws IOException {
    Map<String, Long> totalActionExecutions = new HashMap<String, Long>();

    System.out.println("Combine actions coverage from");
    Gson gson = new Gson();
    File testResults = new File("test-result");
    if (!testResults.isDirectory() || testResults.listFiles() == null) {
      System.err.println("ERROR: Cannot combine actions coverage from " + testResults.getAbsolutePath());
    }
    else {
      for (File file : testResults.listFiles()) {
        if (file.getName().startsWith("actions-coverage-")) {
          String temp = FileUtils.readFileToString(file, "UTF-8");
          Map<String, Long> actionExecutions = gson.fromJson(temp, totalActionExecutions.getClass());

          System.out.println("   - " + file.getName());

          for (Map.Entry<String, Long> entry : actionExecutions.entrySet()) {
            Long counter = totalActionExecutions.get(entry.getKey());
            if (counter == null) counter = 0L;
            counter += ((Number) entry.getValue()).longValue();
            totalActionExecutions.put(entry.getKey(), counter);
          }
        }
      }
    }
    return totalActionExecutions;
  }
}
