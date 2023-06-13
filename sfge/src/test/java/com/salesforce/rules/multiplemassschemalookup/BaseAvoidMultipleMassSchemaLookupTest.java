package com.salesforce.rules.multiplemassschemalookup;

import com.salesforce.rules.MultipleMassSchemaLookupRule;
import com.salesforce.testutils.BasePathBasedRuleTest;
import com.salesforce.testutils.ViolationWrapper;

/** Base class to tests for {@link MultipleMassSchemaLookupRule} */
public abstract class BaseAvoidMultipleMassSchemaLookupTest extends BasePathBasedRuleTest {

    protected static final MultipleMassSchemaLookupRule RULE =
            MultipleMassSchemaLookupRule.getInstance();

    protected ViolationWrapper.MassSchemaLookupInfoBuilder expect(
            int sinkLine,
            String sinkMethodName,
            int occurrenceLine,
            String occurrenceClassName,
            MmslrUtil.RepetitionType type,
            String typeInfo) {
        return ViolationWrapper.MassSchemaLookupInfoBuilder.get(
                sinkLine, sinkMethodName, occurrenceLine, occurrenceClassName, type, typeInfo);
    }
}
