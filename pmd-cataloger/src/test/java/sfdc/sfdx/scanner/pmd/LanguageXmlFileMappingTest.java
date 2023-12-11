package sfdc.sfdx.scanner.pmd;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static sfdc.sfdx.scanner.TestConstants.APEX;
import static sfdc.sfdx.scanner.TestConstants.JAVA;
import static sfdc.sfdx.scanner.TestConstants.SOMECAT_XML_FILE;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.MessagePassableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Unit test for {@link LanguageXmlFileMapping}
 */
public class LanguageXmlFileMappingTest {
	private LanguageXmlFileMapping languageXmlFileMapping;

	@BeforeEach
	public void setup() {
		languageXmlFileMapping = new LanguageXmlFileMapping();
	}

	@Test
	public void testCategories() {
		Map<String, Set<String>> pathMap;
		Set<String> paths;

		setupCategoriesAndRulesets();

		pathMap = languageXmlFileMapping.getCategoryPaths();
		assertThat(pathMap.keySet(), hasSize(equalTo(2)));
		assertThat(pathMap.keySet(), hasItems(APEX.toLowerCase(), JAVA.toLowerCase()));

		paths = pathMap.get(APEX.toLowerCase());
		assertThat(paths, hasSize(equalTo(4)));
		assertThat(paths, hasItems("category/apex/performance1.xml", "category/apex/performance2.xml",
				"category/apex/performance3.xml", "category/apex/performance4.xml"));

		paths = pathMap.get(JAVA.toLowerCase());
		assertThat(paths, hasSize(equalTo(2)));
		assertThat(paths, hasItems("category/java/performance1.xml", "category/java/performance2.xml"));
	}

	@Test
	public void testRulesets() {
		Map<String, Set<String>> pathMap;
		Set<String> paths;

		setupCategoriesAndRulesets();

		pathMap = languageXmlFileMapping.getRulesetPaths();
		assertThat(pathMap.keySet(), hasSize(equalTo(2)));
		assertThat(pathMap.keySet(), hasItems(APEX.toLowerCase(), JAVA.toLowerCase()));

		paths = pathMap.get(APEX.toLowerCase());
		assertThat(paths, hasSize(equalTo(4)));
		assertThat(paths, hasItems("rulesets/apex/performance1.xml", "rulesets/apex/performance2.xml",
				"rulesets/apex/performance3.xml", "rulesets/apex/performance4.xml"));

		paths = pathMap.get(JAVA.toLowerCase());
		assertThat(paths, hasSize(equalTo(2)));
		assertThat(paths, hasItems("rulesets/java/performance1.xml", "rulesets/java/performance2.xml"));
	}

	@Test
	public void testXml() {
		String xmlPath = SOMECAT_XML_FILE.toAbsolutePath().toString();
		Map<String, Set<String>> pathMap;
		Set<String> paths;

		XmlFileFinder.XmlContainer xmlContainer1 = new XmlFileFinder.XmlContainer(xmlPath, Arrays.asList(xmlPath));

		languageXmlFileMapping.addPathsForLanguage(Arrays.asList(xmlContainer1), APEX);

		pathMap = languageXmlFileMapping.getCategoryPaths();
		assertThat(pathMap.keySet(), hasSize(equalTo(1)));
		assertThat(pathMap.keySet(), hasItems(APEX.toLowerCase()));

		paths = pathMap.get(APEX.toLowerCase());
		assertThat(paths, hasSize(equalTo(1)));
		assertThat(paths, hasItems(xmlPath));
	}

	/**
	 * Prevent users from adding categories that have the same relative path. This
	 * prevents unpredictable behavior when PMD is invoked.
	 */
	@Test
	public void testExceptionIsThrownWhenCategoryPathCollides() {
		testCollision("category/performance.xml");
	}

	/**
	 * Prevent users from adding rulesets that have the same relative path. This
	 * prevents unpredictable behavior when PMD is invoked.
	 */
	@Test
	public void testExceptionIsThrownWhenRulesetPathCollides() {
		testCollision("rulesets/performance.xml");
	}

	/**
	 * Verify that a colliding relative path throws the expected exception.
	 */
	private void testCollision(String collidingPath) {
		String jar1 = "jar1.jar";
		String jar2 = "jar2.jar";

		XmlFileFinder.XmlContainer xmlContainer1 = new XmlFileFinder.XmlContainer(jar1, Arrays.asList(collidingPath));
		XmlFileFinder.XmlContainer xmlContainer2 = new XmlFileFinder.XmlContainer(jar2, Arrays.asList(collidingPath));

		languageXmlFileMapping.addPathsForLanguage(Arrays.asList(xmlContainer1), APEX);

        MessagePassableException ex = assertThrows(MessagePassableException.class, () -> languageXmlFileMapping.addPathsForLanguage(Arrays.asList(xmlContainer2), APEX));
        assertThat(ex.getEventKey(), is(EventKey.ERROR_EXTERNAL_DUPLICATE_XML_PATH));
        assertThat(ex.getArgs(), is(new String[] { collidingPath, jar2, jar1 }));
	}

	private void setupCategoriesAndRulesets() {
		XmlFileFinder.XmlContainer xmlContainer1 = new XmlFileFinder.XmlContainer("jar1.jar",
				Arrays.asList("category/apex/performance1.xml", "category/apex/performance2.xml",
						"rulesets/apex/performance1.xml", "rulesets/apex/performance2.xml"));

		XmlFileFinder.XmlContainer xmlContainer2 = new XmlFileFinder.XmlContainer("jar2.jar",
				Arrays.asList("category/apex/performance3.xml", "category/apex/performance4.xml",
						"rulesets/apex/performance3.xml", "rulesets/apex/performance4.xml"));

		XmlFileFinder.XmlContainer xmlContainer3 = new XmlFileFinder.XmlContainer("jar3.jar",
				Arrays.asList("category/java/performance1.xml", "category/java/performance2.xml",
						"rulesets/java/performance1.xml", "rulesets/java/performance2.xml"));

		languageXmlFileMapping.addPathsForLanguage(Arrays.asList(xmlContainer1, xmlContainer2), APEX);
		languageXmlFileMapping.addPathsForLanguage(Arrays.asList(xmlContainer3), JAVA);

	}
}
