package com.salesforce.rules;

import com.salesforce.PackageConstants;
import com.salesforce.exception.SfgeException;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.graph.ops.MethodUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.rules.AbstractRuleRunner.RuleRunnerTarget;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.reflections.Reflections;

public final class RuleUtil {
    private static final Logger LOGGER = LogManager.getLogger(RuleUtil.class);

    /**
     * Load all path entry points in the graph.
     *
     * @param g
     * @return
     */
    public static List<MethodVertex> getPathEntryPoints(GraphTraversalSource g) {
        return getPathEntryPoints(g, new ArrayList<>());
    }

    /**
     * Load all path entry points specified by the target objects. An empty list implicitly includes
     * all files.
     */
    public static List<MethodVertex> getPathEntryPoints(
            GraphTraversalSource g, List<RuleRunnerTarget> targets) {
        // Sort the list of targets into full-file targets and method-level targets.
        List<String> fileLevelTargets =
                targets.stream()
                        .filter(t -> t.getTargetMethods().isEmpty())
                        .map(RuleRunnerTarget::getTargetFile)
                        .collect(Collectors.toList());
        List<RuleRunnerTarget> methodLevelTargets =
                targets.stream()
                        .filter(t -> !t.getTargetMethods().isEmpty())
                        .collect(Collectors.toList());

        // Internally, we'll use a Set to preserve uniqueness.
        Set<MethodVertex> methods = new HashSet<>();

        // If there are any explicitly targeted files, we must process them. If there are no
        // explicit targets of any kind,
        // then all files are implicitly targeted.
        if (!fileLevelTargets.isEmpty() || targets.isEmpty()) {
            // Use the file-level targets to get aura-enabled methods...
            methods.addAll(MethodUtil.getAuraEnabledMethods(g, fileLevelTargets));
            // ...and NamespaceAccessible methods...
            methods.addAll(MethodUtil.getNamespaceAccessibleMethods(g, fileLevelTargets));
            // ...and RemoteAction methods...
            methods.addAll(MethodUtil.getRemoteActionMethods(g, fileLevelTargets));
            // ...and PageReference methods...
            methods.addAll(MethodUtil.getPageReferenceMethods(g, fileLevelTargets));
            // ...and global-exposed methods...
            methods.addAll(MethodUtil.getGlobalMethods(g, fileLevelTargets));
            // ...and implementations of Messaging.InboundEmailHandler#handleInboundEmail...
            methods.addAll(MethodUtil.getInboundEmailHandlerMethods(g, fileLevelTargets));
            // ...and exposed methods on VF controllers.
            methods.addAll(MethodUtil.getExposedControllerMethods(g, fileLevelTargets));
        }

        // Also, if there are any specifically targeted methods, they should be included.
        if (!methodLevelTargets.isEmpty()) {
            methods.addAll(MethodUtil.getTargetedMethods(g, methodLevelTargets));
        }
        // Turn the Set into a List so we can return it.
        return new ArrayList<>(methods);
    }

    public static List<AbstractRule> getAllRules() throws RuleNotFoundException {
        // Get a set of every class in the Rules package that extends AbstractRule.
        Reflections reflections = new Reflections(PackageConstants.RULES_PACKAGE);
        Set<Class<? extends AbstractRule>> ruleTypes =
                reflections.getSubTypesOf(AbstractRule.class);

        // Get an instance of each rule.
        List<AbstractRule> rules = new ArrayList<>();
        for (Class<? extends AbstractRule> ruleType : ruleTypes) {
            // Skip abstract classes.
            if (!Modifier.isAbstract(ruleType.getModifiers())) {
                rules.add(getRuleInner(ruleType.getName()));
            }
        }
        return rules;
    }

    public static AbstractRule getRule(String ruleName) throws RuleNotFoundException {
        return getRuleInner(PackageConstants.RULES_PACKAGE + "." + ruleName);
    }

    @SuppressWarnings(
            "PMD.PreserveStackTrace") // The logic is intentionally not preserving the full
    // stacktrace
    private static AbstractRule getRuleInner(String ruleName) throws RuleNotFoundException {
        try {
            Class<?> clazz = Class.forName(ruleName);
            // Make sure that all rules are correctly singletons. I.e., they have no public
            // constructors and they expose
            // a getInstance() method.
            Constructor[] constructors = clazz.getConstructors();

            if (constructors.length > 0) {
                // This error should only ever happen during development, never in production.
                throw MisconfiguredRuleException.publicConstructorVariant(ruleName);
            }
            Method getInstanceMethod = clazz.getMethod("getInstance");
            int modifiers = getInstanceMethod.getModifiers();
            if (!Modifier.isStatic(modifiers)
                    || !Modifier.isPublic(modifiers)
                    || getInstanceMethod.getReturnType() != clazz) {
                // This should only ever happen during development, never in production.
                throw MisconfiguredRuleException.missingGetInstanceVariant(ruleName);
            }
            return (AbstractRule) getInstanceMethod.invoke(null);
        } catch (ClassNotFoundException | IllegalAccessException | InvocationTargetException ex) {
            // A ClassNotFoundException means that no rule by that name exists. The other two mean
            // the rule couldn't be
            // instantiated.
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Unable to instantiate rule. ClassName=" + ruleName, ex);
            }
            throw new RuleNotFoundException("Unable to instantiate rule " + ruleName);
        } catch (NoSuchMethodException ex) {
            // A NoSuchMethodException means that there's no `getInstance()` method. This exception
            // should never happen
            // in production, only in development.
            if (LOGGER.isErrorEnabled()) {
                LOGGER.error("Rule " + ruleName + " is not a valid singleton", ex);
            }
            throw MisconfiguredRuleException.missingGetInstanceVariant(ruleName);
        }
    }

    private RuleUtil() {}

    public static final class RuleNotFoundException extends SfgeException {
        public RuleNotFoundException(String msg) {
            super(msg);
        }
    }

    /**
     * This exception indicates that we attempted to retrieve a rule only to discover that it's
     * configured incorrectly. As such, this exception should only be thrown in development, and
     * never in production.
     */
    public static final class MisconfiguredRuleException extends SfgeRuntimeException {
        MisconfiguredRuleException(String msg) {
            super(msg);
        }

        private static MisconfiguredRuleException publicConstructorVariant(String ruleName) {
            return new MisconfiguredRuleException(
                    "Rule "
                            + ruleName
                            + " is not a proper singleton, as it has public constructors");
        }

        private static MisconfiguredRuleException missingGetInstanceVariant(String ruleName) {
            return new MisconfiguredRuleException(
                    "Rule "
                            + ruleName
                            + " is not a proper singleton, as it lacks a `public static AbstractRule getInstance()` method");
        }
    }
}
