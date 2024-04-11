import {expect} from "chai";
import {Ux} from "@salesforce/sf-plugins-core";
import sinon = require('sinon');

import {FakeDisplay} from "../FakeDisplay";
import {initializeTestSetup} from "../../test-related-lib/TestOverrides";

import {RuleFilterFactoryImpl} from "../../../src/lib/RuleFilterFactory";
import {RuleListAction} from "../../../src/lib/actions/RuleListAction";
import {Inputs} from "../../../src/types";
import {ENGINE} from "../../../src/Constants";
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
			sinon.stub(Config.prototype, 'isEngineEnabled')
				.callThrough()
				.withArgs(ENGINE.ESLINT_LWC).resolves(false)
				.withArgs(ENGINE.CPD).resolves(true);
		});

		afterEach(() => {
			sinon.restore();
		});

		it('Test Case: Without filters, all rules for enabled and default-runnable engines are returned', async () => {
			await ruleListAction.run([]);

			let tableData: Ux.Table.Data[] = display.getLastTableData();

			for (const rowData of tableData) {
				expect(rowData.engine).to.not.equal('eslint-lwc', 'Should not return rules for disabled engine');
				// NOTE: Currently, CPD has the unique behavior of only running/listing rules when it's explicitly
				//       requested via the --engine parameter, even if it's listed as enabled. So since it wasn't
				//       explicitly requested, it shouldn't be included.
				//       This behavior is something of an anomaly, and should not be taken as ironclad. If it becomes
				//       advantageous or convenient to change it, we should take the opportunity to do so.
				expect(rowData.engine).to.not.equal('cpd', 'Should not return rule for unrequested CPD engine');
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

		// NOTE: Currently, CPD has the unique behavior of only running/listing rules when it's explicitly requested via
		//       the --engine parameter, even if it's listed as enabled. So since it's explicitly requested here, it should
		//       be included.
		//       This behavior is something of an anomaly, and should not be taken as ironclad. If it becomes
		//       advantageous or convenient to change it, we should take the opportunity to do so.
		it('Test Case: Filtering explicitly for a default non-runnable engine will return its rules', async () => {
			const inputs: Inputs = {
				engine: ['cpd']
			};

			await ruleListAction.run(inputs);

			let tableData: Ux.Table.Data[] = display.getLastTableData();
			expect(tableData).to.have.length(1);

			for (const rowData of tableData) {
				expect(rowData.engine).to.equal('cpd');
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

		it('Test Case: Negative category filtering when using default engines', async () => {
			const inputs: Inputs = {
				category: ['!layout']
			};

			await ruleListAction.run(inputs);

			const categoriesListed: Set<string> = new Set();
			let tableData: Ux.Table.Data[] = display.getLastTableData();
			for (const rowData of tableData) {
				for (const category of rowData.categories as string[]) {
					categoriesListed.add(category);
				}
			}
			expect(categoriesListed).to.not.include('layout');
			expect(categoriesListed).to.include('suggestion');
			expect(categoriesListed).to.include('problem');
			expect(categoriesListed).to.include('Security'); // From PMD which is on by default
		});

		it('Test Case: Negative category filtering when non-default engine is specified', async () => {
			const inputs: Inputs = {
				category: ['!layout'],
				engine: ['eslint-lwc'] // This is a non-default engine
			};

			await ruleListAction.run(inputs);

			const categoriesListed: Set<string> = new Set();
			let tableData: Ux.Table.Data[] = display.getLastTableData();
			for (const rowData of tableData) {
				for (const category of rowData.categories as string[]) {
					categoriesListed.add(category);
				}
			}
			expect(categoriesListed).to.not.include('layout');
			expect(categoriesListed).to.include('suggestion');
			expect(categoriesListed).to.include('problem');
			expect(categoriesListed).to.not.include('Security'); // From PMD (which isn't specified)

		});
	});
});
