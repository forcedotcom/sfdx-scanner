package com.salesforce.graph.ops;

import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.has;
import static org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__.out;

import com.salesforce.apex.jorje.ASTConstants;
import com.salesforce.graph.Schema;
import com.salesforce.graph.build.CaseSafePropertyUtil;
import com.salesforce.graph.vertex.MethodVertex;
import com.salesforce.graph.vertex.SFVertexFactory;
import com.salesforce.graph.vertex.UserClassVertex;
import com.salesforce.metainfo.MetaInfoCollectorProvider;
import com.salesforce.rules.AbstractRuleRunner;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.tinkerpop.gremlin.process.traversal.Order;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.Scope;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversal;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.GraphTraversalSource;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.__;
import org.apache.tinkerpop.gremlin.structure.Vertex;

/**
 * Util class for identifying and interacting with path entry points. A path entry point being
 * defined as a point with which an external actor can interact, thus starting code execution.
 */
public final class PathEntryPointUtil {

    /**
     * Indicates whether a method vertex is a path entry point, e.g., a point where path analysis
     * can begin.
     */
    public static boolean isPathEntryPoint(MethodVertex methodVertex) {
        // Global methods are entry points.
        if (methodVertex.getModifierNode().isGlobal()) {
            return true;
        }
        // Methods that return PageReference objects are entry points.
        if (methodVertex.getReturnType().equalsIgnoreCase(Schema.PAGE_REFERENCE)) {
            return true;
        }
        // Certain annotations can designate a method as an entry point.
        String[] entryPointAnnotations =
                new String[] {
                    Schema.AURA_ENABLED,
                    Schema.NAMESPACE_ACCESSIBLE,
                    Schema.REMOTE_ACTION,
                    Schema.INVOCABLE_METHOD
                };
        for (String annotation : entryPointAnnotations) {
            if (methodVertex.hasAnnotation(annotation)) {
                return true;
            }
        }
        // Exposed methods on VF controllers are entry points.
        Set<String> vfControllers =
                MetaInfoCollectorProvider.getVisualForceHandler().getMetaInfoCollected().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet());
        if (vfControllers.contains(methodVertex.getDefiningType().toLowerCase())) {
            return true;
        }

        // InboundEmailHandler methods are entry points.
        // NOTE: This is a pretty cursory check and may struggle with nested inheritance. This isn't
        // likely to happen, but if it does, we can make the check more robust.
        Optional<UserClassVertex> parentClass = methodVertex.getParentClass();
        return parentClass.isPresent()
                && parentClass.get().getInterfaceNames().stream()
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
                        // Does the parent class implement InboundEmailHandler?
                        .contains(Schema.INBOUND_EMAIL_HANDLER.toLowerCase())
                //  Does the method return an InboundEmailResult?
                && methodVertex.getReturnType().equalsIgnoreCase(Schema.INBOUND_EMAIL_RESULT)
                // Is the method named handleInboundEmail?
                && methodVertex.getName().equalsIgnoreCase(Schema.HANDLE_INBOUND_EMAIL);
    }

    /** Load all path entry points in the graph. */
    static List<MethodVertex> getPathEntryPoints(GraphTraversalSource g) {
        return getPathEntryPoints(g, new ArrayList<>());
    }

    /**
     * Load all path entry points specified by the target objects. An empty list implicitly includes
     * all files.
     */
    public static List<MethodVertex> getPathEntryPoints(
            GraphTraversalSource g, List<AbstractRuleRunner.RuleRunnerTarget> targets) {
        // Sort the list of targets into full-file targets and method-level targets.
        List<String> fileLevelTargets =
                targets.stream()
                        .filter(t -> t.getTargetMethods().isEmpty())
                        .map(AbstractRuleRunner.RuleRunnerTarget::getTargetFile)
                        .collect(Collectors.toList());
        List<AbstractRuleRunner.RuleRunnerTarget> methodLevelTargets =
                targets.stream()
                        .filter(t -> !t.getTargetMethods().isEmpty())
                        .collect(Collectors.toList());

        // Internally, we'll use a Set to preserve uniqueness.
        Set<MethodVertex> methods = new HashSet<>();

        // If there are any explicitly targeted files, we must process them. If there are no
        // explicit targets of any kind,
        // then all files are implicitly targeted.
        if (!fileLevelTargets.isEmpty() || targets.isEmpty()) {
            // Use the file-level targets to get aura-enabled methods...
            methods.addAll(getAuraEnabledMethods(g, fileLevelTargets));
            // ...and NamespaceAccessible methods...
            methods.addAll(getNamespaceAccessibleMethods(g, fileLevelTargets));
            // ...and RemoteAction methods...
            methods.addAll(getRemoteActionMethods(g, fileLevelTargets));
            // ...and InvocableMethod methods...
            methods.addAll(getInvocableMethodMethods(g, fileLevelTargets));
            // ...and PageReference methods...
            methods.addAll(getPageReferenceMethods(g, fileLevelTargets));
            // ...and global-exposed methods...
            methods.addAll(getGlobalMethods(g, fileLevelTargets));
            // ...and implementations of Messaging.InboundEmailHandler#handleInboundEmail...
            methods.addAll(getInboundEmailHandlerMethods(g, fileLevelTargets));
            // ...and exposed methods on VF controllers.
            methods.addAll(getExposedControllerMethods(g, fileLevelTargets));
        }

        // Also, if there are any specifically targeted methods, they should be included.
        if (!methodLevelTargets.isEmpty()) {
            methods.addAll(MethodUtil.getTargetedMethods(g, methodLevelTargets));
        }
        // Turn the Set into a List so we can return it.
        return new ArrayList<>(methods);
    }

    /**
     * Returns non-test methods in the target files with an @AuraEnabled annotation. An empty list
     * implicitly includes all files.
     */
    private static List<MethodVertex> getAuraEnabledMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return getMethodsWithAnnotation(g, targetFiles, Schema.AURA_ENABLED);
    }

    /**
     * Returns non-test methods in the target files with a @NamespaceAccessible annotation. An empty
     * list implicitly includes all files.
     */
    private static List<MethodVertex> getNamespaceAccessibleMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return getMethodsWithAnnotation(g, targetFiles, Schema.NAMESPACE_ACCESSIBLE);
    }

    /**
     * Returns non-test methods in the target files with a @RemoteAction annotation. An empty list
     * implicitly includes all files.
     */
    private static List<MethodVertex> getRemoteActionMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return getMethodsWithAnnotation(g, targetFiles, Schema.REMOTE_ACTION);
    }

    /**
     * Returns non-test methods in the target files with an @InvocableMethod annotation. An empty
     * list implicitly includes all files.
     */
    private static List<MethodVertex> getInvocableMethodMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return getMethodsWithAnnotation(g, targetFiles, Schema.INVOCABLE_METHOD);
    }

    static List<MethodVertex> getMethodsWithAnnotation(
            GraphTraversalSource g, List<String> targetFiles, String annotation) {
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .where(
                                out(Schema.CHILD)
                                        .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                        .out(Schema.CHILD)
                                        .where(
                                                CaseSafePropertyUtil.H.has(
                                                        ASTConstants.NodeType.ANNOTATION,
                                                        Schema.NAME,
                                                        annotation)))
                        .order(Scope.global)
                        .by(Schema.DEFINING_TYPE, Order.asc)
                        .by(Schema.NAME, Order.asc));
    }

    /**
     * Returns non-test methods in the target files whose return type is a PageReference. An empty
     * list implicitly includes all files.
     */
    static List<MethodVertex> getPageReferenceMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.RETURN_TYPE,
                                        Schema.PAGE_REFERENCE))
                        .order(Scope.global)
                        .by(Schema.DEFINING_TYPE, Order.asc)
                        .by(Schema.NAME, Order.asc));
    }

    private static GraphTraversal<Vertex, Vertex> rootMethodTraversal(
            GraphTraversalSource g, List<String> targetFiles) {
        // Only look at UserClass vertices. Not interested in Enums, Interfaces, or Triggers
        final String[] labels = new String[] {ASTConstants.NodeType.USER_CLASS};
        return TraversalUtil.fileRootTraversal(g, labels, targetFiles)
                .not(has(Schema.IS_TEST, true))
                .repeat(__.out(Schema.CHILD))
                .until(__.hasLabel(ASTConstants.NodeType.METHOD))
                .not(has(Schema.IS_TEST, true));
    }

    /**
     * Returns non-test methods in the target files whose modifier scope is `global`. An empty list
     * implicitly includes all files.
     */
    static List<MethodVertex> getGlobalMethods(GraphTraversalSource g, List<String> targetFiles) {
        // Get all methods in the target files.
        return SFVertexFactory.loadVertices(
                g,
                rootMethodTraversal(g, targetFiles)
                        .filter(
                                __.and(
                                        // If a method has at least one block statement, then it is
                                        // definitely actually declared, as
                                        // opposed to being an implicit method.
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.BLOCK_STATEMENT)
                                                .count()
                                                .is(P.gte(1)),
                                        // We only want global methods.
                                        out(Schema.CHILD)
                                                .hasLabel(ASTConstants.NodeType.MODIFIER_NODE)
                                                .has(Schema.GLOBAL, true),
                                        // Ignore any standard methods, otherwise will get a ton of
                                        // extra results.
                                        __.not(__.has(Schema.IS_STANDARD, true)))));
    }

    static List<MethodVertex> getInboundEmailHandlerMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        return SFVertexFactory.loadVertices(
                g,
                // Get any target class that implements the email handler interface.
                TraversalUtil.traverseImplementationsOf(
                                g, targetFiles, Schema.INBOUND_EMAIL_HANDLER)
                        // Get every implementation of the handle email method.
                        .out(Schema.CHILD)
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.NAME,
                                        Schema.HANDLE_INBOUND_EMAIL))
                        // Filter the results by return type and arity to limit the possibility of
                        // getting unnecessary results.
                        .where(
                                CaseSafePropertyUtil.H.has(
                                        ASTConstants.NodeType.METHOD,
                                        Schema.RETURN_TYPE,
                                        Schema.INBOUND_EMAIL_RESULT))
                        .has(Schema.ARITY, 2));
    }

    /**
     * Returns all non-test public- and global-scoped methods in controllers referenced by
     * VisualForce pages, filtered by target file list. An empty list implicitly includes all files.
     *
     * @param g
     * @param targetFiles
     * @return
     */
    static List<MethodVertex> getExposedControllerMethods(
            GraphTraversalSource g, List<String> targetFiles) {
        Set<String> referencedVfControllers =
                MetaInfoCollectorProvider.getVisualForceHandler().getMetaInfoCollected();
        // If none of the VF files referenced an Apex class, we can just return an empty list.
        if (referencedVfControllers.isEmpty()) {
            return new ArrayList<>();
        }
        List<MethodVertex> allControllerMethods =
                SFVertexFactory.loadVertices(
                        g,
                        TraversalUtil.fileRootTraversal(g, targetFiles)
                                // Only outer classes can be VF controllers, so we should restrict
                                // our query to UserClasses.
                                .where(
                                        CaseSafePropertyUtil.H.hasWithin(
                                                ASTConstants.NodeType.USER_CLASS,
                                                Schema.DEFINING_TYPE,
                                                referencedVfControllers))
                                .repeat(__.out(Schema.CHILD))
                                .until(__.hasLabel(ASTConstants.NodeType.METHOD))
                                .not(has(Schema.IS_TEST, true))
                                // We want to ignore constructors.
                                .where(
                                        __.not(
                                                CaseSafePropertyUtil.H.hasWithin(
                                                        ASTConstants.NodeType.METHOD,
                                                        Schema.NAME,
                                                        Schema.INSTANCE_CONSTRUCTOR_CANONICAL_NAME,
                                                        Schema
                                                                .STATIC_CONSTRUCTOR_CANONICAL_NAME))));
        // Gremlin isn't sophisticated enough to perform this kind of filtering in the actual query.
        // So we'll just do it
        // manually here.
        return allControllerMethods.stream()
                .filter(
                        methodVertex ->
                                methodVertex.getModifierNode().isPublic()
                                        || methodVertex.getModifierNode().isGlobal())
                .collect(Collectors.toList());
    }

    private PathEntryPointUtil() {}
}
