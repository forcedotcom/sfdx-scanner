package com.salesforce.graph;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.salesforce.graph.vertex.BaseSFVertex;
import com.salesforce.graph.vertex.VertexPredicate;
import com.salesforce.graph.visitor.PathVertex;
import java.util.ArrayList;

/**
 * Tracks information about specific vertex types found in an ApexPath. This class will contain all
 * vertices where {@link VertexPredicate#test(BaseSFVertex)} has returned true.
 */
public final class ApexPathVertexMetaInfo implements DeepCloneable<ApexPathVertexMetaInfo> {
    private final Multimap<Class<? extends BaseSFVertex>, PredicateMatch> matches;

    public ApexPathVertexMetaInfo() {
        this.matches = HashMultimap.create();
    }

    private ApexPathVertexMetaInfo(ApexPathVertexMetaInfo other) {
        this.matches = HashMultimap.create();
        this.matches.putAll(other.matches);
    }

    @Override
    public ApexPathVertexMetaInfo deepClone() {
        return new ApexPathVertexMetaInfo(this);
    }

    public void addVertex(VertexPredicate predicate, PathVertex pathVertex) {
        matches.put(pathVertex.getVertex().getClass(), new PredicateMatch(predicate, pathVertex));
    }

    /** Return all matches of a specific vertex type */
    public ArrayList<PredicateMatch> getMatches(Class<? extends BaseSFVertex> clazz) {
        return new ArrayList<>(matches.get(clazz));
    }

    /** Return all matches regardless of vertex type */
    public ArrayList<PredicateMatch> getAllMatches() {
        return new ArrayList<>(matches.values());
    }

    /**
     * Keep track of which {@link VertexPredicate} expressed interest in which {@link PathVertex}.
     * This allows the filtering of paths using a single call to {@link
     * VertexPredicate#test(BaseSFVertex)}, which is useful since the match is sometimes path based
     * and will not work correctly if called from a static context.
     */
    public static final class PredicateMatch {
        private final VertexPredicate predicate;
        private final PathVertex pathVertex;

        private PredicateMatch(VertexPredicate predicate, PathVertex pathVertex) {
            this.predicate = predicate;
            this.pathVertex = pathVertex;
        }

        public VertexPredicate getPredicate() {
            return predicate;
        }

        public PathVertex getPathVertex() {
            return pathVertex;
        }
    }
}
