package com.salesforce.graph.vertex;

import static com.salesforce.apex.jorje.ASTConstants.PROPERTY_METHOD_PREFIX;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.collections.CollectionUtil;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.symbols.MethodInvocationScope;
import com.salesforce.graph.symbols.ScopeUtil;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.symbols.apex.ApexValue;
import com.salesforce.graph.visitor.PathVertexVisitor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;

public abstract class VariableExpressionVertex extends ChainedVertex
        implements KeyVertex, NamedVertex {
    private final ExpressionType expressionType;
    private final ChainedNamesInitializer chainedNamesInitializer;
    private final FullNameInitializer fullNameInitializer;
    private final SymbolicNameChainInitializer symbolicNameChainInitializer;
    protected final LazyVertex<AbstractReferenceExpressionVertex> referenceExpression;

    VariableExpressionVertex(Map<Object, Object> properties, ExpressionType expressionType) {
        super(properties);
        this.expressionType = expressionType;
        this.referenceExpression = _getReferenceExpression();
        this.chainedNamesInitializer = new ChainedNamesInitializer();
        this.fullNameInitializer = new FullNameInitializer();
        this.symbolicNameChainInitializer = new SymbolicNameChainInitializer();
    }

    @Override
    public ExpressionType getExpressionType() {
        return expressionType;
    }

    @Override
    public Optional<String> getKeyName() {
        if (expressionType.equals(ExpressionType.KEY_VALUE)) {
            return Optional.of(getString(Schema.KEY_NAME));
        } else {
            return Optional.of(getName());
        }
    }

    @Override
    public Optional<String> getSymbolicName() {
        if (chainedNamesInitializer.get().isEmpty()) {
            return Optional.of(getName());
        } else {
            return Optional.of(chainedNamesInitializer.get().get(0));
        }
    }

    public List<String> getSymbolicNameChain() {
        return symbolicNameChainInitializer.get();
    }

    private final class SymbolicNameChainInitializer
            extends UncheckedLazyInitializer<List<String>> {
        @Override
        protected List<String> initialize() throws ConcurrentException {
            final List<String> results = new ArrayList<>();
            if (!chainedNamesInitializer.get().isEmpty()) {
                results.addAll(chainedNamesInitializer.get());
            }
            results.add(getName());
            return results;
        }
    }

    public boolean isThisReference() {
        if (referenceExpression.get() instanceof ReferenceExpressionVertex) {
            return ((ReferenceExpressionVertex) referenceExpression.get())
                    .getThisVariableExpression()
                    .isPresent();
        } else {
            return false;
        }
    }

    private final class ChainedNamesInitializer extends UncheckedLazyInitializer<List<String>> {
        @Override
        protected List<String> initialize() throws ConcurrentException {
            return Collections.unmodifiableList(referenceExpression.get().getNames());
        }
    }

    // TODO: Law of demeter
    public AbstractReferenceExpressionVertex getReferenceExpression() {
        return referenceExpression.get();
    }

    public Optional<ArrayLoadExpressionVertex> getArrayLoadExpressionVertex() {
        return referenceExpression.get().getArrayLoadExpressionVertex();
    }

    @Override
    public List<String> getChainedNames() {
        return chainedNamesInitializer.get();
    }

    @Override
    public String getName() {
        return getString(Schema.NAME);
    }

    public String getFullName() {
        return fullNameInitializer.get();
    }

    private final class FullNameInitializer extends UncheckedLazyInitializer<String> {
        @Override
        protected String initialize() throws ConcurrentException {
            StringBuilder sb = new StringBuilder();
            chainedNamesInitializer.get().forEach(n -> sb.append(n).append('.'));
            sb.append(getName());
            return sb.toString();
        }
    }

    private LazyVertex<AbstractReferenceExpressionVertex> _getReferenceExpression() {
        return new LazyVertex<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(
                                        NodeType.EMPTY_REFERENCE_EXPRESSION, // Assignment of other
                                        // variable
                                        NodeType.REFERENCE_EXPRESSION) // Invocation of method
                                .has(Schema.CHILD_INDEX, 0));
    }

    public static final class Unknown extends VariableExpressionVertex {
        private Unknown(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        // This is an intermediate representation used by the builder. It should never be visited
        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            throw new UnexpectedException(this);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            throw new UnexpectedException(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            throw new UnexpectedException(this);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            throw new UnexpectedException(this);
        }
    }

    public static final class Single extends VariableExpressionVertex implements InvocableVertex {
        private Single(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        public String getFullName() {
            return String.join(".", getSymbolicNameChain());
        }

        @Override
        public Optional<InvocableVertex> getNext() {
            return Optional.empty();
        }

        @Override
        public InvocableVertex getLast() {
            return this;
        }

        @Override
        public List<InvocableVertex> firstToList() {
            return Collections.singletonList(this);
        }

        @Override
        public List<ChainedVertex> getParameters() {
            if (!(referenceExpression.get() instanceof AbstractReferenceExpressionVertex)) {
                // There can't be parameters if it's a standalone variable.
                // TODO: Should this check for chained names instead?
                return Collections.emptyList();
            }

            if (referenceExpression.get() instanceof EmptyReferenceExpressionVertex) {
                // Empty reference expressions occur when the property is accessed from within a
                // class without the
                // class name for a static property or the this qualifier for an instance property
                if (getParent() instanceof AssignmentExpressionVertex
                        && getChildIndex().equals(0)) {
                    // STORE
                    return Collections.singletonList(getNextSibling());
                } else {
                    // LOAD
                    return Collections.emptyList();
                }
            } else if (referenceExpression.get() instanceof ReferenceExpressionVertex) {
                String referenceType =
                        ((ReferenceExpressionVertex) referenceExpression.get()).getReferenceType();
                if (referenceType.equals(ASTConstants.ReferenceType.STORE)) {
                    return Collections.singletonList(getNextSibling());
                } else if (referenceType.equals(ASTConstants.ReferenceType.LOAD)) {
                    return Collections.emptyList();
                } else {
                    throw new UnexpectedException(this);
                }
            } else {
                throw new UnexpectedException(this);
            }
        }

        @Override
        public MethodInvocationScope resolveInvocationParameters(
                MethodVertex methodVertex, SymbolProvider symbols) {
            List<ChainedVertex> parameters = getParameters();

            TreeMap<String, Pair<Typeable, ApexValue<?>>> apexValueParameters =
                    CollectionUtil.newTreeMap();
            if (parameters.size() == 1) {
                ApexValue<?> apexValue =
                        ScopeUtil.resolveToApexValue(symbols, parameters.get(0)).orElse(null);
                Pair<Typeable, ApexValue<?>> pair =
                        Pair.of(methodVertex.getParameters().get(0), apexValue);
                apexValueParameters.put("value", pair);
            }

            return new MethodInvocationScope(this, apexValueParameters);
        }
    }

    /**
     * This is special case when a property refers to itself from within an instance property
     * method. It is different than {@link Single} in order to avoid recursion.
     *
     * <p>public integer prop { get { return prop; } set { prop = value; } }
     */
    public static final class SelfReferentialInstanceProperty extends VariableExpressionVertex {
        private SelfReferentialInstanceProperty(
                Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        public String getFullName() {
            return String.join(".", getSymbolicNameChain());
        }
    }

    /**
     * This is special case when a property refers to itself from within a static property method.
     * It is different than {@link Single} in order to avoid recursion.
     *
     * <p>public static integer prop { get { return prop; } set { prop = value; } }
     */
    public static final class SelfReferentialStaticProperty extends VariableExpressionVertex {
        private SelfReferentialStaticProperty(Map<Object, Object> properties, ExpressionType type) {
            super(properties, type);
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        public String getFullName() {
            return String.join(".", getSymbolicNameChain());
        }
    }

    public static final class ForLoop extends VariableExpressionVertex {
        private final ChainedVertex forLoopValues;

        private ForLoop(Map<Object, Object> properties, ChainedVertex forLoopValues) {
            super(properties, ExpressionType.SIMPLE);
            this.forLoopValues = forLoopValues;
        }

        public ForLoop cloneWithResolvedValues(ChainedVertex resolved) {
            return SFVertexFactory.load(g(), getId(), resolved);
        }

        public ChainedVertex getForLoopValues() {
            return forLoopValues;
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }
    }

    /**
     * Represents a Standard Apex object that can be referred to without being declared. i.e
     * MyObject__c.SObjectType
     */
    public static final class Standard extends VariableExpressionVertex implements Typeable {
        /** Original value as present in the AST. May not be the canonical. */
        private final String vertexType;

        private final ApexValue<?> apexValue;

        private Standard(
                Map<Object, Object> properties, String vertexType, ApexValue<?> apexValue) {
            super(properties, ExpressionType.SIMPLE);
            this.vertexType = vertexType;
            this.apexValue = apexValue;
        }

        @Override
        public boolean visit(PathVertexVisitor visitor, SymbolProvider symbols) {
            return visitor.visit(this, symbols);
        }

        @Override
        public boolean visit(SymbolProviderVertexVisitor visitor) {
            return visitor.visit(this);
        }

        @Override
        public void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols) {
            visitor.afterVisit(this, symbols);
        }

        @Override
        public void afterVisit(SymbolProviderVertexVisitor visitor) {
            visitor.afterVisit(this);
        }

        @Override
        public String getCanonicalType() {
            return ApexStandardLibraryUtil.getCanonicalName(vertexType);
        }

        public ApexValue<?> getApexValue() {
            return apexValue;
        }
    }

    @SuppressWarnings("unused") // Called via reflection
    public static final class Builder {
        public static VariableExpressionVertex create(Map<Object, Object> vertex) {
            return create(vertex, null);
        }

        // for (String fieldToCheck : new String [] {'Name', 'Phone'})
        private static ChainedVertex getForEachLoopReferenceVariable(Long id) {
            return SFVertexFactory.loadSingleOrNull(
                    g(),
                    g().V(id)
                            // If this vertex's heritage suggests it's the iteration variable in a
                            // for-each loop...
                            .out(Schema.PARENT)
                            .hasLabel(NodeType.VARIABLE_DECLARATION)
                            .out(Schema.PARENT)
                            .hasLabel(NodeType.VARIABLE_DECLARATION_STATEMENTS)
                            .out(Schema.PARENT)
                            .hasLabel(NodeType.FOR_EACH_STATEMENT)
                            // ...then get the vertex indicating the iterable over which the loop
                            // iterates.
                            .out(Schema.CHILD)
                            .has(Schema.FIRST_CHILD, true));
        }

        // for (Integer i = 0; i < fieldsToCheck.size(); i++)
        private static ChainedVertex getForLoopReferenceVariable(Long id) {
            List<VariableExpressionVertex> arrayVertices =
                    SFVertexFactory.loadVertices(
                            g(),
                            g().V(id)
                                    // Follow the incoming NextSibling edge back to the previous
                                    // sibling.
                                    .in(Schema.NEXT_SIBLING)
                                    // Make sure that the sibling in question is an ArrayLoad
                                    // expression.
                                    .hasLabel(NodeType.ARRAY_LOAD_EXPRESSION)
                                    // Make sure that this node is a descendent of a BlockStatement
                                    // within a ForLoop.
                                    .where(
                                            __.repeat(__.out(Schema.PARENT))
                                                    .until(__.hasLabel(NodeType.BLOCK_STATEMENT))
                                                    .out(Schema.PARENT)
                                                    .hasLabel(NodeType.FOR_LOOP_STATEMENT))
                                    // Get the children of the ArrayLoad expression.
                                    .out(Schema.CHILD)
                                    .order()
                                    .by(Schema.CHILD_INDEX)
                                    // Make sure they're both VariableExpression nodes.
                                    .hasLabel(NodeType.VARIABLE_EXPRESSION));

            if (arrayVertices == null || arrayVertices.size() != 2) {
                // If there aren't exactly two vertices returned by the query, then there's no loop
                // reference variable.
                return null;
            }

            // TODO: Everything after here is DRAMATICALLY overfitted to the specific use case of
            // identifying when we're
            //  iterating over all of the entries in an array. That's fine for now, but we'll almost
            // certainly want to
            //  have a more generic "is this variable's value loop-dependent" check in the future.
            String arrayName = arrayVertices.get(0).getName();
            String indexName = arrayVertices.get(1).getName();

            ForLoopStatementVertex forLoopStatement =
                    SFVertexFactory.load(
                            g(),
                            g().V(id)
                                    .repeat(__.out(Schema.PARENT))
                                    .until(__.hasLabel(NodeType.FOR_LOOP_STATEMENT)));

            if (!forLoopStatement.isSupportedSimpleIncrementingType(arrayName, indexName)) {
                return null;
            }

            // If we're here, then all of the conditions for a for-loop are satisfied.
            return arrayVertices.get(0);
        }

        public static VariableExpressionVertex create(
                Map<Object, Object> vertex, Object supplementalParam) {
            // TODO: Simpler way to get id
            Unknown unknownVertex = new Unknown(vertex, ExpressionType.UNKNOWN);
            Long id = unknownVertex.getId();

            ApexValue<?> apexValue =
                    ApexStandardLibraryUtil.getStandardType(unknownVertex).orElse(null);
            if (apexValue != null) {
                final String type;
                if (apexValue instanceof Typeable) {
                    Typeable typeable = (Typeable) apexValue;
                    type = typeable.getCanonicalType();
                } else {
                    type = unknownVertex.getName();
                }
                return new Standard(vertex, type, apexValue);
            } else {
                // Variables defined within a for loop may contain one of many variables
                // This vertex is the value that the variable in the foreach points to
                ChainedVertex loopReferenceVariable = getForEachLoopReferenceVariable(id);
                loopReferenceVariable =
                        loopReferenceVariable != null
                                ? loopReferenceVariable
                                : getForLoopReferenceVariable(id);

                if (loopReferenceVariable != null) {
                    if (supplementalParam != null) {
                        return new ForLoop(vertex, (ChainedVertex) supplementalParam);
                    } else {
                        return new ForLoop(vertex, loopReferenceVariable);
                    }
                } else {
                    // Example of KEY_VALUE type. TODO: Test
                    // static void testUDRChangeBehavior(string strProcessor){
                    // npe01__Contacts_and_Orgs_Settings__c contactSettingsForTests =
                    // UTIL_CustomSettingsFacade.getContactsSettingsForTests(new
                    // npe01__Contacts_and_Orgs_Settings__c (
                    //        npe01__Account_Processor__c = strProcessor,
                    //        npe01__Enable_Opportunity_Contact_Role_Trigger__c = true,
                    //        npe01__Opportunity_Contact_Role_Default_role__c =
                    // CAO_Constants.OCR_DONOR_ROLE
                    // TODO: Efficiency: the graph could be annotated with this information
                    MethodVertex parentMethod = unknownVertex.getParentMethod().orElse(null);

                    // Detect if the variable is within a specially named method. This indicates it
                    // is a variable named the same thing as a property and it
                    // is being referenced from within the property method body
                    if (parentMethod != null
                            && parentMethod
                                    .getName()
                                    .equalsIgnoreCase(
                                            PROPERTY_METHOD_PREFIX + unknownVertex.getName())) {
                        if (supplementalParam != null) {
                            if (parentMethod.isStatic()) {
                                return new SelfReferentialStaticProperty(
                                        vertex, (ExpressionType) supplementalParam);
                            } else {
                                return new SelfReferentialInstanceProperty(
                                        vertex, (ExpressionType) supplementalParam);
                            }
                        } else {
                            if (parentMethod.isStatic()) {
                                return new SelfReferentialStaticProperty(
                                        vertex, ExpressionType.SIMPLE);
                            } else {
                                return new SelfReferentialInstanceProperty(
                                        vertex, ExpressionType.SIMPLE);
                            }
                        }
                    } else {
                        if (supplementalParam != null) {
                            return new Single(vertex, (ExpressionType) supplementalParam);
                        } else {
                            return new Single(vertex, ExpressionType.SIMPLE);
                        }
                    }
                }
            }
        }
    }
}
