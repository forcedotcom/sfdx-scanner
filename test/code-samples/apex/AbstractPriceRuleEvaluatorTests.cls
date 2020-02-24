@isTest
public class AbstractPriceRuleEvaluatorTests {
  // -----------------
  // --- TEST DATA ---
  // -----------------
  private static final MockPriceRuleEvaluator INSTANCE = new MockPriceRuleEvaluator();
  private static final MockPriceRule RULE = new MockPriceRule();
  private static final List<AbstractPriceRuleEvaluator.PriceConditionGroup> CONDITION_GROUPS = INSTANCE.getPriceConditionGroups(RULE);
  private static final List<SObject> TARGETS = new List<SObject>{ new QuoteLine__c(), new QuoteLine__c() };

  // ----------------------------
  // --- evaluateConditions() ---
  // ----------------------------
  @isTest
  private static void evaluateConditions_returns_early_if_conditions_met_all_and_failed_check() {
    // setup
    INSTANCE.setEvaluateConditionReturnValue(false); // fail condition check
    RULE.setResultAllowsShortCircuit(true); // conditions met = all

    // sanity check
    System.assert(CONDITION_GROUPS[0].getConditions().size() > 0);

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr =
      INSTANCE.evaluateConditions(RULE, CONDITION_GROUPS[0], TARGETS, null);

    // assert
    System.assertNotEquals(null, pcr, 'Expected a Price Condition Result object.');
    System.assertEquals(false, pcr.result, 'Expected Price Condition to fail.');
    System.assert(pcr.lines.size() > 0);
  }

  @isTest
  private static void evaluateConditions_returns_null_and_sets_advancedConditionMap_if_all_conditions_met() {
    // setup
    INSTANCE.setEvaluateConditionReturnValue(true); // pass condition checks
    RULE.setResultAllowsShortCircuit(false); // conditions met = any
    Map<String, Boolean> advancedConditionMap = new Map<String, Boolean>();

    // sanity check
    System.assert(CONDITION_GROUPS[0].getConditions().size() > 0);

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult result =
      INSTANCE.evaluateConditions(RULE, CONDITION_GROUPS[0], TARGETS, advancedConditionMap);

    // assert
    System.assertEquals(null, result, 'Expected null to be returned.');
    System.assert(advancedConditionMap.keyset().size() > 0, 'Expected advancedConditionMap to be populated.');
    for (Boolean resultValue : advancedConditionMap.values()) {
      System.assert(true,	resultValue);
    }
  }

  @isTest
  private static void evaluateConditions_returns_null_for_an_empty_set_of_price_conditions() {
    // setup
    Map<String, Boolean> advancedConditionMap = new Map<String, Boolean>();
    AbstractPriceRuleEvaluator.PriceConditionGroup emptyConditions =
      new AbstractPriceRuleEvaluator.PriceConditionGroup(new List<AbstractPriceRuleEvaluator.PriceRuleCondition>(), null);

    // sanity check
    System.assert(emptyConditions.getConditions().isEmpty());

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult result =
      INSTANCE.evaluateConditions(RULE, emptyConditions, TARGETS, advancedConditionMap);

    // assert
    System.assertEquals(null, result, 'Expected null to be returned.');
    System.assert(advancedConditionMap.keyset().isEmpty(), 'Expected advancedConditionMap to be empty.');
  }

  // ---------------------------------
  // --- getPriceConditionGroups() ---
  // ---------------------------------
  @isTest
  private static void getPriceConditionGroups_returns_configuration_attribute_conditions_for_configurator_price_rules() {
    // assert
    System.assert(INSTANCE.getPriceConditionGroups(RULE).size() == 3);
  }

  // ------------------------------
  // --- isPriceRuleSatisfied() ---
  // ------------------------------
  @isTest
  private static void isPriceRuleSatisfied_returns_true_for_empty_set_of_conditions() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditions(new List<AbstractPriceRuleEvaluator.PriceRuleCondition>());

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr = INSTANCE.isPriceRuleSatisfied(rule, TARGETS);

    // assert
    System.assertEquals(true, pcr.result);
    System.assertEquals(TARGETS, pcr.lines);
  }

  @isTest
  private static void isPriceRuleSatisfied_returns_early_if_conditions_met_is_all_and_not_satisfied() {
    // setup
    INSTANCE.setEvaluateConditionReturnValue(false); // fail condition check
    RULE.setResultAllowsShortCircuit(true); // conditions met = all

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr = INSTANCE.isPriceRuleSatisfied(RULE, TARGETS);

    // assert
    System.assertEquals(false, pcr.result);
    System.assertEquals(TARGETS, pcr.lines);
  }

  @isTest
  private static void isPriceRuleSatisfied_returns_early_if_conditions_met_is_any_and_is_satisfied() {
    // setup
    INSTANCE.setEvaluateConditionReturnValue(true); // pass condition check
    RULE.setResultAllowsShortCircuit(true); // conditions met = any

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr = INSTANCE.isPriceRuleSatisfied(RULE, TARGETS);

    // assert
    System.assertEquals(true, pcr.result);
    System.assertEquals(TARGETS, pcr.lines);
  }

  @isTest
  private static void isPriceRuleSatisfied_returns_true_if_conditions_met_is_all_and_is_satisfied() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('All');
    INSTANCE.setEvaluateConditionReturnValue(true); // pass all condition checks

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr = INSTANCE.isPriceRuleSatisfied(rule, TARGETS);

    // assert
    System.assertEquals(true, pcr.result);
    System.assertEquals(TARGETS, pcr.lines);
  }

  @isTest
  private static void isPriceRuleSatisfied_returns_false_if_conditions_met_is_any_and_is_not_satisfied() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Any');
    INSTANCE.setEvaluateConditionReturnValue(false); // fail all condition checks

    // execute
    AbstractPriceRuleEvaluator.PriceConditionResult pcr = INSTANCE.isPriceRuleSatisfied(rule, TARGETS);

    // assert
    System.assertEquals(false, pcr.result);
    System.assertEquals(TARGETS, pcr.lines);
  }

  // ----------------------------------------
  // --- PriceRule.isConfiguratorScoped() ---
  // ----------------------------------------
  @isTest
  private static void isConfiguratorScoped_returns_true_for_configurator_scoped_price_rules() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setRuleScope('Configurator');

    // execute and assert
    System.assert(rule.isConfiguratorScoped());
  }

  @isTest
  private static void isConfiguratorScoped_returns_false_for_non_configurator_scoped_price_rules() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setRuleScope('Calculator');

    // execute && assert
    System.assert(!rule.isConfiguratorScoped());

    // setup 2
    rule.setRuleScope('Quote Line');

    // execute && assert 2
    System.assert(!rule.isConfiguratorScoped());
  }

  // ----------------------------------
  // --- PriceRule.handleLateExit() ---
  // ----------------------------------
  @isTest
  private static void handleLateExit_returns_true_if_conditions_met_is_all() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('All');

    // execute and assert
    System.assert(rule.handleLateExit(null));
  }

  @isTest
  private static void handleLateExit_returns_false_if_conditions_met_is_any() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Any');

    // execute and assert
    System.assert(!rule.handleLateExit(null));
  }

  @isTest
  private static void handleLateExit_returns_false_if_advancedConditionMap_is_empty() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Custom');

    // execute and assert
    System.assert(!rule.handleLateExit(new Map<String, Boolean>()));
  }

  // --------------------------------------------
  // --- PriceRule.resultAllowsShortCircuit() ---
  // --------------------------------------------
  @isTest
  private static void resultAllowsShortCircuit_returns_true_if_conditions_met_all_and_falsey_result() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('All');

    // execute and assert
    System.assert(rule.resultAllowsShortCircuit(false));
  }

  @isTest
  private static void resultAllowsShortCircuit_returns_false_if_conditions_met_all_and_truthy_result() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('All');

    // execute and assert
    System.assert(!rule.resultAllowsShortCircuit(true));
  }

  @isTest
  private static void resultAllowsShortCircuit_returns_true_if_conditions_met_any_and_truthy_result() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Any');

    // execute and assert
    System.assert(rule.resultAllowsShortCircuit(true));
  }

  @isTest
  private static void resultAllowsShortCircuit_returns_false_if_conditions_met_any_and_falsey_result() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Any');

    // execute and assert
    System.assert(!rule.resultAllowsShortCircuit(false));
  }

  @isTest
  private static void resultAllowsShortCircuit_returns_false_if_conditions_met_custom() {
    // setup
    MockPriceRule rule = new MockPriceRule();
    rule.setConditionsMet('Custom');

    // execute and assert
    System.assert(!rule.resultAllowsShortCircuit(false));
    System.assert(!rule.resultAllowsShortCircuit(true));
  }

  // -------------------------------------
  // --- PriceRule.addPriceCondition() ---
  // -------------------------------------
  @isTest
  private static void addPriceCondition_appends_quote_conditions_to_quote_array() {
    // setup
    MockPriceRule rule = new MockPriceRule();
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
      return new List<AbstractPriceRuleEvaluator.PriceRuleCondition> {
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
    public MockPriceRuleEvaluator() { super(); }
    public override PriceRule__c[] getPriceRules() { return null; }
    public override SObject[] getTargetRecords() { return null; }
  }
}
