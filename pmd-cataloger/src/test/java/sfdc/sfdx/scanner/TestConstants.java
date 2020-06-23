package sfdc.sfdx.scanner;

import static sfdc.sfdx.scanner.TestConstants.JAR_FILE_CATEGORIES_AND_RULESETS;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Constants used by more than one unit test
 */
public class TestConstants {
	public static final String APEX = "Apex";
	public static final String JAVA = "Java";

	public static final Path TEST_DIR = Paths.get("..", "test");
	public static final Path TEST_JAR_DIR = TEST_DIR.resolve("test-jars");
	public static final Path TEST_XML_DIR = TEST_DIR.resolve("test-xml");
	public static final Path COLLISION_DIR = TEST_JAR_DIR.resolve("negative").resolve("collision-test");
	public static final Path COLLISION_JAR_1 = COLLISION_DIR.resolve("collision-test-1.jar");
	public static final Path COLLISION_JAR_2 = COLLISION_DIR.resolve("collision-test-2.jar");
	public static final Path TEST_JAR_APEX_DIR = TEST_JAR_DIR.resolve("apex");
	public static final Path JAR_FILE_CATEGORIES_AND_RULESETS = TEST_JAR_APEX_DIR
			.resolve("testjar-categories-and-rulesets-1.jar");
	public static final Path XML_FILE = TEST_XML_DIR.resolve("category").resolve("apex").resolve("somecat.xml");
}