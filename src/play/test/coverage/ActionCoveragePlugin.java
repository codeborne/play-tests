package play.test.coverage;

import play.Logger;
import play.Play;
import play.PlayPlugin;
import play.mvc.Http;
import play.mvc.Router;
import play.mvc.results.Result;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ActionCoveragePlugin extends PlayPlugin {
  @Override public void onApplicationStart() {
    if (!Play.runingInTestMode())
      Play.pluginCollection.disablePlugin(this);

    for (Router.Route route : Router.routes) {
      actionExecutions.put(route.action, 0L);
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
    Logger.info("ActionCoveragePlugin: onApplicationStop");
    List<Map.Entry<String, Long>> sorted = sortCounters(actionExecutions);
    System.out.println("-------------------------------");
    System.out.println("Actions coverage:");
    for (Map.Entry<String, Long> action : sorted) {
      System.out.println("   " + action.getKey() + " - tested " + action.getValue() + " times");
    }
    System.out.println("-------------------------------");
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
