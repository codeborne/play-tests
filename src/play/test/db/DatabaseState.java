package play.test.db;

import play.Logger;
import play.db.DB;

import java.io.File;
import java.lang.management.ManagementFactory;

public class DatabaseState {
  public File dumpFile;

  public DatabaseState() {
    String jvmName = ManagementFactory.getRuntimeMXBean().getName();
    this.dumpFile = new File("tmp/tests.backup." + System.nanoTime() + '_' + jvmName + ".sql");
  }

  public DatabaseState(File dumpFile) {
    this.dumpFile = dumpFile;
  }

  public boolean isSaved() {
    return dumpFile.exists();
  }

  public void save() {
    DB.execute("CHECKPOINT SYNC");
    DB.execute("script drop to '" + dumpFile.getPath() + "'");
    Logger.info("Stored initial database state to " + dumpFile.getPath());
  }

  public void restore() {
    long start = System.currentTimeMillis();
    DB.execute("CHECKPOINT SYNC");
    DB.execute("runscript from '" + dumpFile + "'");
    long end = System.currentTimeMillis();
    Logger.info("Restored initial database state from " + dumpFile + " in " + (end - start) + " ms");
  }
}
