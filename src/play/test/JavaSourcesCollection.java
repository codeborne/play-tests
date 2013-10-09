package play.test;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.io.File.separatorChar;
import static org.apache.commons.io.FilenameUtils.removeExtension;

class JavaSourcesCollection {
  private final List<String> sourceRoots = new ArrayList<>();
  private final List<String> sourceFiles = new ArrayList<>(256);
  private final List<String> classNames = new ArrayList<>(256);

  private String currentSourceRoot;

  public JavaSourcesCollection() throws IOException {
    sourceRoots.add("app");
    sourceRoots.add("test");
    File modules = new File("modules");
    if (modules.exists() && modules.isDirectory() && modules.listFiles() != null) {
      for (File module : modules.listFiles()) {
        module = resolveModule(module);
        File moduleSourceDir = new File(module, "app");
        if (moduleSourceDir.exists()) {
          sourceRoots.add(moduleSourceDir.getAbsolutePath());
        }
      }
    }
  }

  private File resolveModule(File module) throws IOException {
    if (module.isDirectory()) {
      return module;
    }
    String moduleFilePath = FileUtils.readFileToString(module).trim();
    return new File(moduleFilePath);
  }

  public JavaSourcesCollection scan() {
    System.out.println("SCAN " + sourceRoots);
    for (String sourceRoot : sourceRoots) {
      scanSourceRoot(sourceRoot);
    }

    return this;
  }

  private void scanSourceRoot(String sourceRoot) {
    final File dir = new File(sourceRoot);
    currentSourceRoot = dir.getAbsolutePath();
    System.out.println("SCAN " + currentSourceRoot);
    scanFileOrDirectory(dir);
  }

  private void scanFileOrDirectory(File fileOrDirectory) {
    if (fileOrDirectory.isDirectory()) {
      scanDirectory(fileOrDirectory);
    }
    if (fileOrDirectory.isFile()) {
      if (fileOrDirectory.getName().endsWith(".java")) {
        String sourceFile = relativePath(fileOrDirectory);
        sourceFiles.add(currentSourceRoot + '/' + sourceFile);

        if (fileOrDirectory.getName().endsWith("Test.java") || fileOrDirectory.getName().endsWith("Spec.java")) {
          String className = removeExtension(sourceFile).replace(separatorChar, '.');
          classNames.add(className);
        }
      }
    }
  }

  private String relativePath(File file) {
    String relative = file.getAbsolutePath().substring(currentSourceRoot.length());
    return relative.startsWith("/") ? relative.substring(1) : relative;
  }

  private void scanDirectory(File dir) {
    File[] files = dir.listFiles();
    if (files != null) {
      for (File file : files) {
        scanFileOrDirectory(file);
      }
    }
  }

  List<String> getSourceRoots() {
    return sourceRoots;
  }

  public List<String> getSourceFiles() {
    return sourceFiles;
  }

  public List<String> getClassNames() {
    return classNames;
  }

  public List<Class> getClasses() throws ClassNotFoundException {
    List<Class> classes = new ArrayList<>(classNames.size());
    for (String className : classNames) {
      classes.add(Class.forName(className));
    }
    return classes;
  }
}

