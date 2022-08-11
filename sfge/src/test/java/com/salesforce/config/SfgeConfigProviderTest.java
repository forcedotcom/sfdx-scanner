package com.salesforce.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;

public class SfgeConfigProviderTest {
    @Test
    public void testDefaultImplementation() {
        final SfgeConfig sfgeConfig = SfgeConfigProvider.get();
        assertDefaultImplementation(sfgeConfig);
    }

    @Test
    public void testOverrideImplementation() {
        try {
            // Create a test implementation that converts the defaults to negatives and inverted
            // booleans
            SfgeConfigTestProvider.set(
                    new SfgeConfig() {
                        @Override
                        public int getRuleThreadCount() {
                            return -1 * SfgeConfigImpl.getInstance().getRuleThreadCount();
                        }

                        @Override
                        public long getRuleThreadTimeout() {
                            return -1 * SfgeConfigImpl.getInstance().getRuleThreadTimeout();
                        }

                        @Override
                        public boolean isWarningViolationEnabled() {
                            return !EnvUtil.DEFAULT_RULE_ENABLE_WARNING_VIOLATION;
                        }

                        @Override
                        public boolean shouldIgnoreParseErrors() {
                            return !EnvUtil.DEFAULT_IGNORE_PARSE_ERRORS;
                        }

                        @Override
                        public boolean shouldLogWarningsOnVerbose() {
                            return !EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE;
                        }

                        @Override
                        public int getProgressIncrementsOnVerbose() {
                            return -1 * EnvUtil.DEFAULT_PROGRESS_INCREMENTS_ON_VERBOSE;
                        }
                    });

            final SfgeConfig sfgeConfig = SfgeConfigProvider.get();
            MatcherAssert.assertThat(sfgeConfig, not(nullValue()));
            MatcherAssert.assertThat(
                    sfgeConfig.getRuleThreadCount(),
                    equalTo(-1 * EnvUtil.DEFAULT_RULE_THREAD_COUNT));
            MatcherAssert.assertThat(
                    sfgeConfig.getRuleThreadTimeout(),
                    equalTo(-1 * EnvUtil.DEFAULT_RULE_THREAD_TIMEOUT));
            MatcherAssert.assertThat(
                    sfgeConfig.isWarningViolationEnabled(),
                    equalTo(!EnvUtil.DEFAULT_RULE_ENABLE_WARNING_VIOLATION));
            MatcherAssert.assertThat(
                    sfgeConfig.shouldIgnoreParseErrors(),
                    equalTo(!EnvUtil.DEFAULT_IGNORE_PARSE_ERRORS));
            MatcherAssert.assertThat(
                    sfgeConfig.shouldLogWarningsOnVerbose(),
                    equalTo(!EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE));
            MatcherAssert.assertThat(
                    sfgeConfig.getProgressIncrementsOnVerbose(),
                    equalTo(-1 * EnvUtil.getProgressIncrementsOnVerbose()));
        } finally {
            SfgeConfigTestProvider.remove();
        }

        // Make sure that the values have been restored to the defaults
        final SfgeConfig sfgeConfig = SfgeConfigProvider.get();
        assertDefaultImplementation(sfgeConfig);
    }

    /**
     * Asserts that <code>sfgeConfig</code> is the default implementation with the default values.
     */
    private void assertDefaultImplementation(SfgeConfig sfgeConfig) {
        MatcherAssert.assertThat(sfgeConfig, instanceOf(SfgeConfigImpl.class));
        MatcherAssert.assertThat(
                sfgeConfig.getRuleThreadCount(), equalTo(EnvUtil.DEFAULT_RULE_THREAD_COUNT));
        MatcherAssert.assertThat(
                sfgeConfig.getRuleThreadTimeout(), equalTo(EnvUtil.DEFAULT_RULE_THREAD_TIMEOUT));
        MatcherAssert.assertThat(
                sfgeConfig.isWarningViolationEnabled(),
                equalTo(EnvUtil.DEFAULT_RULE_ENABLE_WARNING_VIOLATION));
        MatcherAssert.assertThat(
                sfgeConfig.shouldIgnoreParseErrors(), equalTo(EnvUtil.DEFAULT_IGNORE_PARSE_ERRORS));
        MatcherAssert.assertThat(
                sfgeConfig.shouldLogWarningsOnVerbose(),
                equalTo(EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE));
        MatcherAssert.assertThat(
                sfgeConfig.getProgressIncrementsOnVerbose(),
                equalTo(EnvUtil.DEFAULT_PROGRESS_INCREMENTS_ON_VERBOSE));
    }
}
