package play.test.stats;

import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ExecutionTimes {

  private static final int PRINT_TOP_RECORDS = 15;

  private final String name;
  private final Map<String, Long> methodDurations = new ConcurrentHashMap<String, Long>();
  private final Map<String, Long> classDurations = new ConcurrentHashMap<String, Long>();

  public ExecutionTimes() {
    this(ManagementFactory.getRuntimeMXBean().getName());
  }

  public ExecutionTimes(String name) {
    this.name = name;
  }

  public void add(String clazz, String method, long durationMs) {
    storeTime(methodDurations, clazz + '.' + method, durationMs);
    storeTime(classDurations, clazz, durationMs);
  }

  private static void storeTime(Map<String, Long> storage, String key, long durationMs) {
    Long total = storage.get(key);
    if (total == null) total = 0L;
    total += durationMs;
    storage.put(key, total);
  }

  public String longestMethods() {
    return print("Longest methods @ " + name, sortDurations(methodDurations));
  }

  public String longestClasses() {
    return print("Longest classes @ " + name, sortDurations(classDurations));
  }

  private String print(final String title, List<Map.Entry<String, Long>> entries) {
    StringBuilder stats = new StringBuilder(title + ":\n");
    int i = 0;
    for (Map.Entry<String, Long> e : entries) {
      if (i++ > PRINT_TOP_RECORDS) break;
      stats.append(String.format("%8d", e.getValue() / 1000000)).append(" ms ").append(e.getKey()).append("\n");
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
