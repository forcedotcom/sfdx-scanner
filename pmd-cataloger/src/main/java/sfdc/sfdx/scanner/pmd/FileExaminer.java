package sfdc.sfdx.scanner.pmd;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/***
 * Examines files and helps scout for jar files and XMLs
 * @author rmohan
 * Feb 2020
 */
public class FileExaminer {

    enum FileType {
        JAR(".jar"),
        XML(".xml");

        String suffix;

        FileType(String suffix) {
            this.suffix = suffix;
        }
    }


    /***
     * Find XML in a given path
     * @param pathString - could be a directory with classes and/or jar files, or a single jar file
     * @return a list of XML files found
     */
    public List<String> findXmlInPath(String pathString) {

        final List<String> xmlFiles = new ArrayList<>();
        final List<String> jarFiles = new ArrayList<>();

        final Path path = Paths.get(pathString);

        if (!Files.exists(path)) {
            throw new ScannerPmdException("Path does not exist: " + path.getFileName());
        }


        if (Files.isDirectory(path)) {
            // Recursively walk through the directory and scout for XML/JAR files
            xmlFiles.addAll(scoutForFiles(FileType.XML, path));
            jarFiles.addAll(scoutForFiles(FileType.JAR, path));

        } else if (Files.isRegularFile(path)
            && path.getFileName().endsWith(FileType.JAR.suffix)) { // Check if the path we have is a jar file

            jarFiles.add(path.toString());
        }

        jarFiles.forEach(jarPath -> xmlFiles.addAll(findXmlInJar(jarPath)));

        return xmlFiles;
    }


    public List<String> findXmlInJar(String jarPath) {

      final List<String> xmlFiles = new ArrayList<String>();

        // TODO: can we move this high-level try block to another place?
        try {
          JarInputStream jstream = null;
          try {
                jstream = new JarInputStream(new FileInputStream(jarPath));

              JarEntry entry;
              while ((entry = jstream.getNextJarEntry()) != null) {
                    String fName = entry.getName();
                    // For now, take all .xml files. We can ignore non-ruleset XMLs while parsing
                    if (fName.endsWith(FileType.XML.suffix)) {
                        xmlFiles.add(fName);
                    }
                }
            } finally {
                if (jstream != null) {
                    jstream.close();
                }
            }
        } catch (Exception e) {
            // Wrap all exceptions into ScannerPmdException
            throw new ScannerPmdException("Unable to read jar file: " + jarPath, e);
        }

        return xmlFiles;
    }

    private List<String> scoutForFiles(FileType fileType, Path path) {
        final List<String> filesFound = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(path)) {
             filesFound.addAll( walk.map(x -> x.toString())
                        .filter(f -> f.endsWith(fileType.suffix)).collect(Collectors.toList()));
        } catch (IOException e) {
            throw new ScannerPmdException("Error parsing directory: " + path, e);
        }

        return filesFound;
    }
}
