package com.salesforce.graph.source.supplier;

import com.salesforce.collections.CollectionUtil;
import com.salesforce.metainfo.MetaInfoCollectorTestProvider;
import com.salesforce.metainfo.VisualForceHandlerImpl;
import java.util.List;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;

public class ExposedControllerMethodSupplierTest extends BaseSourceSupplierTest {

    /** Public/global methods on known controllers should be covered by this supplier. */
    @Test
    public void supplierLoadsControllerMethods() {
        // spotless:off
        String sourceCode =
            "global class ApexControllerClass {\n"
            // Give the controller a public method.
          + "    public String getPublicString() {\n"
          + "        return 'beep';\n"
          + "    }\n"
            // Give the controller a global method.
          + "    global String getGlobalString() {\n"
          + "        return 'beep';\n"
          + "    }\n"
            // Give the controller a private method.
          + "    private String getPrivateString() {\n"
          + "        return 'beep';\n"
          + "    }\n"
          + "}\n";
        // spotless:on

        try {
            MetaInfoCollectorTestProvider.setVisualForceHandler(
                    new VisualForceHandlerImpl() {
                        private TreeSet<String> references =
                                new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

                        @Override
                        public void loadProjectFiles(List<String> sourceFolders) {
                            // Intentional no-op
                        }

                        @Override
                        public TreeSet<String> getMetaInfoCollected() {
                            references.add("ApexControllerClass");
                            return references;
                        }
                    });
            testSupplier_positive(
                    new String[] {sourceCode},
                    new ExposedControllerMethodSupplier(),
                    CollectionUtil.newTreeSetOf(
                            "ApexControllerClass#getPublicString@2",
                            "ApexControllerClass#getGlobalString@5",
                            // TODO: The synthetic `clone` method is included. This may not be the
                            // behavior we want.
                            //       If we change our mind, the test should change.
                            "ApexControllerClass#clone@1"));
        } finally {
            MetaInfoCollectorTestProvider.removeVisualForceHandler();
        }
    }
}
