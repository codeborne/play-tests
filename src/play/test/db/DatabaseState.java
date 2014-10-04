package play.test.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.DB;

import java.io.File;
import java.lang.management.ManagementFactory;

public class DatabaseState {
  static final Logger logger = LoggerFactory.getLogger(DatabaseState.class);
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
    logger.info("Stored initial database state to " + dumpFile.getPath());
  }

  public void restore() {
    long start = System.currentTimeMillis();
    DB.execute("CHECKPOINT SYNC");
    DB.execute("runscript from '" + dumpFile + "'");
    long end = System.currentTimeMillis();
    logger.info("Restored initial database state from " + dumpFile + " in " + (end - start) + " ms");
  }
}
