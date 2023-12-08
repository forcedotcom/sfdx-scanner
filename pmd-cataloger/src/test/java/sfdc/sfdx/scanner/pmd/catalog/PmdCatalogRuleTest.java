package sfdc.sfdx.scanner.pmd.catalog;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.MessagePassableException;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;


public class PmdCatalogRuleTest {

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
		assertEquals(NAME, jsonObject.get(PmdCatalogJson.JSON_NAME), "Unexpected name on JSON");

		assertEquals(MESSAGE, jsonObject.get(PmdCatalogJson.JSON_MESSAGE), "Unexpected message");

		final List<String> expectedLanguages = new ArrayList<>();
		expectedLanguages.add(LANGUAGE);
		assertEquals(expectedLanguages, (List<String>) jsonObject.get(PmdCatalogJson.JSON_LANGUAGES), "Unexpected language");

		final List<String> expectedCategoryNames = new ArrayList<>();
		expectedCategoryNames.add(CATEGORY_NAME);
		assertEquals(expectedCategoryNames, jsonObject.get(PmdCatalogJson.JSON_CATEGORIES), "Unexpected categories");

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
		assertEquals(emptyDescription, jsonObject.get(PmdCatalogJson.JSON_DESCRIPTION), "Unexpected description");
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
		assertEquals(description, jsonObject.get(PmdCatalogJson.JSON_DESCRIPTION), "Unexpected description");
	}

	@Test
	public void testCatalogRuleJsonWithMultipleDescriptions_expectException() {
		final String description1 = "Some Description";
		final String description2 = "Some Other Description";

		// Setup mock
		final Element elementMock = getElementMock(Arrays.asList(description1, description2));

        // Even initializing the object should be enough to trigger the expected exception.
        MessagePassableException ex = assertThrows(MessagePassableException.class, () -> new PmdCatalogRule(elementMock, CATEGORY, LANGUAGE));
		assertThat(ex.getEventKey(), is(EventKey.ERROR_EXTERNAL_MULTIPLE_RULE_DESC));
        assertThat(ex.getArgs(), is(new String[]{CATEGORY.getPath() + "/" + NAME, "2"}));
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
