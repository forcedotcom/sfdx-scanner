package com.salesforce.apex.jorje;

import apex.jorje.data.Locations;
import apex.jorje.parser.impl.BaseApexLexer;
import apex.jorje.parser.impl.HiddenToken;
import apex.jorje.semantic.ast.AstNode;
import apex.jorje.semantic.ast.visitor.NoopScope;
import apex.jorje.semantic.common.EmptySymbolProvider;
import apex.jorje.semantic.common.TestAccessEvaluator;
import apex.jorje.semantic.common.TestQueryValidators;
import apex.jorje.semantic.compiler.ApexCompiler;
import apex.jorje.semantic.compiler.CodeUnit;
import apex.jorje.semantic.compiler.CompilationInput;
import apex.jorje.semantic.compiler.CompilerStage;
import apex.jorje.semantic.compiler.SourceFile;
import apex.jorje.semantic.compiler.ValidationSettings;
import apex.jorje.semantic.compiler.parser.ParserEngine;
import apex.jorje.semantic.compiler.sfdc.NoopCompilerProgressCallback;
import apex.jorje.services.exception.CompilationException;
import apex.jorje.services.exception.ParseException;
import com.salesforce.config.UserFacingMessages;
import com.salesforce.exception.SfgeRuntimeException;
import com.salesforce.exception.UnexpectedException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/** Converts string source code into a node that can be imported into the graph. */
public final class JorjeUtil {
    static {
        // Inform Jorje to track locations
        Locations.useIndexFactory();
        // Increment log level to avoid printing info lines
        incrementLogLevel();
    }

    public static AstNodeWrapper<?> compileApexFromString(String sourceCode) {
        final SourceFile sourceFile = SourceFile.builder().setBody(sourceCode).build();

        final CompilationInput input =
                new CompilationInput(
                        Collections.singletonList(sourceFile),
                        EmptySymbolProvider.get(),
                        new TestAccessEvaluator(),
                        new TestQueryValidators.Noop(),
                        null,
                        NoopCompilerProgressCallback.get());

        final ValidationSettings validationSettings =
                ValidationSettings.builder()
                        .setValidationBehavior(
                                ValidationSettings.ValidationBehavior.COLLECT_MULTIPLE_ERRORS)
                        .build();

        final ApexCompiler compiler =
                ApexCompiler.builder()
                        .setInput(input)
                        .setValidationSettings(validationSettings)
                        .setHiddenTokenBehavior(ParserEngine.HiddenTokenBehavior.COLLECT_COMMENTS)
                        .build();

        final List<CodeUnit> codeUnits = compiler.compile(CompilerStage.POST_TYPE_RESOLVE);

        if (codeUnits.size() != 1) {
            throw new UnexpectedException(codeUnits);
        }
        final CodeUnit codeUnit = codeUnits.get(0);

        // Find any ParseExceptions. Other exceptions are ignored because the compiler does not have
        // all of the relevant pieces needed to check semantics.
        // Sometimes the same ParseException appears multiple times, so we should use a Set to
        // make sure that the messages are unique.
        final Set<String> exceptionMessages = new HashSet<>();
        final List<CompilationException> exceptions =
                codeUnit.getErrors().get().stream()
                        // Set.add() returns true if the thing being added wasn't already in the
                        // set.
                        .filter(
                                e ->
                                        e instanceof ParseException
                                                && exceptionMessages.add(e.getMessage()))
                        .collect(Collectors.toList());
        if (!exceptions.isEmpty()) {
            throw new JorjeCompilationException(
                    exceptions.stream()
                            .map(
                                    e ->
                                            String.format(
                                                    UserFacingMessages.CompilationErrors
                                                            .INVALID_SYNTAX_TEMPLATE,
                                                    e.getLoc().getLine(),
                                                    e.getLoc().getColumn(),
                                                    e.getError()))
                            .collect(Collectors.joining("\n")));
        }

        // Wrap the top level Jorje node in a AstNodeWrapper and build a new tree of AstNodeWrappers
        final AstNode node = codeUnit.getNode();
        final AstNodeWrapper<?> wrapper = AstNodeWrapperFactory.getVertex(node, null);
        final TreeBuilderVisitor visitor = new TreeBuilderVisitor(wrapper);
        node.traverse(visitor, NoopScope.get());

        // Walk the tree converting comments to EngineDirectives
        NavigableMap<Integer, HiddenToken> hiddenTokenMap = codeUnit.getHiddenTokenMap();
        if (!hiddenTokenMap.isEmpty()) {
            CommentVisitor commentVisitor = new CommentVisitor(hiddenTokenMap);
            visitComments(wrapper, commentVisitor);
        }

        // Assign positional information to each onde
        PositionInformation positionInformation = new PositionInformation(sourceCode);
        setMetaInformation(wrapper, positionInformation);

        return wrapper;
    }

    /** Add additional information such as child index, source code position */
    private static void setMetaInformation(
            JorjeNode wrapper, PositionInformation positionInformation) {
        wrapper.accept(
                new JorjeNodeVisitor() {
                    @Override
                    public void defaultVisit(AstNodeWrapper<?> wrapper) {
                        wrapper.setMetaInformation(positionInformation);
                    }

                    @Override
                    public void defaultVisit(JorjeNode jorjeNode) {
                        jorjeNode.setMetaInformation(positionInformation);
                    }
                });
        for (JorjeNode child : wrapper.getChildren()) {
            setMetaInformation(child, positionInformation);
        }
    }

    private static void visitComments(JorjeNode wrapper, CommentVisitor visitor) {
        wrapper.accept(visitor);
        for (JorjeNode child : wrapper.getChildren()) {
            visitComments(child, visitor);
        }
    }

    public static final class JorjeCompilationException extends SfgeRuntimeException {
        JorjeCompilationException(String message) {
            super(message);
        }
    }

    /** Increments log level of BaseApexLexer class from jorje jar to avoid printing info logs. */
    private static void incrementLogLevel() {
        Logger jorjeLogger = Logger.getLogger(BaseApexLexer.class.getName());
        jorjeLogger.setLevel(Level.WARNING);
    }

    private JorjeUtil() {}
}
