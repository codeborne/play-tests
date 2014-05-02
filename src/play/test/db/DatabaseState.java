package play.test.db;

import play.db.DB;

import java.lang.management.ManagementFactory;

public class DatabaseState {
  public static String initialDatabaseStateFile;

  public static boolean isSaved() {
    return initialDatabaseStateFile != null;
  }

  public static void save() {
    String jvmName = ManagementFactory.getRuntimeMXBean().getName();
    String fileName = "tmp/tests.backup." + System.nanoTime() + '_' + jvmName + ".sql";
    DB.execute("CHECKPOINT SYNC");
    DB.execute("script drop to '" + fileName + "'");
    play.Logger.info("Stored initial database state to " + fileName);

    initialDatabaseStateFile = fileName;
  }

  public static void restore() {
    long start = System.currentTimeMillis();
    DB.execute("CHECKPOINT SYNC");
    DB.execute("runscript from '" + initialDatabaseStateFile + "'");
    long end = System.currentTimeMillis();
    play.Logger.info("Restored initial database state from " + initialDatabaseStateFile +
        " in " + (end - start) + " ms.");
  }
}
