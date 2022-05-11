package com.salesforce.graph;

import com.salesforce.apex.ApexEnum;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.ProgrammingException;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.vertex.MethodCallExpressionVertex;
import com.salesforce.graph.vertex.ReferenceExpressionVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserEnumVertex;
import com.salesforce.metainfo.MetaInfoCollectorProvider;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;

/**
 * This class uses heuristics to identify the types of metadata in the graph. This will eventually
 * be replaced by parsing the metadata files and importing it into the graph. TODO: Replace with
 * Metdata parsing
 */
abstract class AbstractMetadataInfoImpl implements MetadataInfo {
    private static final Logger LOGGER = LogManager.getLogger(AbstractMetadataInfoImpl.class);

    private static final String GET_ALL = "getAll";
    private static final String GET_INSTANCE = "getInstance";
    private static final String GET_ORG_DEFAULTS = "getOrgDefaults";
    private static final String GET_VALUES = "getValues";

    /**
     * Store the information on the current thread. This supports running with multiple in memory
     * graphs at the same time.
     */
    private final TreeSet<String> customSettings;

    /** Map of enumName->enumValues */
    private final TreeMap<String, ApexEnum> enums;

    /** Make sure {@link #initialized} is only called once */
    private boolean initialized;

    protected AbstractMetadataInfoImpl() {
        this.customSettings = CollectionUtil.newTreeSet();
        this.enums = CollectionUtil.newTreeMap();
    }

    @Override
    public synchronized void initialize(GraphTraversalSource g) {
        if (initialized) {
            throw new ProgrammingException("Already initialized");
        }
        loadKnownCustomSettings();
        // If we had not collected custom settings in advance, or if we had run in problems,
        // we can augment the information with custom settings derived from code.
        findCustomSettings(g);
        findEnums(g);
        initialized = true;
    }

    @Override
    public boolean isCustomSetting(String name) {
        return customSettings.contains(name);
    }

    @Override
    public Optional<ApexEnum> getEnum(String name) {
        final String canonicalName = ApexStandardLibraryUtil.getCanonicalName(name);
        return Optional.ofNullable(enums.get(canonicalName));
    }

    /** Loads custom settings information collected from other project files. */
    private void loadKnownCustomSettings() {
        final TreeSet<String> knownCustomSettings =
                MetaInfoCollectorProvider.getCustomSettingsInfoCollector().getMetaInfoCollected();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                    "Loading {} as known custom settings. Thread: {}",
                    knownCustomSettings,
                    Thread.currentThread());
        }
        customSettings.addAll(knownCustomSettings);
    }

    /**
     * Custom settings can be identified by calls to SomeObject__c.getInstance(),
     * SomeObject__c.getOrgDefaults(), or SomeObject__c.getValues();
     * https://developer.salesforce.com/docs/atlas.en-us.apexref.meta/apexref/apex_methods_system_custom_settings.htm
     */
    private void findCustomSettings(GraphTraversalSource g) {
        List<ReferenceExpressionVertex> referenceExpressions =
                SFVertexFactory.loadVertices(
                        g,
                        g.V().where(
                                        CaseSafePropertyUtil.H.hasWithin(
                                                ASTConstants.NodeType.METHOD_CALL_EXPRESSION,
                                                Schema.METHOD_NAME,
                                                GET_INSTANCE,
                                                GET_ORG_DEFAULTS,
                                                GET_VALUES,
                                                GET_ALL))
                                .out(Schema.CHILD)
                                .where(
                                        CaseSafePropertyUtil.H.has(
                                                ASTConstants.NodeType.REFERENCE_EXPRESSION,
                                                Schema.REFERENCE_TYPE,
                                                ASTConstants.ReferenceType.METHOD))
                                .where(
                                        CaseSafePropertyUtil.H.hasEndingWith(
                                                ASTConstants.NodeType.REFERENCE_EXPRESSION,
                                                Schema.NAME,
                                                ASTConstants.TypeSuffix.SUFFIX_CUSTOM_OBJECT)));

        for (ReferenceExpressionVertex referenceExpression : referenceExpressions) {
            if (referenceExpression.getNames().size() == 1) {
                // TODO: Efficiency. I could not get union to work, it would find an odd number of
                // vertices;
                MethodCallExpressionVertex methodCallExpression = referenceExpression.getParent();
                // These method calls take 0 or 1 parameters
                if (GET_INSTANCE.equalsIgnoreCase(methodCallExpression.getMethodName())
                        || GET_VALUES.equalsIgnoreCase(methodCallExpression.getMethodName())) {
                    if (methodCallExpression.getParameters().size() <= 1) {
                        customSettings.add(referenceExpression.getName());
                    }
                } else if (GET_ORG_DEFAULTS.equalsIgnoreCase(
                        methodCallExpression.getMethodName())) {
                    if (methodCallExpression.getParameters().size() == 0) {
                        customSettings.add(referenceExpression.getName());
                    }
                } else if (GET_ALL.equalsIgnoreCase(methodCallExpression.getMethodName())) {
                    if (methodCallExpression.getParameters().size() == 0) {
                        customSettings.add(referenceExpression.getName());
                    }
                }
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Ignoring method call. vertex=" + referenceExpression);
            }
        }
    }

    private void findEnums(GraphTraversalSource g) {
        final List<UserEnumVertex> vertices =
                SFVertexFactory.loadVertices(g, g.V().hasLabel(ASTConstants.NodeType.USER_ENUM));

        for (UserEnumVertex vertex : vertices) {
            ApexEnum apexEnum = new ApexEnum(vertex.getDefiningType(), vertex.getValues());
            enums.put(apexEnum.getName(), apexEnum);
        }
    }
}
