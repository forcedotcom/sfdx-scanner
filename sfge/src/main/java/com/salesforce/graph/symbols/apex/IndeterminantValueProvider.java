package com.salesforce.graph.symbols.apex;

import com.salesforce.graph.ops.ApexStandardLibraryUtil;
import com.salesforce.graph.vertex.SyntheticTypedVertex;
import java.util.function.Supplier;

/**
 * Generic providers to return indeterminant values. Allows easy mapping from method name to
 * provider
 */
@SuppressWarnings("PMD.UnusedPrivateField") // builder is used by the supplier
public class IndeterminantValueProvider<T extends ApexValue<T>> {
    private final ApexValueBuilder builder;
    private final Supplier<T> supplier;

    protected IndeterminantValueProvider(ApexValueBuilder builder, Supplier<T> supplier) {
        this.builder = builder.withStatus(ValueStatus.INDETERMINANT);
        this.supplier = supplier;
    }

    public T get() {
        return supplier.get();
    }

    public static class BooleanValueProvider extends IndeterminantValueProvider<ApexBooleanValue> {
        private BooleanValueProvider(ApexValueBuilder builder) {
            super(builder, builder::buildBoolean);
        }

        public static BooleanValueProvider get(ApexValueBuilder builder) {
            return new BooleanValueProvider(builder);
        }
    }

    public static class IntegerValueProvider extends IndeterminantValueProvider<ApexIntegerValue> {
        private IntegerValueProvider(ApexValueBuilder builder) {
            super(builder, builder::buildInteger);
        }

        public static IntegerValueProvider get(ApexValueBuilder builder) {
            return new IntegerValueProvider(builder);
        }
    }

    public static class ListValueProvider extends IndeterminantValueProvider<ApexListValue> {
        private ListValueProvider(ApexValueBuilder builder) {
            super(builder, builder::buildList);
        }

        public static ListValueProvider get(ApexValueBuilder builder) {
            return new ListValueProvider(builder);
        }

        public static ListValueProvider getStringList(ApexValueBuilder builder) {
            String type =
                    ApexStandardLibraryUtil.getListDeclaration(ApexStandardLibraryUtil.Type.STRING);
            builder.declarationVertex(SyntheticTypedVertex.get(type));
            return new ListValueProvider(builder);
        }
    }

    public static class SetValueProvider extends IndeterminantValueProvider<ApexSetValue> {
        private SetValueProvider(ApexValueBuilder builder) {
            super(builder, builder::buildSet);
        }

        public static SetValueProvider get(ApexValueBuilder builder) {
            return new SetValueProvider(builder);
        }

        public static SetValueProvider getStringSet(ApexValueBuilder builder) {
            String type =
                    ApexStandardLibraryUtil.getSetDeclaration(ApexStandardLibraryUtil.Type.STRING);
            builder.declarationVertex(SyntheticTypedVertex.get(type));
            return new SetValueProvider(builder);
        }
    }

    public static class StringValueProvider extends IndeterminantValueProvider<ApexStringValue> {
        private StringValueProvider(ApexValueBuilder builder) {
            super(builder, builder::buildString);
        }

        public static StringValueProvider get(ApexValueBuilder builder) {
            return new StringValueProvider(builder);
        }
    }
}
