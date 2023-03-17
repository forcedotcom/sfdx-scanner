package com.salesforce.cli;

import com.google.common.annotations.VisibleForTesting;
import com.google.gson.Gson;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.rules.AbstractRule;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import com.salesforce.rules.RuleUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CliArgParser {
    public enum CLI_ACTION {
        EXECUTE,
        CATALOG
    }

    private static final Logger LOGGER = LogManager.getLogger(CliArgParser.class);

    public CliArgParser() {}

    public CLI_ACTION getCliAction(String... args) {
        final String actionArg = args[0];

        if ("execute".equalsIgnoreCase(actionArg)) {
            return CLI_ACTION.EXECUTE;
        } else if ("catalog".equalsIgnoreCase(actionArg)) {
            return CLI_ACTION.CATALOG;
        } else {
            throw new InvocationException(
                    String.format(UserFacingMessages.InvocationErrors.UNRECOGNIZED_ACTION, actionArg));
        }
    }

    public static class CatalogArgParser {
        private static final int ARG_COUNT = 2;
        // NOTE: This value must match the one for the RuleType enum declared in Constants.ts.
        private static final String PATHLESS = "pathless";
        // NOTE: This value must match the one for the RuleType enum declared in Constants.ts.
        private static final String DFA = "dfa";

        private List<AbstractRule> selectedRules;

        public CatalogArgParser() {
            selectedRules = new ArrayList<>();
        }

        /**
         * See the documentation of {@link com.salesforce.Main} for information about the
         * expectations for args.
         */
        public void parseArgs(String... args) throws RuleUtil.RuleNotFoundException {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("CLI args received: " + Arrays.toString(args));
            }
            // Make sure we have the right number of arguments.
            if (args.length != ARG_COUNT) {
                throw new InvocationException(
                        String.format(
                                "Wrong number of arguments. Expected %d; received %d",
                                ARG_COUNT, args.length));
            }
            switch (args[1]) {
                case PATHLESS:
                    selectedRules = RuleUtil.getEnabledStaticRules();
                    break;
                case DFA:
                    selectedRules = RuleUtil.getEnabledPathBasedRules();
                    break;
                default:
                    selectedRules = RuleUtil.getEnabledRules();
                    break;
            }
        }

        public List<AbstractRule> getSelectedRules() {
            return selectedRules;
        }
    }

    public static class ExecuteArgParser {
        private static int ARG_COUNT = 2;

        private final List<String> projectDirs;
        private final List<RuleRunnerTarget> targets;
        private final List<AbstractRule> selectedRules;

        private final Dependencies dependencies;

        public ExecuteArgParser() {
            this(new Dependencies());
        }

        @VisibleForTesting
        public ExecuteArgParser(Dependencies dependencies) {
            projectDirs = new ArrayList<>();
            targets = new ArrayList<>();
            selectedRules = new ArrayList<>();
            this.dependencies = dependencies;
        }

        /**
         * See the documentation of {@link com.salesforce.Main} for information about the
         * expectations for args.
         */
        public void parseArgs(String... args) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("CLI args received: " + Arrays.toString(args));
            }
            // Make sure we have the right number of arguments.
            if (args.length != ARG_COUNT) {
                throw new InvocationException(
                        String.format(
                                UserFacingMessages.InvocationErrors.INCORRECT_ARGUMENT_COUNT,
                                ARG_COUNT,
                                args.length));
            }
            ExecuteInput input = readInputFile(args[1]);
            targets.addAll(input.targets);
            projectDirs.addAll(input.projectDirs);
            identifyRules(input.rulesToRun);
        }

        public List<String> getProjectDirs() {
            return projectDirs;
        }

        public List<RuleRunnerTarget> getTargets() {
            return targets;
        }

        public List<AbstractRule> getSelectedRules() {
            return selectedRules;
        }

        private ExecuteInput readInputFile(String fileName) {
            try {
                String inputJson = String.join("\n", readFile(fileName));
                Gson gson = new Gson();
                return gson.fromJson(inputJson, ExecuteInput.class);
            } catch (IOException ex) {
                throw new InvocationException(
                        "Could not read input file " + fileName + ": " + ex.getMessage(), ex);
            }
        }

        private List<String> readFile(String fileName) throws IOException {
            final List<String> allLines = dependencies.getAllLines(fileName);
            final List<String> lines =
                    allLines.stream()
                            .filter(line -> StringUtils.isNotBlank(line))
                            .collect(Collectors.toList());

            return lines;
        }

        private void identifyRules(List<String> rulesToRun) {
            try {
                if (rulesToRun.isEmpty()) {
                    selectedRules.addAll(RuleUtil.getEnabledRules());
                } else {
                    for (String ruleName : rulesToRun) {
                        AbstractRule rule = RuleUtil.getRule(ruleName);
                        selectedRules.add(rule);
                    }
                }
            } catch (RuleUtil.RuleNotFoundException ex) {
                throw new InvocationException(ex.getMessage(), ex);
            }
        }
    }

    public static class InvocationException extends SfgeRuntimeException {
        InvocationException(String msg) {
            super(msg);
        }

        InvocationException(String msg, Throwable cause) {
            super(msg, cause);
        }
    }

    public static class ExecuteInput {
        private List<String> rulesToRun;
        private List<String> projectDirs;
        private List<RuleRunnerTarget> targets;
    }

    @VisibleForTesting
    public static class Dependencies {

        @VisibleForTesting
        public List<String> getAllLines(String fileName) throws IOException {
            return Files.readAllLines(Paths.get(fileName));
        }
    }
}
