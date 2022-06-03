package sfdc.sfdx.scanner.pmd.catalog;

import com.salesforce.messaging.EventKey;
import org.json.simple.JSONObject;

import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;

import static org.mockito.Mockito.*;

import org.junit.rules.ExpectedException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import sfdc.sfdx.scanner.pmd.MessagePassableExceptionMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class PmdCatalogRuleTest {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private static final String NAME = "Name";
	private static final String MESSAGE = "Some message";
	private static final String LANGUAGE = "apex";

	private static final String CATEGORY_NAME = "Best Practices";
	private static final String CATEGORY_PATH = "/some/path";
	private static final String CATEGORY_SOURCEJAR = "/path/to/sourcejar.jar";
	private static final PmdCatalogCategory CATEGORY = new PmdCatalogCategory(CATEGORY_NAME, CATEGORY_PATH, CATEGORY_SOURCEJAR);

	@Test
	public void testCatalogRuleJsonConversion() {
		// Setup mock
		final Element elementMock = getElementMock(Collections.singletonList("description"));
		final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);


		// Execute
		final JSONObject jsonObject = catalogRule.toJson();


		// Validate
		assertEquals("Unexpected name on JSON", NAME, jsonObject.get(PmdCatalogJson.JSON_NAME));

		assertEquals("Unexpected message", MESSAGE, jsonObject.get(PmdCatalogJson.JSON_MESSAGE));

		final List<String> expectedLanguages = new ArrayList<>();
		expectedLanguages.add(LANGUAGE);
		assertEquals("Unexpected language", expectedLanguages, (List<String>) jsonObject.get(PmdCatalogJson.JSON_LANGUAGES));

		final List<String> expectedCategoryNames = new ArrayList<>();
		expectedCategoryNames.add(CATEGORY_NAME);
		assertEquals("Unexpected categories", expectedCategoryNames, jsonObject.get(PmdCatalogJson.JSON_CATEGORIES));

	}

	@Test
	public void testCatalogRuleNoDescription() {

		final String emptyDescription = "";

		// Setup mock
		final Element elementMock = getElementMock(Collections.singletonList(""));
		final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);

		// Execute
		final JSONObject jsonObject = catalogRule.toJson();

		// Validate
		assertEquals("Unexpected description", emptyDescription, jsonObject.get(PmdCatalogJson.JSON_DESCRIPTION));
	}

	@Test
	public void testCatalogRuleJsonWithDescription() {
		final String description = "Some description";

		// Setup mock
		final Element elementMock = getElementMock(Collections.singletonList(description));
		final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);

		// Execute
		final JSONObject jsonObject = catalogRule.toJson();

		// Validate
		assertEquals("Unexpected description", description, jsonObject.get(PmdCatalogJson.JSON_DESCRIPTION));
	}

	@Test
	public void testCatalogRuleJsonWithMultipleDescriptions_expectException() {
		final String description1 = "Some Description";
		final String description2 = "Some Other Description";

		// Setup mock
		final Element elementMock = getElementMock(Arrays.asList(description1, description2));
		thrown.expect(new MessagePassableExceptionMatcher(EventKey.ERROR_EXTERNAL_MULTIPLE_RULE_DESC,
			new String[]{CATEGORY.getPath() + "/" + NAME, "2"}
		));
		// Even initializing the object should be enough to trigger the expected exception.
		final PmdCatalogRule catalogRule = new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE);
	}

	private Element getElementMock(List<String> descriptions) {
		final Element elementMock = mock(Element.class);
		doReturn(NAME).when(elementMock).getAttribute(PmdCatalogRule.ATTR_NAME);
		doReturn(MESSAGE).when(elementMock).getAttribute(PmdCatalogRule.ATTR_MESSAGE);

		final NodeList nodeListMock = mock(NodeList.class);
		doReturn(descriptions.size()).when(nodeListMock).getLength();
		for (int i = 0; i < descriptions.size(); i++) {
			final Element descElementMock = mock(Element.class);
			doReturn(descriptions.get(i)).when(descElementMock).getTextContent();
			doReturn(descElementMock).when(nodeListMock).item(i);
		}
		doReturn(nodeListMock).when(elementMock).getElementsByTagName(PmdCatalogRule.ATTR_DESCRIPTION);

		return elementMock;
	}
}
