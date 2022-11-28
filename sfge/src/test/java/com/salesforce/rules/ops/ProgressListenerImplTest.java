package com.salesforce.rules.ops;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import com.salesforce.graph.ApexPath;
import com.salesforce.rules.Violation;
import com.salesforce.testutils.DummyVertex;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ProgressListenerImplTest {

    final ProgressListenerImpl progressListener;

    public ProgressListenerImplTest() {
        SfgeConfigTestProvider.set(
                new TestSfgeConfig() {
                    @Override
                    public int getProgressIncrements() {
                        return 3;
                    }
                });

        // Creating an instance directly to avoid conflicts with other tests
        progressListener = new ProgressListenerImpl();
    }

    @BeforeEach
    public void beforeEach() {
        progressListener.reset();
    }

    @AfterEach
    public void afterEach() {
        progressListener.reset();
        SfgeConfigTestProvider.remove();
    }

    @Test
    public void testCompiledAnotherFile() {
        progressListener.compiledAnotherFile();
        assertThat(progressListener.getFilesCompiled(), equalTo(1));
    }

    @Test
    public void testFinishedAnalyzingEntryPoint() {
        List<ApexPath> paths = Lists.newArrayList(new ApexPath(null), new ApexPath(null));
        Set<Violation> violations =
                Sets.newHashSet(
                        new Violation.InternalErrorViolation("details", new DummyVertex("label")));

        progressListener.finishedAnalyzingEntryPoint(paths, violations);
        assertThat(progressListener.getPathsDetected(), equalTo(2));
        assertThat(progressListener.getViolationsDetected(), equalTo(1));
        assertThat(progressListener.getEntryPointsAnalyzed(), equalTo(1));
        assertThat(progressListener.getLastPathCountReported(), equalTo(0));
    }

    @Test
    public void testFinishedAnalyzingEntryPoint_progressIncrement() {
        List<ApexPath> paths = Lists.newArrayList(new ApexPath(null), new ApexPath(null));
        Set<Violation> violations =
                Sets.newHashSet(
                        new Violation.InternalErrorViolation("details", new DummyVertex("label")));

        progressListener.finishedAnalyzingEntryPoint(paths, violations);
        assertThat(progressListener.getPathsDetected(), equalTo(2));
        assertThat(progressListener.getViolationsDetected(), equalTo(1));
        assertThat(progressListener.getEntryPointsAnalyzed(), equalTo(1));
        assertThat(progressListener.getLastPathCountReported(), equalTo(0));

        progressListener.finishedAnalyzingEntryPoint(paths, violations);
        assertThat(progressListener.getPathsDetected(), equalTo(4));
        assertThat(progressListener.getViolationsDetected(), equalTo(2));
        assertThat(progressListener.getEntryPointsAnalyzed(), equalTo(2));
        assertThat(progressListener.getLastPathCountReported(), equalTo(4));
    }

    @Test
    public void testStringify() {
        final List<String> items = Lists.newArrayList("one", "two");
        assertThat(progressListener.stringify(items), equalTo("one,two"));
    }

    @Test
    public void testStringifyEmpty() {
        final List<String> items = new ArrayList<>();
        assertThat(progressListener.stringify(items), equalTo(ProgressListenerImpl.NONE_FOUND));
    }
}
