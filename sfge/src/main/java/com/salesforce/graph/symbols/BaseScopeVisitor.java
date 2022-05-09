package com.salesforce.graph.symbols;

import com.salesforce.graph.DeepCloneable;
import com.salesforce.graph.ops.CloneUtil;
import com.salesforce.graph.visitor.DefaultThrowVertexVisitor;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/** Derives from the throw visitor to ensure we aren't accidentally missing vertices. */
abstract class BaseScopeVisitor<T extends BaseScopeVisitor> extends DefaultThrowVertexVisitor
        implements ScopeVisitor, MutableSymbolProvider {

    /** Used to give each PathScopeVisitor a unique id */
    private static final AtomicLong SCOPE_ID_INCREMENTER = new AtomicLong();

    /** Unique id for this scope */
    private final Long id;

    /** Tracks which scope this scope was cloned from */
    private final Long clonedFromId;

    /**
     * Outer scope that provides context, new variable declarations will hide the inherited scope,
     * new assignments without declaration will write to the inherited scope. Methods should never
     * be called on this because some subclasses implement a decoupled inheritedScope and don't
     * store this value. All access should be via {@link #getInheritedScope()}
     */
    private final T inheritedScope;

    @SuppressWarnings(
            "PMD.NullAssignment") // clonedFromId has an alternative final value set in another
    // constructor
    protected BaseScopeVisitor(T inheritedScope) {
        this.inheritedScope = inheritedScope;
        this.id = SCOPE_ID_INCREMENTER.incrementAndGet();
        this.clonedFromId = null;
    }

    /** Copy constructor. Boolean is added to remove ambiguity with other constructor */
    protected BaseScopeVisitor(BaseScopeVisitor other, boolean ignored) {
        this.inheritedScope = (T) CloneUtil.clone((DeepCloneable) other.inheritedScope);
        this.id = SCOPE_ID_INCREMENTER.incrementAndGet();
        this.clonedFromId = other.id;
    }

    protected Optional<T> getInheritedScope() {
        return Optional.ofNullable(inheritedScope);
    }

    public Long getId() {
        return this.id;
    }

    public Optional<Long> getClonedFromId() {
        return Optional.ofNullable(this.clonedFromId);
    }

    @Override
    public String toString() {
        return "BaseScopeVisitor{" + "id=" + id + ", clonedFromId=" + clonedFromId + '}';
    }
}
