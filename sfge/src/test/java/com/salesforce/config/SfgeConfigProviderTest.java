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
                        public boolean isWarningViolationDisabled() {
                            return !EnvUtil.DEFAULT_RULE_DISABLE_WARNING_VIOLATION;
                        }

                        @Override
                        public boolean shouldLogWarningsOnVerbose() {
                            return !EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE;
                        }

                        @Override
                        public int getProgressIncrements() {
                            return -1 * EnvUtil.DEFAULT_PROGRESS_INCREMENTS;
                        }

                        @Override
                        public int getStackDepthLimit() {
                            return (int) (0.1 * EnvUtil.DEFAULT_PROGRESS_INCREMENTS);
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
                    sfgeConfig.isWarningViolationDisabled(),
                    equalTo(!EnvUtil.DEFAULT_RULE_DISABLE_WARNING_VIOLATION));
            MatcherAssert.assertThat(
                    sfgeConfig.shouldLogWarningsOnVerbose(),
                    equalTo(!EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE));
            MatcherAssert.assertThat(
                    sfgeConfig.getProgressIncrements(),
                    equalTo(-1 * EnvUtil.getProgressIncrements()));
            MatcherAssert.assertThat(
                sfgeConfig.getStackDepthLimit(),
                equalTo(0.1 * EnvUtil.DEFAULT_STACK_DEPTH));
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
                sfgeConfig.isWarningViolationDisabled(),
                equalTo(EnvUtil.DEFAULT_RULE_DISABLE_WARNING_VIOLATION));
        MatcherAssert.assertThat(
                sfgeConfig.shouldLogWarningsOnVerbose(),
                equalTo(EnvUtil.DEFAULT_LOG_WARNINGS_ON_VERBOSE));
        MatcherAssert.assertThat(
                sfgeConfig.getProgressIncrements(), equalTo(EnvUtil.DEFAULT_PROGRESS_INCREMENTS));
        MatcherAssert.assertThat(
            sfgeConfig.getStackDepthLimit(), equalTo(EnvUtil.DEFAULT_STACK_DEPTH));
    }
}
