package play.test.stats;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionTimes {

  private final Map<String, Long> methodDurations = new ConcurrentHashMap<String, Long>();
  private final Map<String, Long> classDurations = new ConcurrentHashMap<String, Long>();
  
  public void add(String clazz, String method, long durationMs) {
    methodDurations.put(clazz + '.' + method, durationMs);
    
    Long classTotal = classDurations.get(clazz);
    if (classTotal == null) classTotal = 0L;
    classTotal += durationMs;
    classDurations.put(clazz, classTotal);
  }

  public String longestMethods() {
    return print("Longest methods", sortDurations(methodDurations));
  }

  public String longestClasses() {
    return print("Longest classes", sortDurations(classDurations));
  }

  private String print(final String title, List<Map.Entry<String, Long>> entries) {
    StringBuilder stats = new StringBuilder(title + ":\n");
    for (Map.Entry<String, Long> e : entries) {
      stats.append(String.format("%8d", e.getValue())).append(" ms ").append(e.getKey()).append("\n");
    }
    return stats.toString();
  }

  private List<Map.Entry<String, Long>> sortDurations(Map<String, Long> durations) {
    List<Map.Entry<String, Long>> sorted = new ArrayList<Map.Entry<String, Long>>(durations.entrySet());
    Collections.sort(sorted, new Comparator<Map.Entry<String, Long>>() {
      @Override public int compare(Map.Entry<String, Long> o1, Map.Entry<String, Long> o2) {
        return o2.getValue().compareTo(o1.getValue());
      }
    });
    return sorted;
  }
}
