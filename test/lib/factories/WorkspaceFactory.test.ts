import path from 'node:path';
import {CodeAnalyzerConfig, CodeAnalyzer, Workspace} from '@salesforce/code-analyzer-core';
import {WorkspaceFactory} from '../../../src/lib/factories/WorkspaceFactory';


describe('WorkspaceFactory', () => {
	const ORIGINAL_TEST_DIR = process.cwd();
	const PATH_TO_WORKSPACE = path.join(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'workspace-with-dotted-items');

	beforeEach(() => {
		process.chdir(PATH_TO_WORKSPACE)
	});

	afterEach(() => {
		process.chdir(ORIGINAL_TEST_DIR);
	});

	describe('File/Folder matching', () => {
		it.each([
			{scenario: 'a single file', expectation: 'just that file', input: ['undotted-file-1.txt']},
			{
				scenario: 'a list of files',
				expectation: 'just those files',
				input: [path.join('.', 'undotted-directory-a', 'undotted-file-2a.txt'), '.dotted-file-2.txt']
			},
			{scenario: 'a single folder', expectation: 'just that folder', input: ['undotted-directory-a']},
			{
				scenario: 'a list of folders',
				expectation: 'just those folders',
				input: ['undotted-directory-a', '.dotted-directory-b']
			},
			{
				scenario: 'mixed folders and files',
				expectation: 'just those files/folders',
				input: ['.dotted-file-1.txt', path.join('.', 'undotted-directory-b')]
			}
		])('Given $scenario, requests a workspace with $expectation', async ({input}) => {
			// ==== TEST SETUP ====
			const config: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();
			const core: CodeAnalyzer = new CodeAnalyzer(config);
			const factory: WorkspaceFactory = new WorkspaceFactory();

			// ==== TESTED BEHAVIOR ====
			const workspace: Workspace = await factory.create(core, input);

			// ==== ASSERTIONS ====
			const workspaceItems: string[] = workspace.getFilesAndFolders();
			const expectedWorkspaceSet = new Set(input.map(i => path.resolve('.', i)));
			expect(workspaceItems).toHaveLength(expectedWorkspaceSet.size);
			for (const expectedWorkspaceItem of expectedWorkspaceSet.keys()) {
				expect(workspaceItems).toContain(expectedWorkspaceItem);
			}
		});
	});

	describe('Glob matching', () => {
		it.each([
			{
				scenario: 'a single relative file',
				expectation: 'just that file',
				input: path.join('.', 'undotted*1.txt'),
				output: new Set(['undotted-file-1.txt'])
			},
			{
				scenario: 'a list of relative files',
				expectation: 'just those files',
				input: path.join('.', 'undotted*'),
				output: new Set(['undotted-file-1.txt', 'undotted-file-2.txt'])
			},
			{
				scenario: 'a single absolute file',
				expectation: 'just that file',
				input: path.resolve(PATH_TO_WORKSPACE, 'undotted*1.txt'),
				output: new Set(['undotted-file-1.txt'])
			},
			{
				scenario: 'a list of absolute files',
				expectation: 'just those files',
				input: path.resolve(PATH_TO_WORKSPACE, 'undotted*'),
				output: new Set(['undotted-file-1.txt', 'undotted-file-2.txt'])
			}
		])('Given a glob that matches $scenario, requests a workspace with $expectation', async ({input, output}) => {
			// ==== TEST SETUP ====
			const config: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();
			const core: CodeAnalyzer = new CodeAnalyzer(config);
			const factory: WorkspaceFactory = new WorkspaceFactory();

			// ==== TESTED BEHAVIOR ====
			const workspace: Workspace = await factory.create(core, [input]);

			// ==== ASSERTIONS ====
			const workspaceBaseNames: string[] = workspace.getFilesAndFolders().map(i => path.basename(i));
			expect(workspaceBaseNames).toHaveLength(output.size);
			for (const expectedBaseName of output.keys()) {
				expect(workspaceBaseNames).toContain(expectedBaseName);
			}
		});

		describe('Dotted file/folder handling', () => {
			it.each([
				{
					scenario: 'the current directory',
					glob: path.join('.', '.*'),
					expectation: [
						expect.stringMatching('\\.dotted-file-1\\.txt'),
						expect.stringMatching('\\.dotted-file-2\\.txt')
					]
				},
				{
					scenario: 'dotted subfolders of the current directory',
					glob: path.join('.', '.*/.*'),
					expectation: [
						expect.stringMatching('\\.dotted-file-1a\\.txt'),
						expect.stringMatching('\\.dotted-file-1b\\.txt')
					]
				},
				{
					scenario: 'undotted subfolders of the current directory',
					glob: path.join('.', '*/.*'),
					expectation: [
						expect.stringMatching('\\.dotted-file-2a\\.txt'),
						expect.stringMatching('\\.dotted-file-2b\\.txt')
					]
				}
			])('A glob explicitly seeking dotted files in $scenario will find them', async ({glob, expectation}) => {
				// ==== TEST SETUP ====
				const config: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();
				const core: CodeAnalyzer = new CodeAnalyzer(config);
				const factory: WorkspaceFactory = new WorkspaceFactory();

				// ==== TESTED BEHAVIOR ====
				const workspace: Workspace = await factory.create(core, [glob]);

				// ==== ASSERTIONS ====
				const workspaceFiles: string[] = workspace.getFilesAndFolders();
				expect(workspaceFiles).toHaveLength(expectation.length);
				expect(workspaceFiles).toEqual(expect.arrayContaining(expectation));
			});

			it.each([
				{
					scenario: 'the current directory',
					glob: path.join('.', '*'),
					expectation: [
						expect.stringMatching('undotted-file-1\\.txt'),
						expect.stringMatching('undotted-file-2\\.txt')
					]
				},
				{
					scenario: 'dotted subfolders of the current directory',
					glob: path.join('.', '.**/*'),
					expectation: [
						expect.stringMatching('undotted-file-1a\\.txt'),
						expect.stringMatching('undotted-file-1b\\.txt')
					]
				},
				{
					scenario: 'undotted subfolders of the current directory',
					glob: path.join('.', '*/*'),
					expectation: [
						expect.stringMatching('undotted-file-2a\\.txt'),
						expect.stringMatching('undotted-file-2b\\.txt')
					]
				}
			])('A glob NOT explicitly seeking dotted items in $scenario will find only undotted items', async ({
																												   glob,
																												   expectation
																											   }) => {
				// ==== TEST SETUP ====
				const config: CodeAnalyzerConfig = CodeAnalyzerConfig.withDefaults();
				const core: CodeAnalyzer = new CodeAnalyzer(config);
				const factory: WorkspaceFactory = new WorkspaceFactory();

				// ==== TESTED BEHAVIOR ====
				const workspace: Workspace = await factory.create(core, [glob]);

				// ==== ASSERTIONS ====
				const workspaceFiles: string[] = workspace.getFilesAndFolders();
				expect(workspaceFiles).toHaveLength(expectation.length);
				expect(workspaceFiles).toEqual(expect.arrayContaining(expectation));
			});
		});
	});
});
