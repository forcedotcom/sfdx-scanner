package com.salesforce.graph.source.supplier;

import com.salesforce.collections.CollectionUtil;
import org.junit.jupiter.api.Test;

public class InboundEmailHandlerSupplierTest extends BaseSourceSupplierTest {

    /**
     * Implementations of the {@code InboundEmailHandler#handleInboundEmail} method should be
     * returned by this supplier.
     */
    @Test
    public void supplierLoadsInboundEmailHandlerImplementations() {
        // spotless:off
        String[] sourceCodes = {
            // Add a class that implements the interface.
            "public class MyClass implements Messaging.InboundEmailHandler {\n"
            // Implement the handleInboundEmail method as per the interface.
          + "    public Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {\n"
          + "        return null;\n"
          + "    }\n"
            // Add an overload of the handleInboundEmail method that takes different arguments.
          + "    public Messaging.InboundEmailResult handleInboundEmail(boolean b) {\n"
          + "        return null;\n"
          + "    }\n"
            // Add a method with some other name entirely.
          + "    public boolean someSecondaryMethod() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}\n",
            // Add a class that has a method with the expected signature, but doesn't
            // actually implement the interface. It should not be included.
            "public class MyOtherClass {\n"
          + "    public Messaging.InboundEmailResult handleInboundEmail(Messaging.InboundEmail email, Messaging.InboundEnvelope envelope) {\n"
          + "        return null;\n"
          + "    }\n"
          + "}\n"
        };
        // spotless:on

        testSupplier_positive(
                sourceCodes,
                new InboundEmailHandlerSupplier(),
                CollectionUtil.newTreeSetOf("MyClass#handleInboundEmail@2"));
    }
}
