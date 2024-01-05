package sfdc.sfdx.scanner.pmd;

import java.util.*;
import java.util.stream.Collectors;

import com.salesforce.messaging.EventKey;
import com.salesforce.messaging.MessagePassableException;
import com.salesforce.messaging.CliMessager;
import sfdc.sfdx.scanner.Constants;

public class Main {

	static String DIVIDER = "=";
	static String COMMA = ",";

	static final String NO_ARGUMENTS_FOUND = "No arguments found";
	static final String EXPECTED_DIVIDER = "Expected one " + DIVIDER + " in argument: %s";
	static final String MISSING_PARTS = "Missing language and/or paths in argument: %s";
	static final String NO_PATH_PROVIDED = "At least one path needs to be provided for language %s in argument: %s";

	Dependencies dependencies;

	public Main() {
		this(new Dependencies());
	}

	public Main(Dependencies dependencies) {
		this.dependencies = dependencies;
	}

	/**
	 * Main entry to PMD Rule Catalog builder
	 *
	 * @param args should contain language separated by '=' from their comma-separated path mapping list. For example, here are some accepted arg values :
	 *             "apex=/some/lib/withjars,/more/path"
	 *             "javascript=/another/path,/yet/another/path/with/javascript/rules"
	 */
	public static void main(String[] args) {

		final Main main = new Main(new Dependencies());
		final int exitCode = main.mainInternal(args) ? 0 : 1;
		System.exit(exitCode);
	}

	boolean mainInternal(String[] args) {
		boolean exitGracefully = true;
		try {
			final Map<String, List<String>> rulePathEntries = parseArguments(args);
			catalogRules(rulePathEntries);

		} catch (MessagePassableException se) {
			// Add all SfdxScannerExceptions as messages
			CliMessager.getInstance().addMessage(se);
			exitGracefully = false;
		} catch (Throwable throwable) {
			// Catch and handle any exceptions that may have slipped through
			final MessagePassableException exception = new MessagePassableException(EventKey.ERROR_INTERNAL_UNEXPECTED, throwable, throwable.getMessage());
			CliMessager.getInstance().addMessage(exception);
			exitGracefully = false;
		} finally {
			// Print all the messages we have collected in a parsable format
			System.out.println(CliMessager.getInstance().getAllMessagesWithFormatting());
		}

		return exitGracefully;
	}

	private void catalogRules(Map<String, List<String>> rulePathEntries) {
		PmdRuleCataloger prc = dependencies.getPmdRuleCataloger(rulePathEntries);
		prc.catalogRules();
	}

	Map<String, List<String>> parseArguments(String[] args) {
		if (args == null || args.length < 1) {
			throw new MessagePassableException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, NO_ARGUMENTS_FOUND);
		}

		final Map<String, List<String>> rulePathEntries = new HashMap<>();

		for (String arg : args) {
			parseArg(rulePathEntries, arg);
		}
		return rulePathEntries;
	}

	private void parseArg(Map<String, List<String>> languagePathEntries, String arg) {
		final String[] splitArg = arg.split(DIVIDER);

		// DIVIDER should split arg in language and path list. No less, no more
		if (splitArg.length != 2) {
			throw new MessagePassableException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, String.format(EXPECTED_DIVIDER, arg));
		}
		final String language = splitArg[0].trim();
		final String paths = splitArg[1];

		if ("".equals(language.trim()) || "".equals((paths.trim()))) {
			throw new MessagePassableException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, String.format(MISSING_PARTS, arg));
		}

		final String[] pathArray = paths.split(COMMA);

		if (pathArray.length < 1) {
			throw new MessagePassableException(EventKey.ERROR_INTERNAL_MAIN_INVALID_ARGUMENT, String.format(NO_PATH_PROVIDED, language, arg));
		}

		// Stream path array to filter out empty path
		List<String> pathList = Arrays.stream(pathArray).filter(path -> !"".equals(path.trim())).collect(Collectors.toList());

		languagePathEntries.put(language, pathList);
	}

	static class Dependencies {

		PmdRuleCataloger getPmdRuleCataloger(Map<String, List<String>> rulePathEntries) {
            String catalogHome = System.getProperty(Constants.SystemProperty.CATALOG_HOME);
            String catalogName = System.getProperty(Constants.SystemProperty.CATALOG_NAME);
            String catalogedEngineName = System.getProperty(Constants.SystemProperty.CATALOGED_ENGINE_NAME);
			return new PmdRuleCataloger(rulePathEntries, catalogHome, catalogName, catalogedEngineName);
		}
	}
}
