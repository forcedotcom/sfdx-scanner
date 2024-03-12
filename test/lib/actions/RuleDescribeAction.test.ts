import {AnyJson} from "@salesforce/ts-types";
import {expect} from "chai";

import {FakeDisplay} from "../FakeDisplay";
import {initializeTestSetup} from "../../test-related-lib/TestOverrides";

import {RuleFilterFactoryImpl} from "../../../src/lib/RuleFilterFactory";
import {Inputs} from "../../../src/types";
import {RuleDescribeAction} from "../../../src/lib/actions/RuleDescribeAction";
import {Pmd6CommandInfo} from "../../../src/lib/pmd/PmdCommandInfo";
import {Controller} from "../../../src/Controller";
import {getMessage, BundleName} from '../../../src/MessageCatalog';

describe("RuleDescribeAction", () => {
	let display: FakeDisplay;
	let ruleDescribeAction: RuleDescribeAction;
	beforeEach(() => {
		// TODO: Add some functional tests for RuleManager.getRulesMatchingOnlyExplicitCriteria(),
		//       then switch this from initializing the controller to using mock-ups, so we're only
		//       testing the actual code here and not the underlying implementation.
		initializeTestSetup();
		display = new FakeDisplay();
		ruleDescribeAction = new RuleDescribeAction(display, new RuleFilterFactoryImpl());
	});

	describe('#run', () => {
		describe('When no rules match...', () => {
			it('Shows warning and returns nothing', async () => {
				const inputs: Inputs = {
					rulename: 'definitely-fake-rule'
				};

				const output = await ruleDescribeAction.run(inputs);

				expect(display.getOutputArray()).to.contain(`[Warning]: ${getMessage(BundleName.Describe, 'output.noMatchingRules', [inputs.rulename])}`);
				expect(output).to.deep.equal([]);
			});
		});

		describe('When one rule matches...', () => {
			it('Rule is displayed and returned', async () => {
				const inputs: Inputs = {
					rulename: 'TooManyFields'
				};

				const output = await ruleDescribeAction.run(inputs);

				// Test the returned value (used by --json flag)
				expect(output).to.have.lengthOf(1, 'Wrong number of rules returned');
				expect(output[0]['name']).to.equal(inputs.rulename, 'Wrong name in returned output');
				expect(output[0]['engine']).to.equal('pmd', 'Wrong engine in returned output');
				expect(output[0]['enabled']).to.equal(true, 'Wrong enablement status in returned output');
				expect(output[0]['categories']).to.deep.equal(['Design'], 'Wrong categories in returned output');
				expect(output[0]['languages']).to.deep.equal(['apex'], 'Wrong language in returned output');
				expect(output[0]['message']).to.equal('Too many fields', 'Wrong message in returned output');

				// Test the displayed value
				const displayedRule: AnyJson = display.getLastStyledObject();
				expect(displayedRule['name']).to.equal(inputs.rulename, 'Wrong name in displayed output');
				expect(displayedRule['engine']).to.equal('pmd', 'Wrong engine in displayed output');
				expect(displayedRule['enabled']).to.equal(true, 'Wrong enablement status in displayed output');
				expect(displayedRule['categories']).to.deep.equal(['Design'], 'Wrong categories in displayed output');
				expect(displayedRule['languages']).to.deep.equal(['apex'], 'Wrong language in displayed output');
				expect(displayedRule['message']).to.equal('Too many fields', 'Wrong message in displayed output');

			});
		});

		describe('When multiple rules match...', () => {
			it('Shows warning; displays and returns rules', async () => {
				const inputs: Inputs = {
					rulename: 'constructor-super'
				};

				const output = await ruleDescribeAction.run(inputs);

				expect(display.getOutputArray()).to.contain(`[Warning]: ${getMessage(BundleName.Describe,'output.multipleMatchingRules', [3, inputs.rulename])}`)

				// Multiple rules should have been returned
				expect(output).to.have.lengthOf(3, 'Wrong number of rules returned');
				// Check some properties on each rule
				for (let i = 0; i < 3; i++) {
					expect(output[i]['name']).to.equal(inputs.rulename, `Wrong name in returned output for rule #${i + 1}`);
					expect(output[i]['engine']).to.contain('eslint', `Wrong engine in returned output for rule #${i + 1}`);
					expect(output[i]['categories']).to.deep.equal(['problem'], `Wrong categories in returned output for rule #${i + 1}`);
					expect(output[i]['description']).to.equal('Require `super()` calls in constructors', `Wrong description in returned output for rule #${i + 1}`);
				}

				// Just check the last displayed rule, it should be fine.
				const displayedRule = display.getLastStyledObject();
				expect(displayedRule['name']).to.equal(inputs.rulename, 'Wrong name in displayed output');
				expect(displayedRule['engine']).to.equal('eslint-typescript', 'Wrong engine in displayed output');
				expect(displayedRule['categories']).to.deep.equal(['problem'], 'Wrong categories in displayed output');
				expect(displayedRule['languages']).to.deep.equal(['typescript'], 'Wrong language in displayed output');
				expect(displayedRule['description']).to.equal('Require `super()` calls in constructors', 'Wrong description in displayed output');
			});
		});

		describe('When PMD7 binary is invoked...', () => {
			afterEach(() => {
				// Until we remove global state, we should clean up after ourselves to prevent other tests from being impacted
				Controller.setActivePmdCommandInfo(new Pmd6CommandInfo());
			})

			it('PMD7 pmd rule is returned', async () => {
				const inputs: Inputs = {
					rulename: 'ApexCRUDViolation',
					"preview-pmd7": true
				}
				await ruleDescribeAction.run(inputs);

				const rule: AnyJson  = display.getLastStyledObject();
				expect(rule['name']).to.equal('ApexCRUDViolation');
				expect(rule['engine']).to.equal('pmd');
				expect(rule['isPilot']).to.equal(false);
				expect(rule['enabled']).to.equal(true);
				expect(rule['categories']).to.deep.equal(['Security']);
				expect(rule['rulesets']).to.contain('quickstart');
				expect(rule['languages']).to.deep.equal(['apex']);
				expect(rule['description']).to.have.length.greaterThan(0);
				expect(rule['message']).to.have.length.greaterThan(0);
			});

			it('PMD7 cpd rule is returned', async () => {
				const inputs: Inputs = {
					rulename: 'copy-paste-detected',
					"preview-pmd7": true
				};
				await ruleDescribeAction.run(inputs);

				const rule: AnyJson  = display.getLastStyledObject();
				expect(rule['name']).to.equal('copy-paste-detected');
				expect(rule['engine']).to.equal('cpd');
				expect(rule['isPilot']).to.equal(false);
				expect(rule['enabled']).to.equal(false);
				expect(rule['categories']).to.deep.equal(['Copy/Paste Detected']);
				expect(rule['rulesets']).to.deep.equal([]);
				expect(rule['languages']).to.deep.equal(['apex', 'java', 'visualforce', 'xml']);
				expect(rule['description']).to.have.length.greaterThan(0);
			});
		});
	});
});
