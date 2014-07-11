package play.test.coverage;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.classloading.ApplicationClasses;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.results.Result;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.reflect.Modifier.isPublic;
import static java.lang.reflect.Modifier.isStatic;

public class ActionCoveragePlugin extends PlayPlugin {
  @Override public void onApplicationStart() {
    if (!Play.runingInTestMode())
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
    if (Play.mode.isProd()) {
      Logger.info("ActionCoveragePlugin: onApplicationStop");
      
      StringBuilder message = new StringBuilder(1024);
      List<Map.Entry<String, Long>> sorted = sortCounters(actionExecutions);
      message.append("-------------------------------\n");
      message.append("Actions coverage @ ").append(ManagementFactory.getRuntimeMXBean().getName()).append(":\n");
      for (Map.Entry<String, Long> action : sorted) {
        message.append("   ").append(action.getKey()).append(" - tested ").append(action.getValue()).append(" times\n");
      }
      message.append("-------------------------------\n");

      Logger.info(message.toString());
    }
  }

  private List<Map.Entry<String, Long>> sortCounters(Map<String, Long> counters) {
    List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(counters.entrySet());
    Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
      @Override public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return o1.getValue().compareTo(o2.getValue());
      }
    });
    return sorted;
  }
}
