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
                    String.format(UserFacingMessages.UNRECOGNIZED_ACTION, actionArg));
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

        public void parseArgs(String... args) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("CLI args received: " + Arrays.toString(args));
            }
            // Make sure we have the right number of arguments.
            if (args.length != ARG_COUNT) {
                throw new InvocationException(
                        String.format(
                                UserFacingMessages.INCORRECT_ARGUMENT_COUNT,
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

        private void identifyProjectDirs(String inputFile) {
            try {
                projectDirs.addAll(readFile(inputFile));
            } catch (IOException ex) {
                throw new InvocationException(
                        "Could not read source-list file " + inputFile + ": " + ex.getMessage(),
                        ex);
            }
        }

        private void identifyTargetFiles(String inputFile) {
            try {
                String targetJson = String.join("\n", readFile(inputFile));
                Gson gson = new Gson();
                targets.addAll(Arrays.asList(gson.fromJson(targetJson, RuleRunnerTarget[].class)));
            } catch (IOException ex) {
                throw new InvocationException(
                        "Could not read target-list file " + inputFile + ": " + ex.getMessage(),
                        ex);
            }
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
