package play.test;

import org.apache.commons.io.FileUtils;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileFilter;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class Compiler {
  private static final FileFilter NON_JAVA_FILES = new FileFilter() {
    @Override public boolean accept(File pathname) {
      return !pathname.getName().endsWith(".java");
    }
  };

  public static void addToClasspath(URL url) throws Exception {
    URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
    Class<URLClassLoader> sysclass = URLClassLoader.class;

    Method method = sysclass.getDeclaredMethod("addURL", URL.class);
    method.setAccessible(true);
    method.invoke(sysloader, new Object[] { url });
  }

  public static void compile(List<String> resourceRoots, List<String> javaSources, String output) throws Exception {
    System.out.println("Compile " + javaSources.size() + " java files");

    String[] args = new String[javaSources.size() + 3];
    args[0] = "-g";
    args[1] = "-d";
    args[2] = output;
    for (int i = 0; i < javaSources.size(); i++) {
      args[i+3] = javaSources.get(i);
    }
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    compiler.run(null, System.out, System.err, args);

    File target = new File(output);
    for (String resourceRoot : resourceRoots) {
      FileUtils.copyDirectory(new File(resourceRoot), target, NON_JAVA_FILES);
    }

    addToClasspath(target.toURI().toURL());
  }

  public static void main(String[] args) throws Exception {
    JavaSourcesCollection sources = new JavaSourcesCollection().scan();
    Compiler.compile(sources.getSourceRoots(), sources.getSourceFiles(), "test-classes");
//
//    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
//    args = new String[]{"test/controllers/BanklinkTest.java"};
//    compiler.run(null, null, null, args);
  }
}
