package play.test;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

import static java.io.File.separatorChar;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FilenameUtils.removeExtension;

class ClassesCollection {
  private static final List<String> IGNORED = asList("tmp");

  Set<String> scannedFolders = new HashSet<String>();
  List<Class> allClasses = new ArrayList<Class>(256);

  private String currentCpEntry;

  public List<Class> findTestClasses() throws ClassNotFoundException {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
//    System.out.println("Classloader: " + cl);
    URL[] urls = ((URLClassLoader) cl).getURLs();
//    System.out.println("URLs: " + Arrays.toString(urls));
    for (URL url : urls) {
      scanCpEntry(url.getFile());
    }

    scanCpEntry(new File("test-classes/").getAbsolutePath());

    return allClasses;
  }

  private void scanCpEntry(String cpEntry) throws ClassNotFoundException {
    currentCpEntry = cpEntry;
    System.out.println("SCAN " + cpEntry);
    scanFileOrDirectory(new File(currentCpEntry));
  }

  private void scanFileOrDirectory(File fileOrDirectory) throws ClassNotFoundException {
    if (fileOrDirectory.isDirectory()) {
      scanDirectory(fileOrDirectory);
    }
    if (fileOrDirectory.isFile()) {
      if (fileOrDirectory.getName().endsWith("Test.class") || fileOrDirectory.getName().endsWith("Spec.class")) {
        String className = removeExtension(relativePath(fileOrDirectory)).replace(separatorChar, '.');
//        System.out.println("Found test class: " + className);
        allClasses.add(Class.forName(className));
      }
    }
  }

  private String relativePath(File file) {
    final String relative = file.getAbsolutePath().substring(currentCpEntry.length());
    return relative.startsWith("/") ? relative.substring(1) : relative;
  }

  private void scanDirectory(File dir) throws ClassNotFoundException {
    if (scannedFolders.contains(dir.getAbsolutePath())) {
      return;
    }
    if (IGNORED.contains(dir.getName())) {
      return;
    }

    scannedFolders.add(dir.getAbsolutePath());
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        scanFileOrDirectory(file);
      }
    }
  }
}

