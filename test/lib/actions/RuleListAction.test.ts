import {FakeDisplay} from "../FakeDisplay";
import {initContainer} from "../../../src/ioc.config";
import {RuleFilterFactoryImpl} from "../../../src/lib/RuleFilterFactory";
import {RuleListAction} from "../../../src/lib/actions/RuleListAction";
import {Pmd7CommandInfo, PmdCommandInfo} from "../../../src/lib/pmd/PmdCommandInfo";
import {Controller} from "../../../src/Controller";
import {after} from "mocha";
import {Inputs} from "../../../src/types";
import {expect} from "chai";
import {Ux} from "@salesforce/sf-plugins-core";
import {PMD7_LIB} from "../../../src/Constants";

describe("Tests for RuleListAction", () => {
	let display: FakeDisplay;
	let ruleListAction: RuleListAction;
	before(() => {
		initContainer();
	});
	beforeEach(() => {
		display = new FakeDisplay();
		ruleListAction = new RuleListAction(display, new RuleFilterFactoryImpl());
	});

	describe("Tests to confirm that PMD7 binary files are invoked when choosing PMD7", () => {

		// TODO: Soon we will have an input flag to control this. Once we do, we can update this to use that instead
		const originalPmdCommandInfo: PmdCommandInfo = Controller.getActivePmdCommandInfo()
		before(() => {
			Controller.setActivePmdCommandInfo(new Pmd7CommandInfo());
		});
		after(() => {
			Controller.setActivePmdCommandInfo(originalPmdCommandInfo);
		});

		it("When using PMD7, the rule list for the pmd engine should give back rules for PMD 7", async () => {
			const inputs: Inputs = {
				engine: ['pmd']
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
				engine: ['cpd']
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
