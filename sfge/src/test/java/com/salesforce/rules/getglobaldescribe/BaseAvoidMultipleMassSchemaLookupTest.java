package com.salesforce.rules.getglobaldescribe;

import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;

/** Base class to tests for {@link com.salesforce.rules.AvoidMultipleMassSchemaLookup} */
public abstract class BaseAvoidMultipleMassSchemaLookupTest extends BasePathBasedRuleTest {

    protected ViolationWrapper.MassSchemaLookupInfoBuilder expect(
            int sinkLine,
            String sinkMethodName,
            int occurrenceLine,
            String occurrenceClassName,
            RuleConstants.RepetitionType type,
            String typeInfo) {
        return ViolationWrapper.MassSchemaLookupInfoBuilder.get(
                sinkLine, sinkMethodName, occurrenceLine, occurrenceClassName, type, typeInfo);
    }
}
