package com.salesforce.rules;

import com.salesforce.PackageConstants;
import com.salesforce.exception.SfgeException;
import com.salesforce.exception.SfgeRuntimeException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.reflections.Reflections;

public final class RuleUtil {
    private static final Logger LOGGER = LogManager.getLogger(RuleUtil.class);

    public static List<AbstractRule> getEnabledRules() throws RuleNotFoundException {
        final List<AbstractRule> allRules = getAllRules();
        return allRules.stream().filter(rule -> rule.isEnabled()).collect(Collectors.toList());
    }

    static List<AbstractRule> getAllRules() throws RuleNotFoundException {
        // Get a set of every class in the Rules package that extends AbstractRule.
        Reflections reflections = new Reflections(PackageConstants.RULES_PACKAGE);
        Set<Class<? extends AbstractRule>> ruleTypes =
                reflections.getSubTypesOf(AbstractRule.class);

        // Get an instance of each rule.
        List<AbstractRule> rules = new ArrayList<>();
        for (Class<? extends AbstractRule> ruleType : ruleTypes) {
            // Skip abstract classes.
            if (!Modifier.isAbstract(ruleType.getModifiers())) {
                final AbstractRule rule = getRuleInner(ruleType.getName());
                rules.add(rule);
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
