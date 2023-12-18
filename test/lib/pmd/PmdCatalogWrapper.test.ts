import {PmdCatalogWrapper} from '../../../src/lib/pmd/PmdCatalogWrapper';
import {Config} from '../../../src/lib/util/Config';
import {expect} from 'chai';
import Sinon = require('sinon');
import path = require('path');
import {ENGINE,LANGUAGE} from '../../../src/Constants';
import { after } from 'mocha';

// In order to get access to PmdCatalogWrapper's protected methods, we're going to extend it with a test class here.
class TestablePmdCatalogWrapper extends PmdCatalogWrapper {
	public async buildCommandArray(): Promise<[string, string[]]> {
		return super.buildCommandArray();
	}
}

describe('PmdCatalogWrapper', () => {
	describe('buildCommandArray()', () => {
		describe('JAR parameters', () => {
			describe('Common to all scenarios', () => {
				before(() => {
					Sinon.createSandbox();
					// Spoof a config that claims that only Apex's default PMD JAR is enabled.
					Sinon.stub(Config.prototype, 'getSupportedLanguages').withArgs(ENGINE.PMD).resolves([LANGUAGE.APEX]);
				});

				after(() => {
					Sinon.restore();
				});

				it('uses the correct common parameter values', async () => {
					const thePath = path.join('dist', 'pmd-cataloger', 'lib');
					const expectedParamList = [
						`-DcatalogHome=`,
						'-DcatalogName=',
						'-cp',
						thePath,
						'sfdc.sfdx.scanner.pmd.Main'];

					const target = await TestablePmdCatalogWrapper.create({
						rulePathsByLanguage: new Map<string, Set<string>>()
					});
					const params = (await target.buildCommandArray())[1];

					expectedParamList.forEach((value: string, index: number) => {
						expect(params[index]).contains(value, `Unexpected param value at position ${index}`);
					});
				});
			});
		});
	});
});

