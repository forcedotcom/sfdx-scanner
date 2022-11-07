package com.salesforce.graph.build;

import com.salesforce.apex.jorje.AstNodeWrapper;
import com.salesforce.graph.MetadataInfoProvider;
import com.salesforce.graph.cache.VertexCacheProvider;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

public final class Util {
    private static final Logger LOGGER = LogManager.getLogger(Util.class);

    public static void buildGraph(Config config) {
        // The cache needs to know about the graph before any of the  builders run
        VertexCacheProvider.get().initialize(config.g);
        for (GraphBuilder graphBuilder :
                new GraphBuilder[] {
                    new ApexStandardLibraryVertexBuilder(config.g),
                    new CustomerApexVertexBuilder(config.g, config.customerCompilations),
                    new InheritanceInformationBuilder(config.g)
                }) {
            if (config.ignoreBuilders.contains(graphBuilder.getClass())) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Ignoring. builder=" + graphBuilder);
                }
                continue;
            }
            StopWatch stopWatch = StopWatch.createStarted();
            String name = graphBuilder.getClass().getSimpleName();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Starting " + name);
            }
            graphBuilder.build();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(
                        "Finished "
                                + name
                                + ", time(seconds)="
                                + stopWatch.getTime(TimeUnit.SECONDS));
            }
        }

        // Initialize the MetadataInfoProvider with data from the graph
        MetadataInfoProvider.get().initialize(config.g);
    }

    /** Configuration object used to avoid method overload proliferation */
    public static final class Config {
        private final GraphTraversalSource g;
        private final List<CompilationDescriptor> customerCompilations;
        /**
         * Set of GraphBuilder classes to ignore. This should only be used during tests as a speed
         * optimization
         */
        private final Set<Class<? extends GraphBuilder>> ignoreBuilders;

        private Config(Builder builder) {
            this.g = builder.g;
            this.customerCompilations = builder.compilations;
            this.ignoreBuilders = builder.ignoreBuilders;
        }

        public static final class Builder {
            private final GraphTraversalSource g;
            private final List<CompilationDescriptor> compilations;
            private final Set<Class<? extends GraphBuilder>> ignoreBuilders;

            private Builder(GraphTraversalSource g, List<CompilationDescriptor> compilations) {
                this.g = g;
                this.compilations = compilations;
                this.ignoreBuilders = new HashSet<>();
            }

            /**
             * @param compilations customer supplied Apex that has been compiled
             */
            public static Builder get(
                    GraphTraversalSource g, List<CompilationDescriptor> compilations) {
                return new Builder(g, compilations);
            }

            /**
             * @param compilation customer supplied Apex that has been compiled
             */
            public static Builder get(GraphTraversalSource g, CompilationDescriptor compilation) {
                return new Builder(g, Collections.singletonList(compilation));
            }

            /**
             * Additive GraphBuilder classes to ignore. This should only be used during tests as a
             * speed optimization
             */
            public Builder skipBuilders(Class<? extends GraphBuilder> ignoreBuilder) {
                ignoreBuilders.add(ignoreBuilder);
                return this;
            }

            public Config build() {
                return new Config(this);
            }
        }
    }

    public static final class CompilationDescriptor {
        private final String fileName;
        private final AstNodeWrapper<?> astNodeWrapper;

        public CompilationDescriptor(String fileName, AstNodeWrapper<?> astNodeWrapper) {
            this.fileName = fileName;
            this.astNodeWrapper = astNodeWrapper;
        }

        public String getFileName() {
            return this.fileName;
        }

        public AstNodeWrapper<?> getCompilation() {
            return this.astNodeWrapper;
        }
    }

    private Util() {}
}
