package sfdc.sfdx.scanner.paths;

import java.net.URL;

public class PathManipulator {
  private static PathManipulator INSTANCE = null;

  public static PathManipulator getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new PathManipulator();
    }
    return INSTANCE;
  }

  public String convertResourcePathToAbsolutePath(String resourcePath) {
    URL resource = getClass().getClassLoader().getResource(resourcePath);
    return resource.getPath();
  }
}
