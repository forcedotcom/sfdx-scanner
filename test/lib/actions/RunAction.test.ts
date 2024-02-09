import {InputProcessor, InputProcessorImpl} from "../../../src/lib/InputProcessor";
import {RuleFilterFactoryImpl} from "../../../src/lib/RuleFilterFactory";
import {RunEngineOptionsFactory} from "../../../src/lib/EngineOptionsFactory";
import {RunAction} from "../../../src/lib/actions/RunAction";
import {FakeDisplay} from "../FakeDisplay";
import {Logger} from "@salesforce/core";
import {Inputs, PathlessRuleViolation, RuleResult} from "../../../src/types";
import * as path from "path";
import {initContainer} from '../../../src/ioc.config';
import {expect} from "chai";
import {Results} from "../../../src/lib/output/Results";
import {PMD6_VERSION, PMD7_VERSION} from "../../../src/Constants";
import {FakeResultsProcessorFactory, RawResultsProcessor} from "./fakes";
import {Controller} from "../../../lib/Controller";
import {Pmd6CommandInfo} from "../../../lib/lib/pmd/PmdCommandInfo";

const codeFixturesDir = path.join(__dirname, '..', '..', 'code-fixtures');
const pathToSomeTestClass = path.join(codeFixturesDir, 'apex', 'SomeTestClass.cls');
const pathToCodeForCpd = path.join(codeFixturesDir, 'cpd');

describe("Tests for RunAction", () => {
	let display: FakeDisplay;
	let resultsProcessor: RawResultsProcessor;
	let runAction: RunAction;
	before(() => {
		initContainer();
	});
	beforeEach(() => {
		display = new FakeDisplay();
		resultsProcessor = new RawResultsProcessor();

		const inputProcessor: InputProcessor = new InputProcessorImpl("2.11.8", display);
		runAction = new RunAction(
			Logger.childFromRoot("forTesting"),
			display,
			inputProcessor,
			new RuleFilterFactoryImpl(),
			new RunEngineOptionsFactory(inputProcessor),
			new FakeResultsProcessorFactory(resultsProcessor));
	});

	describe("Tests to confirm that PMD7 binary files are invoked when choosing PMD7", () => {
		afterEach(() => {
			// Until we remove global state, we should cleanup after ourselves to prevent other tests from being impacted
			Controller.setActivePmdCommandInfo(new Pmd6CommandInfo())
		})

		it("When using PMD7, the pmd engine actually uses PMD7 instead of PMD6", async () => {
			const inputs: Inputs = {
				target: [pathToSomeTestClass],
				engine: ['pmd'],
				'normalize-severity': true,
				"preview-pmd7": true
			}
			await runAction.run(inputs);

			const results: Results = resultsProcessor.getResults();
			expect(results.getExecutedEngines().size).to.equal(1);
			expect(results.getExecutedEngines()).to.contain('pmd');
			const ruleResults: RuleResult[] = results.getRuleResults();
			expect(ruleResults).to.have.length(1);
			expect(ruleResults[0].violations).to.have.length(8);
			for (let violation of ruleResults[0].violations) {
				violation = violation as PathlessRuleViolation;

				// Unfortunately, there isn't an easy way to detect that we are using PMD 7 binaries other than checking
				// that the violation urls contain version 7 information instead of version 6.
				expect(violation.url).to.contain(PMD7_VERSION);
				expect(violation.url).not.to.contain(PMD6_VERSION);

				// Other sanity checks to make the fields are filled in
				expect(violation.ruleName).to.have.length.greaterThan(0);
				expect(violation.category).to.have.length.greaterThan(0);
				expect(violation.line).to.be.greaterThan(0);
				expect(violation.column).to.be.greaterThan(0);
				expect(violation.message).to.have.length.greaterThan(0);
				expect(violation.severity).to.be.greaterThanOrEqual(3);
				expect(violation.normalizedSeverity).to.equal(3);
			}
		});

		it("When using PMD7, the cpd engine actually uses PMD7 instead of PMD6", async () => {
			const inputs: Inputs = {
				target: [pathToCodeForCpd],
				engine: ['cpd'],
				'normalize-severity': true,
				"preview-pmd7": true
			}
			await runAction.run(inputs);

			const results: Results = resultsProcessor.getResults();
			expect(results.getExecutedEngines().size).to.equal(1);
			expect(results.getExecutedEngines()).to.contain('cpd');
			const ruleResults: RuleResult[] = results.getRuleResults();
			expect(ruleResults).to.have.length(2);
			expect(ruleResults[0].violations).to.have.length(1);
			expect(ruleResults[1].violations).to.have.length(1);
			const violation1: PathlessRuleViolation = ruleResults[0].violations[0] as PathlessRuleViolation;
			const violation2: PathlessRuleViolation = ruleResults[1].violations[0] as PathlessRuleViolation;

			for (let violation of [violation1, violation2]) {

				// Unfortunately, there isn't an easy way to detect that we are using PMD 7 binaries.
				// The best we can do is check for 'latest' in the url.
				expect(violation.url).to.contain('latest');
				expect(violation.url).not.to.contain(PMD6_VERSION);

				// Other sanity checks to make the fields are filled in
				expect(violation.ruleName).to.have.length.greaterThan(0);
				expect(violation.category).to.have.length.greaterThan(0);
				expect(violation.line).to.be.greaterThan(0);
				expect(violation.column).to.be.greaterThan(0);
				expect(violation.message).to.have.length.greaterThan(0);
				expect(violation.severity).to.be.greaterThanOrEqual(3);
				expect(violation.normalizedSeverity).to.equal(3);
			}
		});
	});
});
