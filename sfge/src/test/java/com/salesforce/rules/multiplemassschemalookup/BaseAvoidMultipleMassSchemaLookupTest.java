package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.rules.AvoidMultipleMassSchemaLookups;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;

/** Base class to tests for {@link AvoidMultipleMassSchemaLookups} */
public abstract class BaseAvoidMultipleMassSchemaLookupTest extends BasePathBasedRuleTest {

    protected static final String MY_CLASS = "MyClass";

    protected static final AvoidMultipleMassSchemaLookups RULE =
            AvoidMultipleMassSchemaLookups.getInstance();

    protected ViolationWrapper.MassSchemaLookupInfoBuilder expect(
            int sinkLine, String sinkMethodName, MmslrUtil.RepetitionType type) {
        return ViolationWrapper.MassSchemaLookupInfoBuilder.get(sinkLine, sinkMethodName, type);
    }
}
