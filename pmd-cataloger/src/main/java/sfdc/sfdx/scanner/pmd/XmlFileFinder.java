package sfdc.sfdx.scanner.pmd;

import sfdc.sfdx.scanner.messaging.SfdxScannerException;
import sfdc.sfdx.scanner.messaging.EventKey;
import sfdc.sfdx.scanner.messaging.SfdxMessager;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
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
public class XmlFileFinder {

	enum FileType {
		JAR(".jar"),
		XML(".xml");

		String suffix;

		FileType(String suffix) {
			this.suffix = suffix;
		}
	}

	/**
	 * Represents a container for XML. This can either be a Jar file which may contain multiple XML files or an XML
	 * file that contains itself.
	 */
	public static final class XmlContainer {
		/**
		 * This is the file path to either a Jar file or an XML file.
		 */
		public final String filePath;

		/**
		 * In the case of a Java Jar, this contains path to all XML files in that Jar. In the case of an XML file, this
		 * contains a single value that is the same as {@link #filePath}. This allows the caller to treat them the same.
		 */
		public final List<String> containedFilePaths;

		public XmlContainer(String filePath) {
			this(filePath, Collections.singletonList(filePath));
		}

		public XmlContainer(String filePath, List<String> containedFilePaths) {
			this.filePath = filePath;
			this.containedFilePaths = containedFilePaths;
		}
	}

	/***
	 * Find XML in a given path
	 * @param pathString - could be a directory with classes and/or jar files, or a single jar file
	 * @return a list of XML files found
	 */
	public List<XmlContainer> findXmlFilesInPath(String pathString) {

		final List<String> xmlFiles = new ArrayList<>();
		final List<String> jarFiles = new ArrayList<>();

		final Path path = Paths.get(pathString);

		List<XmlContainer> xmlContainers = new ArrayList<>(); 

		// Make sure that the path exists to begin with
		if (!Files.exists(path)) {
			throw new SfdxScannerException(EventKey.ERROR_INTERNAL_CLASSPATH_DOES_NOT_EXIST, path.getFileName().toString());
		}

		if (Files.isDirectory(path)) {
			// Recursively walk through the directory and scout for XML/JAR files
			xmlFiles.addAll(scoutForFiles(FileType.XML, path));
			jarFiles.addAll(scoutForFiles(FileType.JAR, path));

		} else if (Files.isRegularFile(path)) {
			if (path.toString().endsWith(FileType.JAR.suffix)) { // Check if the path we have is a jar file
				jarFiles.add(path.toString());
			} else if (path.toString().endsWith(FileType.XML.suffix)) {
				xmlFiles.add(path.toString());
			}
		}

		xmlFiles.forEach(x -> xmlContainers.add(new XmlContainer(x)));

		jarFiles.forEach(jarPath -> {
			List<String> jarXmlFiles = findXmlFilesInJar(jarPath);
			if (!jarXmlFiles.isEmpty()) {
				xmlContainers.add(new XmlContainer(jarPath, jarXmlFiles));
			}
		});

		return xmlContainers;
	}

	/**
	 * Read through file entries in a Jar manifest and identify all XML files
	 */
	public List<String> findXmlFilesInJar(String jarPath) {

		final List<String> xmlFiles = new ArrayList<String>();
		try (
			JarInputStream jstream = new JarInputStream(new FileInputStream(jarPath));
		) {

			JarEntry entry;
			while ((entry = jstream.getNextJarEntry()) != null) {
				String fName = entry.getName();
				// For now, take all .xml files. We can ignore non-ruleset XMLs while parsing
				if (fName.endsWith(FileType.XML.suffix)) {
					xmlFiles.add(fName);
				}
			}

			SfdxMessager.getInstance().addMessage("", EventKey.INFO_JAR_AND_XML_PROCESSED, jarPath, xmlFiles.toString());
		} catch (Exception e) {
			//TODO: add logging and print stacktrace for debugging
			throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_JAR_NOT_READABLE, e, jarPath);
		}

		return xmlFiles;
	}

	/**
	 * Walks a directory path to identify files of a requested type
	 */
	private List<String> scoutForFiles(FileType fileType, Path path) {
		final List<String> filesFound = new ArrayList<>();

		// Create a stream of Paths for the contents of the directory
		try (Stream<Path> walk = Files.walk(path)) {

			// Filter the stream to find only Paths that match the filetype we are looking
			filesFound.addAll(walk.map(x -> x.toString())
				.filter(f -> f.endsWith(fileType.suffix)).collect(Collectors.toList()));
		} catch (IOException e) {
			throw new SfdxScannerException(EventKey.ERROR_EXTERNAL_DIR_NOT_READABLE, e, path.toString());
		}

		return filesFound;
	}
}
