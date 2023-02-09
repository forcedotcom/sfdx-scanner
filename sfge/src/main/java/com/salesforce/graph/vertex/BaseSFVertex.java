package com.salesforce.graph.vertex;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.hasLabel;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.CollectibleObject;
import com.salesforce.NullCollectibleObject;
import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.apex.jorje.ASTConstants.NodeType;
import com.salesforce.exception.TodoException;
import com.salesforce.exception.UnexpectedException;
import com.salesforce.graph.Schema;
import com.salesforce.graph.cache.VertexCacheProvider;
import com.salesforce.graph.ops.directive.EngineDirective;
import com.salesforce.graph.ops.directive.EngineDirectiveCommand;
import com.salesforce.graph.symbols.SymbolProvider;
import com.salesforce.graph.symbols.SymbolProviderVertexVisitor;
import com.salesforce.graph.visitor.PathVertexVisitor;
import com.salesforce.graph.visitor.TypedVertexVisitor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.concurrent.ThreadSafe;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.T;

/**
 * The Java representation of a Tinkerpop vertex.
 *
 * <p><b>IMPORTANT</b> All subclasses must be immutable and written in a ThreadSafe manner. Any lazy
 * loading should use the LazyVertex classes or derive a new class from {@link
 * UncheckedLazyInitializer} for types that aren't vertices.
 */
@ThreadSafe
public abstract class BaseSFVertex implements CollectibleObject, SFVertex {
    /** Value used in collections that don't support null values. See {@link CollectibleObject} */
    public static final NullCollectibleObject<BaseSFVertex> NULL_VALUE =
            new NullCollectibleObject<>(BaseSFVertex.class);

    // TODO Make private
    protected final Map<String, Object> properties;

    private final MappedLazyOptionalVertex<String, BaseSFVertex> firstParentOfType;
    private final MappedLazyVertexList<String, BaseSFVertex> childrenByName;
    private final MappedLazyVertex<Integer, BaseSFVertex> siblings;
    private final LazyVertexList<BaseSFVertex> children;
    private final LazyVertex<BaseSFVertex> parent;
    private final LazyOptionalVertex<UserClassVertex> parentClass;
    private final LazyVertexList<AnnotationVertex> previousLineAnnotations;

    /**
     * Unique id that identifies the vertex in the tinkerpop graph. This is not kept in {@link
     * #properties} in order to improve efficiency.
     */
    private final Long id;

    /**
     * Label of the vertex in the tinkerpop graph, this is typically the top level Java classname
     * minus the "Vertex" suffix. This is not kept in {@link #properties} in order to improve
     * efficiency.
     */
    private final String label;

    private final int hash;

    BaseSFVertex(Map<Object, Object> properties) {
        final Map<String, Object> vertexMap = new HashMap<>();

        Long idFromProperties = null;
        String labelFromProperties = null;
        for (Map.Entry<Object, Object> e : properties.entrySet()) {
            final Object key = e.getKey();
            final String stringKey;
            if (key instanceof T) {
                // Handle special case where the keys are enums
                switch ((T) key) {
                    case id:
                        idFromProperties = (Long) e.getValue();
                        continue;
                    case label:
                        labelFromProperties = (String) e.getValue();
                        continue;
                    default:
                        throw new UnexpectedException(key);
                }
            } else if (key instanceof String) {
                stringKey = (String) key;
            } else {
                throw new UnexpectedException(key);
            }

            Object value = e.getValue();
            if (value instanceof Object[]) {
                // Convert arrays to lists. The graph contains arrays, but lists are easier to work
                // with
                value = new ArrayList<>(Arrays.asList((Object[]) value));
            }
            if (value instanceof String) {
                value = ((String) value).intern();
            }
            vertexMap.put(stringKey, value);
        }

        assert idFromProperties != null;
        assert labelFromProperties != null;

        this.properties = Collections.unmodifiableMap(vertexMap);
        this.id = idFromProperties;
        this.hash = Objects.hashCode(this.id);
        this.label = labelFromProperties;

        // Lazy vertices
        this.childrenByName = _getChildrenOfType();
        this.siblings = _getSiblings();
        this.firstParentOfType = _getFirstParentOfType();
        this.children = _getChildren();
        this.parent = _getParent();
        this.parentClass = _getParentClass();
        this.previousLineAnnotations = _getPreviousLineAnnotations();
    }

    /**
     * Helper method to retrieve the full graph. Used for lazy loading etc.
     *
     * @return the GraphTraversalSource that all vertices are loaded from
     */
    protected static GraphTraversalSource g() {
        return VertexCacheProvider.get().getFullGraph();
    }

    // TODO: This should be name "accept" to more closely match the visitor pattern conventions
    public abstract boolean visit(PathVertexVisitor visitor, SymbolProvider symbols);

    public abstract boolean visit(SymbolProviderVertexVisitor visitor);

    public abstract void afterVisit(PathVertexVisitor visitor, SymbolProvider symbols);

    public abstract void afterVisit(SymbolProviderVertexVisitor visitor);

    public <T> T accept(TypedVertexVisitor<T> visitor) {
        throw new TodoException(
                "Implement in subclasses. className=" + this.getClass().getSimpleName());
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getDefiningType() {
        return getString(Schema.DEFINING_TYPE);
    }

    @Override
    public Integer getChildIndex() {
        return getInteger(Schema.CHILD_INDEX);
    }

    @Override
    public final Integer getBeginLine() {
        return getInteger(Schema.BEGIN_LINE);
    }

    @Override
    public Integer getEndLine() {
        return getInteger(Schema.END_LINE);
    }

    @Override
    public final Integer getBeginColumn() {
        return getInteger(Schema.BEGIN_COLUMN);
    }

    public boolean isStandardType() {
        return getBoolean(Schema.IS_STANDARD);
    }

    @Override
    public BaseSFVertex getCollectibleObject() {
        return this;
    }

    /**
     * Indicates if this node starts an inner scope. TODO: This is duplicative with
     * AstContants#START_INNER_SCOPE_LABELS
     */
    public boolean startsInnerScope() {
        return false;
    }

    /**
     * Value added to vertices that end a particular scope. For instance the last literal within an
     * if/else block would have a value of {@link
     * com.salesforce.apex.jorje.ASTConstants.NodeType#IF_ELSE_BLOCK_STATEMENT}
     */
    public List<String> getEndScopes() {
        return getStrings(Schema.END_SCOPES);
    }

    private LazyVertexList<BaseSFVertex> _getChildren() {
        return new LazyVertexList<>(
                () ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    public List<BaseSFVertex> getChildren() {
        return children.get();
    }

    private MappedLazyOptionalVertex<String, BaseSFVertex> _getFirstParentOfType() {
        return new MappedLazyOptionalVertex<>(
                parentLabel ->
                        g().V(getId()).repeat(out(Schema.PARENT)).until(hasLabel(parentLabel)));
    }

    public <T extends BaseSFVertex> Optional<T> getFirstParentOfType(String parentLabel) {
        return (Optional<T>) firstParentOfType.get(parentLabel);
    }

    private MappedLazyVertexList<String, BaseSFVertex> _getChildrenOfType() {
        return new MappedLazyVertexList<>(
                childLabel ->
                        g().V(getId())
                                .out(Schema.CHILD)
                                .hasLabel(childLabel)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    public <T extends BaseSFVertex> List<T> getChildren(String childLabel) {
        return (List<T>) childrenByName.get(childLabel);
    }

    public <T extends BaseSFVertex> T getOnlyChild() {
        List<BaseSFVertex> children = getChildren();

        if (children.size() != 1) {
            throw new UnexpectedException("this=" + this + ", children=" + children);
        }

        return (T) children.get(0);
    }

    public <T extends BaseSFVertex> T getOnlyChild(String childLabel) {
        T result = getOnlyChildOrNull(childLabel);

        if (result == null) {
            throw new UnexpectedException(this);
        }

        return result;
    }

    public <T extends BaseSFVertex> T getOnlyChildOrNull(String childLabel) {
        List<T> children = getChildren(childLabel);

        if (children.size() > 1) {
            throw new UnexpectedException(this);
        }

        return children.isEmpty() ? null : children.get(0);
    }

    public <T extends BaseSFVertex> T getChild(int index) {
        return (T) getChildren().get(index);
    }

    private LazyVertex<BaseSFVertex> _getParent() {
        return new LazyVertex<>(() -> g().V(getId()).out(Schema.PARENT));
    }

    public <T extends BaseSFVertex> T getParent() {
        return (T) parent.get();
    }

    /**
     * Returns this node's super-parent, i.e. the node at the very end of the chain of outgoing
     * parent edges.
     *
     * @param <T>
     * @return
     */
    private <T extends BaseSFVertex> T getSuperParent() {
        return SFVertexFactory.load(
                g(),
                g().V(getId())
                        // Create a list containing this vertex, and every vertex in the chain of of
                        // parent.
                        .union(__.identity(), __.repeat(__.out(Schema.PARENT)).emit())
                        // Order them by the number of parent vertices they have. The super-parent
                        // will always be at the start of
                        // the list, since it's the only vertex with exactly 0 parents.
                        .order()
                        .by(__.out(Schema.PARENT).count())
                        // Return only the first entry in the list, which is guaranteed to be the
                        // super-parent.
                        .limit(1));
    }

    @Override
    public String getFileName() {
        if (properties.containsKey(Schema.FILE_NAME)) {
            return getString(Schema.FILE_NAME);
        } else {
            return getSuperParent().getFileName();
        }
    }

    /**
     * Traverse up the {@link Schema#PARENT} edges to find the first vertex of type {@link
     * ASTConstants.NodeType#METHOD}
     */
    public Optional<MethodVertex> getParentMethod() {
        return getFirstParentOfType(NodeType.METHOD);
    }

    private LazyOptionalVertex<UserClassVertex> _getParentClass() {
        return new LazyOptionalVertex<>(
                () ->
                        g().V(getId())
                                .repeat(out(Schema.PARENT))
                                .until(hasLabel(NodeType.USER_CLASS)));
    }

    public Optional<UserClassVertex> getParentClass() {
        return parentClass.get();
    }

    /** Finds all Annotation vertices that are present on the previous line in the code */
    public List<AnnotationVertex> getAnnotations() {
        return previousLineAnnotations.get();
    }

    private LazyVertexList<AnnotationVertex> _getPreviousLineAnnotations() {
        return new LazyVertexList<>(
                () ->
                        g().V()
                                .hasLabel(NodeType.ANNOTATION)
                                .has(Schema.DEFINING_TYPE, getDefiningType())
                                .has(Schema.BEGIN_LINE, getBeginLine() - 1)
                                .has(Schema.END_LINE, getBeginLine() - 1)
                                .order(Scope.global)
                                .by(Schema.CHILD_INDEX, Order.asc));
    }

    public List<EngineDirective> getEngineDirectives() {
        List<EngineDirective> engineDirectives = new ArrayList<>();
        List<EngineDirectiveVertex> engineDirectiveVertices =
                getChildren(Schema.JorjeNodeType.ENGINE_DIRECTIVE);
        for (EngineDirectiveVertex engineDirectiveVertex : engineDirectiveVertices) {
            engineDirectives.add(engineDirectiveVertex.getEngineDirective());
        }
        return engineDirectives;
    }

    public List<EngineDirective> getEngineDirectives(EngineDirectiveCommand token) {
        return getEngineDirectives().stream()
                .filter(e -> e.getDirectiveToken().equals(token))
                .collect(Collectors.toList());
    }

    public List<EngineDirective> getAllEngineDirectives() {
        final List<EngineDirective> engineDirectives = new ArrayList<>();
        engineDirectives.addAll(getEngineDirectives());
        getParentMethod().ifPresent(m -> engineDirectives.addAll(m.getEngineDirectives()));
        getParentClass().ifPresent(c -> engineDirectives.addAll(c.getAllEngineDirectives()));
        // TODO: This needs to be more future proof
        for (String nodeType :
                new String[] {
                    NodeType.EXPRESSION_STATEMENT, NodeType.VARIABLE_DECLARATION_STATEMENTS
                }) {
            getFirstParentOfType(nodeType)
                    .ifPresent(p -> engineDirectives.addAll(p.getEngineDirectives()));
        }
        return engineDirectives;
    }

    /**
     * Aggregates all of the annotations that apply to the given vertex. Combining the different
     * levels of previous line, method, and class level annotation.
     */
    public List<AnnotationVertex> getAllAnnotations() {
        final List<AnnotationVertex> annotations = new ArrayList<>();
        annotations.addAll(getAnnotations());
        getParentMethod().ifPresent(m -> annotations.addAll(m.getAnnotations()));
        getParentClass().ifPresent(c -> annotations.addAll(c.getAllAnnotations()));
        return annotations;
    }

    private MappedLazyVertex<Integer, BaseSFVertex> _getSiblings() {
        return new MappedLazyVertex<>(
                offset ->
                        g().V(getId())
                                .out(Schema.PARENT)
                                .out(Schema.CHILD)
                                .has(Schema.CHILD_INDEX, getChildIndex() + offset));
    }

    public <T extends BaseSFVertex> T getOffsetSibling(int offset) {
        return (T) siblings.get(offset);
    }

    public <T extends BaseSFVertex> T getPreviousSibling() {
        return getOffsetSibling(-1);
    }

    public <T extends BaseSFVertex> T getNextSibling() {
        return getOffsetSibling(1);
    }

    /**
     * Workaround for the fact that gremlin sometimes adds lists as raw strings if the list has one
     * value. We also have to handle the fact that it sometimes adds Objects[] and Strings[]
     * depending on local or remote.
     */
    protected List<String> getStrings(String key) {
        Object result = properties.get(key);
        if (result == null) {
            return new ArrayList<>();
        } else if (result instanceof String) {
            return Collections.singletonList((String) result);
        } else if (result instanceof List) {
            return (List<String>) result;
        } else if (result instanceof String[]) {
            return Arrays.asList((String[]) result);
        } else if (result instanceof Object[]) {
            return Arrays.stream((Object[]) result)
                    .map(o -> (String) o)
                    .collect(Collectors.toList());
        } else {
            throw new UnexpectedException(this);
        }
    }

    protected boolean getBoolean(String key) {
        return toBoolean(properties.get(key));
    }

    protected String getString(String key) {
        return (String) properties.get(key);
    }

    protected int getInteger(String key) {
        Object result = properties.get(key);
        if (result instanceof Number) {
            // Integers just need to be cast to the proper type.
            return (Integer) result;
        } else if (result instanceof String) {
            // Strings need to be fed through Integer.valueOf().
            return Integer.valueOf((String) result);
        } else {
            throw new UnexpectedException(key);
        }
    }

    protected long getLong(String key) {
        Object result = properties.get(key);
        if (result instanceof Number) {
            // Longs just need to be cast to the proper type.
            return (Long) result;
        } else if (result instanceof String) {
            // Strings need to be fed through Integer.valueOf().
            return Long.valueOf((String) result);
        } else {
            throw new UnexpectedException(key);
        }
    }

    static boolean toBoolean(Object value) {
        if (value instanceof Boolean) {
            return value != null ? (Boolean) value : false;
        } else {
            return false;
        }
    }

    /**
     * Get the raw properties. Mainly provided to serialize the vertex. Don't use for business
     * decisions, add a getter instead.
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    protected Object getProperty(String name) {
        return properties.get(name);
    }

    protected <T> T getOrDefault(String key, T defaultValue) {
        return (T) properties.getOrDefault(key, defaultValue);
    }

    // Intentionally only use id for equals and hashcode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BaseSFVertex that = (BaseSFVertex) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public String toString() {
        return label + "{" + "properties=" + properties + '}';
    }

    public String toMinimalString() {
        return label
                + "{"
                + "DefiningType="
                + getDefiningType()
                + ", BeginLine="
                + getBeginLine()
                + ", EndLine="
                + getEndLine()
                + '}';
    }
}
