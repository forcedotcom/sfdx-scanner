import {expect} from "chai";
import {Ux} from "@salesforce/sf-plugins-core";
import sinon = require('sinon');

import {FakeDisplay} from "../FakeDisplay";
import {initializeTestSetup} from "../../test-related-lib/TestOverrides";

import {RuleFilterFactoryImpl} from "../../../src/lib/RuleFilterFactory";
import {RuleListAction} from "../../../src/lib/actions/RuleListAction";
import {Inputs} from "../../../src/types";
import {ENGINE, PMD7_LIB} from "../../../src/Constants";
import {Controller} from "../../../src/Controller";
import {Pmd6CommandInfo} from "../../../src/lib/pmd/PmdCommandInfo";
import {Config} from '../../../src/lib/util/Config';

describe("Tests for RuleListAction", () => {
	let display: FakeDisplay;
	let ruleListAction: RuleListAction;

	beforeEach(() => {
		initializeTestSetup()
		display = new FakeDisplay();
		ruleListAction = new RuleListAction(display, new RuleFilterFactoryImpl());
	});

	describe('Filtering logic', () => {

		beforeEach(() => {
			sinon.stub(Config.prototype, 'isEngineEnabled').callThrough().withArgs(ENGINE.ESLINT_LWC).resolves(false);
		});

		afterEach(() => {
			sinon.restore();
		});

		it('Test Case: Without filters, all rules for enabled engines are returned', async () => {
			await ruleListAction.run([]);

			let tableData: Ux.Table.Data[] = display.getLastTableData();

			for (const rowData of tableData) {
				expect(rowData.engine).to.not.equal('eslint-lwc', 'Should not return rules for disabled engine');
			}
		});

		it('Test Case: Filtering explicitly for disabled engine will return its rules', async () => {
			const inputs: Inputs = {
				engine: ['eslint-lwc']
			};

			await ruleListAction.run(inputs);

			let tableData: Ux.Table.Data[] = display.getLastTableData();
			expect(tableData).to.have.length(222);

			for (const rowData of tableData) {
				expect(rowData.engine).to.equal('eslint-lwc');
			}
		});

		it('Edge Case: No matching rules causes empty table', async () => {
			const inputs: Inputs = {
				category: ['beebleborp']
			};

			await ruleListAction.run(inputs);

			let tableData: Ux.Table.Data[] = display.getLastTableData();
			expect(tableData).to.have.length(0, 'No rules should have been logged');
		});
	});

	describe("Tests to confirm that PMD7 binary files are invoked when choosing PMD7", () => {
		afterEach(() => {
			// Until we remove global state, we should cleanup after ourselves to prevent other tests from being impacted
			Controller.setActivePmdCommandInfo(new Pmd6CommandInfo())
		})

		it("When using PMD7, the rule list for the pmd engine should give back rules for PMD 7", async () => {
			const inputs: Inputs = {
				engine: ['pmd'],
				"preview-pmd7": true
			}
			await ruleListAction.run(inputs);

			let tableData: Ux.Table.Data[]  = display.getLastTableData();
			expect(tableData).to.have.length(67);
			for (const rowData of tableData) {
				expect(rowData.engine).to.equal("pmd");
				expect(rowData.sourcepackage).to.contain(PMD7_LIB);
				expect(rowData.name).to.have.length.greaterThan(0);
				expect(rowData.categories).to.have.length.greaterThan(0);
				expect(rowData.isDfa).to.equal(false);
				expect(rowData.isPilot).to.equal(false);
				expect(rowData.languages).to.have.length.greaterThan(0);
			}
		})

		it("When using PMD7, the rule list for the cpd engine should give back the copy-paste-detected rule", async () => {
			const inputs: Inputs = {
				engine: ['cpd'],
				"preview-pmd7": true
			}
			await ruleListAction.run(inputs);

			let tableData: Ux.Table.Data[]  = display.getLastTableData();
			expect(tableData).to.have.length(1);
			expect(tableData[0].engine).to.equal("cpd");
			expect(tableData[0].sourcepackage).to.equal("cpd");
			expect(tableData[0].name).to.equal("copy-paste-detected");
			expect(tableData[0].categories).to.deep.equal(["Copy/Paste Detected"]);
			expect(tableData[0].rulesets).to.deep.equal([]);
			expect(tableData[0].isDfa).to.equal(false);
			expect(tableData[0].isPilot).to.equal(false);
			expect(tableData[0].languages).to.deep.equal(['apex', 'java', 'visualforce', 'xml']);
		});
	});
});
