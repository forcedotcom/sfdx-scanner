package sfdc.sfdx.scanner.pmd;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static sfdc.sfdx.scanner.TestConstants.JAR_FILE_CATEGORIES_AND_RULESETS;
import static sfdc.sfdx.scanner.TestConstants.TEST_JAR_DIR;
import static sfdc.sfdx.scanner.TestConstants.TEST_XML_DIR;
import static sfdc.sfdx.scanner.TestConstants.XML_FILE;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import com.salesforce.messaging.EventKey;

/**
 * Unit tests for {@link XmlFileFinder}
 */
public class XmlFileFinderTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void testAllJarsInDiretory() {
		XmlFileFinder xmlFileFinder = new XmlFileFinder();

		List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder
				.findXmlFilesInPath(TEST_JAR_DIR.toAbsolutePath().toString());
		assertThat(xmlContainers, hasSize(equalTo(7)));

		// Verify the contents of each jar
		assertEquals("category/joshapex/somecat1.xml", getSingleFile(xmlContainers, "testjar1.jar"));
		assertEquals("category/joshapex/somecat2.xml", getSingleFile(xmlContainers, "testjar2.jar"));
		assertEquals("category/joshapex/somecat3.xml", getSingleFile(xmlContainers, "testjar3.jar"));
		assertEquals("category/joshapex/somecat4.xml", getSingleFile(xmlContainers, "testjar4.jar"));
		assertEquals("category/joshapex/somecat.xml", getSingleFile(xmlContainers, "collision-test-1.jar"));
		assertEquals("category/joshapex/somecat.xml", getSingleFile(xmlContainers, "collision-test-2.jar"));

		XmlFileFinder.XmlContainer xmlContainer = findXmlContainer(xmlContainers, "testjar-categories-and-rulesets-1.jar");
		assertThat(xmlContainer.containedFilePaths, hasSize(equalTo(2)));
		assertThat(xmlContainer.containedFilePaths, hasItems("category/apex/cat1.xml", "rulesets/apex/rules1.xml"));
	}

	@Test
	public void testSingleJar() {
		XmlFileFinder xmlFileFinder = new XmlFileFinder();

		List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder
				.findXmlFilesInPath(JAR_FILE_CATEGORIES_AND_RULESETS.toAbsolutePath().toString());
		assertThat(xmlContainers, hasSize(equalTo(1)));

		XmlFileFinder.XmlContainer xmlContainer = xmlContainers.get(0);
		assertEquals(JAR_FILE_CATEGORIES_AND_RULESETS.toAbsolutePath().toString(), xmlContainer.filePath);
		assertThat(xmlContainer.containedFilePaths, hasSize(equalTo(2)));
		assertThat(xmlContainer.containedFilePaths, hasItems("category/apex/cat1.xml", "rulesets/apex/rules1.xml"));
	}

	@Test
	public void testAllXmlFilesInDirectory() {
		XmlFileFinder xmlFileFinder = new XmlFileFinder();

		List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder
				.findXmlFilesInPath(TEST_XML_DIR.toAbsolutePath().toString());
		assertThat(xmlContainers, hasSize(equalTo(1)));
		XmlFileFinder.XmlContainer xmlContainer = xmlContainers.get(0);
		assertEquals(XML_FILE.toAbsolutePath().toString(), xmlContainer.filePath);
		assertThat(xmlContainer.containedFilePaths, hasSize(equalTo(1)));
		assertThat(xmlContainer.containedFilePaths, contains(XML_FILE.toAbsolutePath().toString()));
	}

	@Test
	public void testSingleXmlFile() {
		XmlFileFinder xmlFileFinder = new XmlFileFinder();

		List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder
				.findXmlFilesInPath(XML_FILE.toAbsolutePath().toString());
		assertThat(xmlContainers, hasSize(equalTo(1)));
		XmlFileFinder.XmlContainer xmlContainer = xmlContainers.get(0);
		assertEquals(XML_FILE.toAbsolutePath().toString(), xmlContainer.filePath);
		assertThat(xmlContainer.containedFilePaths, hasSize(equalTo(1)));
		assertThat(xmlContainer.containedFilePaths, contains(XML_FILE.toAbsolutePath().toString()));
	}

	private XmlFileFinder.XmlContainer findXmlContainer(List<XmlFileFinder.XmlContainer> xmlContainers, String jarName) {
		for (XmlFileFinder.XmlContainer xmlContainer : xmlContainers) {
			if (xmlContainer.filePath.endsWith(jarName)) {
				return xmlContainer;
			}
		}

		throw new RuntimeException("Uanble to find " + jarName);
	}

	private String getSingleFile(List<XmlFileFinder.XmlContainer> xmlContainers, String jarName) {
		XmlFileFinder.XmlContainer xmlContainer = findXmlContainer(xmlContainers, jarName);
		assertThat(xmlContainer.containedFilePaths, hasSize(equalTo(1)));

		return xmlContainer.containedFilePaths.get(0);
	}

	@Test
	public void testFindingNonExistentFile_ExpectError() {
		XmlFileFinder xmlFileFinder = new XmlFileFinder();
		thrown.expect(new MessagePassableExceptionMatcher(EventKey.ERROR_INTERNAL_CLASSPATH_DOES_NOT_EXIST, new String[]{"nonexistentfile.xml"}));

		List<XmlFileFinder.XmlContainer> xmlContainers = xmlFileFinder.findXmlFilesInPath("nonexistentfile.xml");
	}
}
