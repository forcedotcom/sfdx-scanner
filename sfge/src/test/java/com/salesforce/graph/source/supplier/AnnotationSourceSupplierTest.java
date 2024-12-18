package com.salesforce.graph.source.supplier;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.graph.Schema;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Annotation suppliers generally work the same, so they can all be put into a single file with
 * parameterized tests.
 */
public class AnnotationSourceSupplierTest extends BaseSourceSupplierTest {
    // spotless:off
    private static final String TEMPLATE =
        "public class Foo {\n"
        // Add a method with a configurable annotation.
      + "    @%s\n"
      + "    public boolean annotatedMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "    \n"
      + "    @%s\n"
      + "    public boolean simpleProp {\n"
      + "        get;\n"
      + "        set;\n"
      + "    }\n"
      + "    \n"
      + "    @%s\n"
      + "    public integer complicatedProp {\n"
      + "        get { return complicatedProp; }\n"
      + "        set { complicatedProp = value; }\n"
      + "    }\n"
      + "    \n"
      + "    @%s\n"
      + "    public void bodilessMethod() {}\n"
      + "    \n"
        // Add a method without an annotation.
      + "    public boolean nonAnnotatedMethod() {\n"
      + "        return true;\n"
      + "    }\n"
      + "}\n";
    // spotless:on

    /**
     * Suppliers that check for a given annotation should return methods that are appropriately
     * annotated.
     */
    @MethodSource("supplierLoadsAnnotatedMethods_params")
    @ParameterizedTest(name = "{displayName}: Annotation @{0}")
    public void supplierLoadsAnnotatedMethods(
            String annotation, AbstractAnnotationSourceSupplier supplier) {
        String sourceCode = String.format(TEMPLATE, annotation, annotation, annotation, annotation);
        testSupplier_positive(
                new String[] {sourceCode},
                supplier,
                CollectionUtil.newTreeSetOf("Foo#annotatedMethod@3"));
    }

    private static Stream<Arguments> supplierLoadsAnnotatedMethods_params() {
        return Stream.of(
                Arguments.of(Schema.AURA_ENABLED, new AnnotationAuraEnabledSupplier()),
                Arguments.of(Schema.INVOCABLE_METHOD, new AnnotationInvocableMethodSupplier()),
                Arguments.of(
                        Schema.NAMESPACE_ACCESSIBLE, new AnnotationNamespaceAccessibleSupplier()),
                Arguments.of(Schema.REMOTE_ACTION, new AnnotationRemoteActionSupplier()));
    }
}
