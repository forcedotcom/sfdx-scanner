import "reflect-metadata";
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {RuleResult} from '../../../src/types';
import path = require('path');
import {expect} from 'chai';
import Sinon = require('sinon');
import {container} from 'tsyringe'
import {Services} from '../../../src/ioc.config';
import {RuleEngine} from '../../../src/lib/services/RuleEngine'
import { PmdEngine }  from '../../../src/lib/pmd/PmdEngine'

class TestPmdEngine extends PmdEngine {
	public xmlToRuleResults(pmdXml: string): RuleResult[] {
		return super.xmlToRuleResults(pmdXml);
	}
}

describe('PmdEngine', () => {
	before(() => {
		Sinon.createSandbox();
		// Bootstrap the container.
		// Avoids 'Cannot register a type name as a singleton without a "to" token' exception
		container.resolveAll<RuleEngine>(Services.RuleEngine);
	});
	after(() => {
		Sinon.restore();
	});
	describe('xmlToRuleResults()', () => {
		it('Unknown XML nodes are ignored', async () => {
			// This xml file contains a 'configerror' node that is a direct child of the pmd node.
			// The configerror node should be filtered out of the results without causing any errors.
			const xmlPath = path.join('test', 'code-fixtures', 'pmd-results', 'result-with-configerror.xml');
			const fileHandler: FileHandler = new FileHandler();
			const xml: string = await fileHandler.readFile(xmlPath);
			expect(xml).to.not.be.null;

			const testPmdEngine = new TestPmdEngine();
			await testPmdEngine.init();

			const results = testPmdEngine.xmlToRuleResults(xml);
			expect(results).to.be.length(1, 'Results should be for be a single file');
			expect(results[0].violations).to.be.length(13, 'The file should have 13 violations');
		});
	});
});
