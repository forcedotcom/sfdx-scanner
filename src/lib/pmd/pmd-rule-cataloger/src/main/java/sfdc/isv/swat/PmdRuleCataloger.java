package sfdc.isv.swat;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

public class PmdRuleCataloger {
  public void doTheThing(String[] args) {
    List<String> files = getFilesFromJar(args[0]);
    logFileContents(files.get(0));
  }

  private List<String> getFilesFromJar(String jarName) {
    List<String> foundFiles = new ArrayList<>();
    try {
      JarInputStream jstream = new JarInputStream(new FileInputStream(jarName));
      JarEntry entry;

      while ((entry = jstream.getNextJarEntry()) != null) {
        String name = entry.getName();
        if (name.endsWith(".xml") && (name.startsWith("category") || name.startsWith("rulesets"))) {
          foundFiles.add(name);
          System.out.println("Found file " + name + " in jar " + jarName);
        }
      }
    } catch (FileNotFoundException fnf) {
      System.out.println("FileNotFound Exception: " + fnf.getMessage());
    } catch (IOException io) {
      System.out.println("IOException: " + io.getMessage());
    }
    return foundFiles;
  }

  private void logFileContents(String path) {
    List<String> contents = new ArrayList<>();
    try (
      InputStream in = getResourceAsStream(path);
      BufferedReader br = new BufferedReader(new InputStreamReader(in))
    ) {
      String resource;
      while ((resource = br.readLine()) != null) {
        System.out.println(resource);
        contents.add(resource);
      }
    } catch (IOException io) {
      System.out.println("IOException: " + io.getMessage());
    }
  }

  private InputStream getResourceAsStream(String path) {
    final InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
    return in == null ? getClass().getResourceAsStream(path) : in;
  }
}
