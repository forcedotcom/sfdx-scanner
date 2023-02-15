package com.salesforce.graph.ops.expander;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.salesforce.TestUtil;
import com.salesforce.config.SfgeConfigTestProvider;
import com.salesforce.config.TestSfgeConfig;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.graph.ApexPath;
import com.salesforce.graph.ops.registry.PathExpansionLimitReachedException;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PathExpansionRegistryTest {

    private GraphTraversalSource g;

    @BeforeEach
    public void setup() {
        this.g = TestUtil.getGraph();
    }

    @AfterEach
    public void cleanUp() {
        SfgeConfigTestProvider.remove();
    }

    @Test
    public void testApexPathExpanderExceedsLimit() {
        final int limit = 10;
        initialize(limit);

        final int itemCountToAdd = 11;

        assertThrows(
                PathExpansionLimitReachedException.class,
                () -> createApexPathExpanders(itemCountToAdd, new PathExpansionRegistry()),
                String.format(UserFacingMessages.PATH_EXPANSION_LIMIT_REACHED, limit));
    }

    @Test
    public void testApexPathExpanderNoLimit() {
        final int limit = -1; // Negative 1 implies no upper limit
        initialize(limit);

        final int itemCountToAdd = 25000;

        PathExpansionRegistry registry = new PathExpansionRegistry();
        createApexPathExpanders(itemCountToAdd, registry);
        MatcherAssert.assertThat(
                registry.size(ApexPathExpander.class), Matchers.equalTo(itemCountToAdd));
    }

    @Test
    public void testApexPathExpanderWithinLimit() {
        final int limit = 10;
        initialize(limit);

        final int itemCountToAdd = 9;

        PathExpansionRegistry registry = new PathExpansionRegistry();
        createApexPathExpanders(itemCountToAdd, registry);
        MatcherAssert.assertThat(
                registry.size(ApexPathExpander.class), Matchers.equalTo(itemCountToAdd));
    }

    private void createApexPathExpanders(int itemCountToAdd, PathExpansionRegistry registry) {
        final ApexPathCollapser collapser = NoOpApexPathCollapser.getInstance();
        final ApexPath apexPath = Mockito.mock(ApexPath.class);
        final ApexPathExpanderConfig config = ApexPathExpanderConfig.Builder.get().build();

        for (int i = 0; i < itemCountToAdd; i++) {
            // Create a new ApexPathExpander. Internally it gets registered.
            new ApexPathExpander(g, collapser, apexPath, config, registry);
        }
    }

    private void initialize(int limitSize) {
        SfgeConfigTestProvider.set(
                new TestSfgeConfig() {
                    @Override
                    public int getPathExpansionLimit() {
                        return limitSize;
                    }
                });
    }
}
