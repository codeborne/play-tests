package play.test.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.DB;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.sql.ResultSet;
import java.sql.SQLException;

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
//    DB.execute("CHECKPOINT SYNC");
    DB.execute("script drop to '" + dumpFile.getPath() + "'");
    showTables();
    logger.info("Stored initial database state to " + dumpFile.getPath());
  }

  public void restore() {
    long start = System.currentTimeMillis();
//    DB.execute("CHECKPOINT SYNC");
    DB.execute("runscript from '" + dumpFile + "'");
    showTables();
    long end = System.currentTimeMillis();
    logger.info("Restored initial database state from " + dumpFile + " in " + (end - start) + " ms");
  }

  private void showTables() {
    logger.info("Tables:");
    try {
      ResultSet rowSet = DB.executeQuery("show tables");
      try {
        while (rowSet.next()) {
          logger.info(rowSet.getString(1));
        }
        logger.info("   ");
      }
      finally {
        rowSet.close();
      }
    }
    catch (SQLException e) {
      e.printStackTrace();
    }
  }
}
