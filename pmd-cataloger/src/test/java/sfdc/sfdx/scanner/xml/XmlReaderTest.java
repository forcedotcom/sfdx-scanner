package sfdc.sfdx.scanner.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static sfdc.sfdx.scanner.TestConstants.*;

import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import sfdc.sfdx.scanner.TestConstants;

/**
 * Unit test for {@link XmlReader}
 */
public class XmlReaderTest {
	private XmlReader xmlReader;
	
	@Before
	public void setup() {
		xmlReader = new XmlReader();
	}
	
	/**
	 * Verify that a file can be read from the classpath. This test relies on {@link #JAR_FILE_CATEGORIES_AND_RULESETS} 
	 */
	@Test
	public void testReadFromJar() {
		// This is contained in the JAR_FILE_CATEGORIES_AND_RULESETS file.
		Document document = xmlReader.getDocumentFromPath("rulesets/apex/rules1.xml");
		assertNotNull(document);
		assertEquals("ruleset", document.getDocumentElement().getTagName());
	}

	@Test
	public void testReadFromFileSystem() {
		Document document = xmlReader.getDocumentFromPath(XML_FILE.toAbsolutePath().toString());
		assertNotNull(document);
		assertEquals("ruleset", document.getDocumentElement().getTagName());
	}
}
