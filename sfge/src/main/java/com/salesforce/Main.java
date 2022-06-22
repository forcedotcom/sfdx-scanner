package com.salesforce;

import com.salesforce.cli.CliArgParser;
import com.salesforce.cli.OutputFormatter;
import com.salesforce.exception.SfgeException;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.graph.ops.GraphUtil;
import com.salesforce.messaging.CliMessager;
import com.salesforce.metainfo.MetaInfoCollector;
import com.salesforce.metainfo.MetaInfoCollectorProvider;
import com.salesforce.rules.AbstractRule;
import com.salesforce.rules.RuleRunner;
import com.salesforce.rules.RuleUtil;
import com.salesforce.rules.Violation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * The main class, invoked by sfdx-scanner. `catalog` flow lists all of the available rules in a
 * standardized format.
 *
 * <p>The `execute` flow accepts as a parameter the name of a file whose contents are a JSON with
 * the following structure:
 *
 * <ol>
 *   <li>rulesToRun: An array of rule names.
 *   <li>projectDirs: An array of directories from which the graph should be built.
 *   <li>targets: An array of objects with a `targetFile` property indicating the file to be
 *       analyzed and a `targetMethods` property indicating individual methods.
 * </ol>
 *
 * <p>Exit codes:
 *
 * <ul>
 *   <li>Negative numbers indicate an internal error.
 *   <li>0 indicates a successful run with * no violations.
 *   <li>Positive numbers indicate a successful run with exit-code-many violations.
 * </ul>
 *
 * <p>Usage: mvn exec:java -Dexec.mainClass=com.salesforce.Main -Dexec.args="catalog" OR mvn
 * exec:java -Dexec.mainClass=com.salesforce.Main -Dexec.args="execute [path to file listing
 * targets] [path to file listing sources] [comma-separated rules]"
 */
@SuppressWarnings(
        "PMD.SystemPrintln") // Since println is currently used to communicate to outer layer
public class Main {
    private static final int EXIT_NO_VIOLATIONS = 0;
    private static final int EXIT_WITH_VIOLATIONS = 4;
    private static final int INTERNAL_ERROR = 1;
    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    public static final String ERROR_PREFIX = "SfgeErrorStart\n";

    public static void main(String[] args) {
        Main m = new Main();
        int status = m.process(args);
        System.exit(status);
    }

    private int process(String... args) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Invoked with args=" + Arrays.asList(args));
        }
        if (args.length == 0) {
            // No args means we can't do anything productive.
            System.err.println("Engine requires at least one argument");
            return INTERNAL_ERROR;
        }

        CliArgParser parser = new CliArgParser();
        CliArgParser.CLI_ACTION action = parser.getCliAction(args);

        switch (action) {
            case CATALOG:
                return catalog();
            case EXECUTE:
                return execute(args);
            default:
                System.err.println("Unrecognized action " + action);
                return INTERNAL_ERROR;
        }
    }

    private int catalog() {
        LOGGER.info("Invoked CATALOG flow");
        List<AbstractRule> rules;
        try {
            rules = RuleUtil.getAllRules();
        } catch (SfgeException | SfgeRuntimeException ex) {
            System.err.println(ex.getMessage());
            return INTERNAL_ERROR;
        }
        OutputFormatter formatter = new OutputFormatter();
        System.out.println(formatter.formatRuleJsons(rules));
        return EXIT_NO_VIOLATIONS;
    }

    private int execute(String... args) {
        LOGGER.info("Invoked EXECUTE flow");
        // Parse the arguments with our delegate class.
        CliArgParser.ExecuteArgParser eap = new CliArgParser.ExecuteArgParser();
        try {
            eap.parseArgs(args);
        } catch (SfgeRuntimeException ex) {
            LOGGER.error("Error while parsing input arguments", ex);
            System.err.println(formatError(ex));
            return INTERNAL_ERROR;
        }

        // Collect additional information from other files in project
        try {
            collectMetaInfo(eap);
        } catch (MetaInfoCollector.MetaInfoLoadException ex) {
            LOGGER.error("Error while collecting meta info", ex);
            System.err.println(formatError(ex));
            return INTERNAL_ERROR;
        }

        // Initialize and clean our graph.
        GraphTraversalSource g = GraphUtil.getGraph();

        // Compile all of the Apex into ASTs.
        try {
            GraphUtil.loadSourceFolders(g, eap.getProjectDirs());
        } catch (GraphUtil.GraphLoadException ex) {
            LOGGER.error("Error while loading graph", ex);
            System.err.println(formatError(ex));
            return INTERNAL_ERROR;
        }

        // Run all of the rules.
        List<Violation> allViolations;
        try {
            allViolations = new RuleRunner(g).runRules(eap.getSelectedRules(), eap.getTargets());
        } catch (SfgeRuntimeException ex) {
            LOGGER.error("Error while running rules", ex);
            System.err.println(formatError(ex));
            return INTERNAL_ERROR;
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(
                    String.format(
                            "This many files: %d. This many rules: %d. This many violations: %d",
                            eap.getTargets().size(),
                            eap.getSelectedRules().size(),
                            allViolations.size()));
        }
        OutputFormatter formatter = new OutputFormatter();
        System.out.println(CliMessager.getInstance().getAllMessagesWithFormatting());
        System.out.println(formatter.formatViolationJsons(allViolations));
        return allViolations.isEmpty() ? EXIT_NO_VIOLATIONS : EXIT_WITH_VIOLATIONS;
    }

    private void collectMetaInfo(CliArgParser.ExecuteArgParser eap) {
        final Collection<? extends MetaInfoCollector> allCollectors =
                MetaInfoCollectorProvider.getAllCollectors();

        for (MetaInfoCollector collector : allCollectors) {
            collector.loadProjectFiles(eap.getProjectDirs());
        }
    }

    private String formatError(Throwable error) {
        return ERROR_PREFIX + error.getMessage();
    }
}
