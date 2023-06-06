package com.salesforce.graph.source.supplier;

import com.salesforce.collections.CollectionUtil;
import org.junit.jupiter.api.Test;

public class PageReferenceSupplierTest extends BaseSourceSupplierTest {

    /** Non-test methods that return page references should be returned by this supplier. */
    @Test
    public void supplierLoadsPageReferenceMethods() {
        // spotless:off
        String[] sourceCodes = {
            "public class MyClass {\n"
          + "    public static PageReference staticPageRef() {\n"
          + "        return null;\n"
          + "    }\n"
          + "    \n"
          + "    public PageReference instancePageRef() {\n"
          + "        return null;\n"
          + "    }\n"
          + "    public static boolean staticBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "    public boolean instanceBool() {\n"
          + "        return true;\n"
          + "    }\n"
          + "}\n",
            // Also add a test class with test methods.
            // The test methods should NOT be included by the supplier.
            "@IsTest\n"
          + "public class MyTestClass {\n"
          + "    @IsTest\n"
          + "    public static PageReference staticPageRef() {\n"
          + "        return null;\n"
          + "    }\n"
          + "    \n"
          + "    public static testMethod PageReference testPageRef() {\n"
          + "        return null;\n"
          + "    }\n"
          + "}\n"
        };
        // spotless:on

        testSupplier_positive(
                sourceCodes,
                new PageReferenceSupplier(),
                CollectionUtil.newTreeSetOf(
                        "MyClass#staticPageRef@2", "MyClass#instancePageRef@6"));
    }
}
