@isTest
public class BadSyntax1 {
	// -----------------
	// --- TEST DATA ---
	// -----------------
	private static final MockPriceRuleEvaluator INSTANCE = new MockPriceRuleEvaluator();
	private static final MockPriceRule RULE = new MockPriceRule();
	private static final List<AbstractPriceRuleEvaluator.PriceConditionGroup> CONDITION_GROUPS = INSTANCE.getPriceConditionGroups(RULE);
	private static final List<SObject> TARGETS = new List<SObject>{
		new QuoteLine__c(), new QuoteLine__c()
	};

	// -------------------------------------
	// --- PriceRule.addPriceCondition() ---
	// -------------------------------------
	@isTest
	private static void addPriceCondition_appends_quote_conditions_to_quote_array() {
		// setup
		// NOTE: We've removed the close-paren from the following line, which will cause a syntax error when evaluating rules
		// against this file.
		MockPriceRule rule = new MockPriceRule(;
		rule.conditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.quoteConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();

		AbstractPriceRuleEvaluator.PriceRuleCondition prc = new AbstractPriceRuleEvaluator.PriceRuleCondition();
		prc.testedField = 'FooBar';
		prc.testedObject = 'Quote';

		// sanity check
		System.assert(rule.conditions.isEmpty());
		System.assert(rule.quoteConditions.isEmpty());

		// execute
		rule.addPriceCondition(prc);

		// assert
		System.assertEquals(1, rule.conditions.size());
		System.assertEquals(rule.conditions[0], prc);
		System.assertEquals(rule.quoteConditions[0], prc);
	}

	@isTest
	private static void addPriceCondition_appends_configuration_attribute_conditions_to_configuration_attribute_array() {
		// setup
		MockPriceRule rule = new MockPriceRule();
		rule.conditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.configAttrConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.setRuleScope('Configurator'); // proper configuration

		AbstractPriceRuleEvaluator.PriceRuleCondition prc = new AbstractPriceRuleEvaluator.PriceRuleCondition();
		prc.testedField = 'FooBar';
		prc.testedObject = 'Configuration Attribute';

		// sanity check
		System.assert(rule.conditions.isEmpty());
		System.assert(rule.configAttrConditions.isEmpty());

		// execute
		rule.addPriceCondition(prc);

		// assert
		System.assertEquals(1, rule.conditions.size());
		System.assertEquals(rule.conditions[0], prc);
		System.assertEquals(rule.configAttrConditions[0], prc);
	}

	@isTest
	private static void addPriceCondition_throws_error_when_configuration_attribute_conditions_are_in_calculator_price_rules() {
		// setup
		MockPriceRule rule = new MockPriceRule();
		rule.conditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.configAttrConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.setRuleScope('Calculator'); // a misconfiguration

		AbstractPriceRuleEvaluator.PriceRuleCondition prc = new AbstractPriceRuleEvaluator.PriceRuleCondition();
		prc.testedField = 'FooBar';
		prc.testedObject = 'Configuration Attribute';

		// sanity check
		System.assert(rule.conditions.isEmpty());
		System.assert(rule.configAttrConditions.isEmpty());

		// execute
		try {
			rule.addPriceCondition(prc);
			System.assert(false, System.Label.msg_no_config_attr_calc_rule);
		} catch (Exception e) {
			System.assertEquals(System.Label.msg_no_config_attr_calc_rule, e.getMessage());
		}
	}

	@isTest
	private static void addPriceCondition_appends_line_item_conditions_to_line_item_array() {
		// setup
		MockPriceRule rule = new MockPriceRule();
		rule.conditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.lineItemConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();

		AbstractPriceRuleEvaluator.PriceRuleCondition prc = new AbstractPriceRuleEvaluator.PriceRuleCondition();
		prc.testedField = 'FooBar';
		prc.testedObject = 'Quote Line';

		// sanity check
		System.assert(rule.conditions.isEmpty());
		System.assert(rule.lineItemConditions.isEmpty());

		// execute
		rule.addPriceCondition(prc);

		// assert
		System.assertEquals(1, rule.conditions.size());
		System.assertEquals(rule.conditions[0], prc);
		System.assertEquals(rule.lineItemConditions[0], prc);
	}

	@isTest
	private static void addPriceCondition_appends_summary_variable_conditions_to_summary_variable_array() {
		// setup
		MockPriceRule rule = new MockPriceRule();
		rule.conditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		rule.sumVarConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();

		AbstractPriceRuleEvaluator.PriceRuleCondition prc = new AbstractPriceRuleEvaluator.PriceRuleCondition();
		prc.testedVariableId = 'a0c17000002AkcKAAS';

		// sanity check
		System.assert(rule.conditions.isEmpty());
		System.assert(rule.sumVarConditions.isEmpty());

		// execute
		rule.addPriceCondition(prc);

		// assert
		System.assertEquals(1, rule.conditions.size());
		System.assertEquals(rule.conditions[0], prc);
		System.assertEquals(rule.sumVarConditions[0], prc);
	}

	// ----------------------------
	// --- MOCK IMPLEMENTATIONS ---
	// ----------------------------
	public class MockPriceRule extends AbstractPriceRuleEvaluator.PriceRule {
		public MockPriceRule() {
			super();
			this.configAttrConditions = seedPriceRuleConditions();
			this.sumVarConditions = seedPriceRuleConditions();
			this.quoteConditions = seedPriceRuleConditions();
			this.conditions = seedPriceRuleConditions();

			// NOTE: We kept this an empty list because there's extremely
			// complex logic in isPriceRuleSatisfied() that needs valid Line Item
			// conditions setup. Could be a good future unit test to create if more
			// work is done here.
			this.lineItemConditions = new List<AbstractPriceRuleEvaluator.PriceRuleCondition>();
		}

		private List<AbstractPriceRuleEvaluator.PriceRuleCondition> seedPriceRuleConditions() {
			return new List<AbstractPriceRuleEvaluator.PriceRuleCondition>{
				new AbstractPriceRuleEvaluator.PriceRuleCondition(),
				new AbstractPriceRuleEvaluator.PriceRuleCondition()
			};
		}

		public void setConditions(List<AbstractPriceRuleEvaluator.PriceRuleCondition> conditions) {
			this.conditions = conditions;
		}

		private Boolean resultAllowsShortCircuitReturnValue;
		public void setResultAllowsShortCircuit(Boolean value) {
			this.resultAllowsShortCircuitReturnValue = value;
		}

		public override Boolean resultAllowsShortCircuit(Boolean result) {
			// if not mocked, call actual implementation
			if (resultAllowsShortCircuitReturnValue == null) {
				return super.resultAllowsShortCircuit(result);
			}
			return resultAllowsShortCircuitReturnValue;
		}

		public void setRuleScope(String scope) {
			this.targetObject = scope;
		}

		public void setConditionsMet(String conditionsMet) {
			this.conditionsMet = conditionsMet;
		}
	}

	public class MockPriceRuleEvaluator extends AbstractPriceRuleEvaluator {
		private Boolean evaluateConditionReturnValue = true;
		public void setEvaluateConditionReturnValue(Boolean value) {
			this.evaluateConditionReturnValue = value;
		}

		public override Boolean evaluateCondition(RuleCondition cond, SObject obj) {
			return this.evaluateConditionReturnValue;
		}

		// Required implementations
		public MockPriceRuleEvaluator() {
			super();
		}
		public override PriceRule__c[] getPriceRules() {
			return null;
		}
		public override SObject[] getTargetRecords() {
			return null;
		}
	}
}
