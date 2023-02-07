import path = require('path');
import {expect} from 'chai';
import {ClassDescriptor} from '../src/types';
import {summarizeErrors} from '../src/summarizeMochaErrors';

describe('#summarizeErrors', () => {
	it('When there are no failures, does nothing', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'mocha', 'no-failures'));
		const classDescriptors: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(classDescriptors.length).to.equal(0);
	});

	it('If one file has failures, one summary is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'mocha', 'one-failing-file'));
		const classDescriptors: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(classDescriptors.length).to.equal(1);
		expect(path.basename(classDescriptors[0].file)).to.equal('PathMatcher.test.ts');
		expect(classDescriptors[0].failures.length).to.equal(3);
		expect(classDescriptors[0].failures[0].test).to.equal('INCLUDES paths matching ANY positive pattern');
		expect(classDescriptors[0].failures[1].test).to.equal('EXCLUDES paths matching NO positive patterns');
		expect(classDescriptors[0].failures[2].test).to.equal('INCLUDES paths matching EVERY negative pattern');
	});

	it('When multiple files have failures, one summary per file is added', async () => {
		const pathToProject = path.resolve(path.join('code-fixtures', 'mocha', 'multiple-failing-files'));
		const classDescriptors: ClassDescriptor[] = await summarizeErrors(pathToProject);
		expect(classDescriptors.length).to.equal(2);
		expect(path.basename(classDescriptors[0].file)).to.equal('RuleResultRecombinator.test.ts');
		expect(classDescriptors[0].failures.length).to.equal(2);
		expect(classDescriptors[0].failures[0].test).to.equal('Properly handles one file with one violation');
		expect(classDescriptors[0].failures[1].test).to.equal('Run with no violations returns engines that were run');
		expect(path.basename(classDescriptors[1].file)).to.equal('PathMatcher.test.ts');
		expect(classDescriptors[1].failures.length).to.equal(3);
		expect(classDescriptors[1].failures[0].test).to.equal('INCLUDES paths matching ANY positive pattern');
		expect(classDescriptors[1].failures[1].test).to.equal('EXCLUDES paths matching NO positive patterns');
		expect(classDescriptors[1].failures[2].test).to.equal('INCLUDES paths matching EVERY negative pattern');
	});
});
