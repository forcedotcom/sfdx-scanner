package com.salesforce.graph.source.supplier;

import com.salesforce.collections.CollectionUtil;
import org.junit.jupiter.api.Test;

public class GlobalMethodSupplierTest extends BaseSourceSupplierTest {

    /** Non-test global-scoped methods should be returned by this supplier. */
    @Test
    public void supplierLoadsGlobalMethods() {
        // spotless:off
        String[] sourceCodes = {
            "global class Foo {\n"
            // Add a global static method.
          + "    global static boolean globStatBool() {\n"
          + "        return true;\n"
          + "    }\n"
            // Add a global instance method.
          + "    global boolean globInstBool() {\n"
          + "        return true;\n"
          + "    }\n"
            // Add a public static method.
          + "    public static boolean pubStatBool() {\n"
          + "        return true;\n"
          + "    }\n"
            // Add a public instance method.
          + "    public boolean pubInstBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}\n",
            "@IsTest\n"
          + "global class TestFoo {\n"
          + "    @IsTest\n"
          + "    global testMethod boolean getBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}\n"
        };
        // spotless:on

        testSupplier_positive(
                sourceCodes,
                new GlobalMethodSupplier(),
                CollectionUtil.newTreeSetOf("Foo#globStatBool@2", "Foo#globInstBool@5"));
    }
}
