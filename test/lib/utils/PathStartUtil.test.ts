import path from 'node:path';
import * as PathStartUtil from '../../../src/lib/utils/PathStartUtil';


describe('PathStartUtil', () => {
	describe('#createPathStarts()', () => {
		const ORIGINAL_TEST_DIR = process.cwd();
		const PATH_TO_WORKSPACE = path.join(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'workspace-with-dotted-items');

		beforeEach(() => {
			process.chdir(PATH_TO_WORKSPACE);
		});

		afterEach(() => {
			process.chdir(ORIGINAL_TEST_DIR);
		})

		it('When passed undefined, returns undefined', async () => {
			const output = await PathStartUtil.createPathStarts(undefined);
			expect(output).toBeUndefined();
		});

		it('When passed an empty array, returns an empty array', async () => {
			const output = await PathStartUtil.createPathStarts([]);
			expect(output).toEqual([]);
		});

		describe('File matching', () => {
			it.each([
				{scenario: 'a single relative file', expectation: 'just that file', input: ['undotted-file-1.txt']},
				{
					scenario: 'a list of relative files',
					expectation: 'just those files',
					input: [path.join('.', 'undotted-directory-a', 'undotted-file-2a.txt'), '.dotted-file-2.txt']
				},
				{
					scenario: 'a single absolute file',
					expectation: 'just that file',
					input: [path.join(PATH_TO_WORKSPACE, 'undotted-file-1.txt')]
				},
				{
					scenario: 'a list of absolute files',
					expectation: 'just those files',
					input: [
						path.join(PATH_TO_WORKSPACE, 'undotted-directory-a', 'undotted-file-2a.txt'),
						path.join(PATH_TO_WORKSPACE, 'undotted-file-2.txt')
					]
				}
			])('Given $scenario, returns $expectation', async ({input}) => {
				const output = await PathStartUtil.createPathStarts(input);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(input.length);
				expect(output as string[]).toEqual(input);
			});

			it('File names with brackets are matched as files instead of globs', async () => {
				const input = [path.join('.', 'path-name[with]brackets.txt')];
				const output = await PathStartUtil.createPathStarts(input);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(input.length);
				expect(output as string[]).toEqual(input);
			});
		});

		describe('Glob matching', () => {
			it.each([
				{
					scenario: 'a single relative file',
					glob: path.join('.', 'undotted*1.txt'),
					expectation: new Set(['undotted-file-1.txt'])
				},
				{
					scenario: 'a list of relative files',
					glob: path.join('.', 'undotted*'),
					expectation: new Set(['undotted-file-1.txt', 'undotted-file-2.txt'])
				},
				{
					scenario: 'a single absolute file',
					glob: path.resolve(PATH_TO_WORKSPACE, 'undotted*1.txt'),
					expectation: new Set([
						path.resolve(PATH_TO_WORKSPACE, 'undotted-file-1.txt')
					])
				},
				{
					scenario: 'a list of absolute files',
					glob: path.resolve(PATH_TO_WORKSPACE, 'undotted*'),
					expectation: new Set([
						path.resolve(PATH_TO_WORKSPACE, 'undotted-file-1.txt'),
						path.resolve(PATH_TO_WORKSPACE, 'undotted-file-2.txt')
					])
				}
			])('Given a glob that matches $scenario, returns the correct files', async ({glob, expectation}) => {
				const output = await PathStartUtil.createPathStarts([glob]);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(expectation.size);
				for (const expectedPathStart of expectation.keys()) {
					expect(output as string[]).toContain(expectedPathStart);
				}
			});

			describe('Dotted file handling', () => {
				it.each([
					{
						scenario: 'the current directory',
						glob: path.join('.', '.*'),
						expectation: new Set([
							'.dotted-file-1.txt',
							'.dotted-file-2.txt'
						])
					},
					{
						scenario: 'dotted subfolders of the current directory',
						glob: path.join('.', '.*', '.*'),
						expectation: new Set([
							path.join('.dotted-directory-a', '.dotted-file-1a.txt'),
							path.join('.dotted-directory-b', '.dotted-file-1b.txt')
						])
					},
					{
						scenario: 'undotted subfolders of the current directory',
						glob: path.join('.', '*', '.*'),
						expectation: new Set([
							path.join('undotted-directory-a', '.dotted-file-2a.txt'),
							path.join('undotted-directory-b', '.dotted-file-2b.txt')
						])
					}
				])('A glob explicitly seeking dotted files in $scenario will find them', async ({
																									glob,
																									expectation
																								}) => {
					const output = await PathStartUtil.createPathStarts([glob]);
					expect(output).toBeDefined();
					expect(output as string[]).toHaveLength(expectation.size);
					for (const expectedPathStart of expectation.keys()) {
						expect(output as string[]).toContain(expectedPathStart);
					}
				});

				it.each([
					{
						scenario: 'the current directory',
						glob: path.join('.', '*'),
						expectation: new Set([
							'undotted-file-1.txt',
							'undotted-file-2.txt'
						])
					},
					{
						scenario: 'dotted subfolders of the current directory',
						glob: path.join('.', '.**', '*'),
						expectation: new Set([
							path.join('.dotted-directory-a', 'undotted-file-1a.txt'),
							path.join('.dotted-directory-b', 'undotted-file-1b.txt')
						])
					},
					{
						scenario: 'undotted subfolders of the current directory',
						glob: path.join('.', '*', '*'),
						expectation: new Set([
							path.join('undotted-directory-a', 'undotted-file-2a.txt'),
							path.join('undotted-directory-b', 'undotted-file-2b.txt')
						])
					},
				])('A glob NOT explicitly seeking dotted items in $scenario will find only undotted items', async ({
																													   glob,
																													   expectation
																												   }) => {
					const output = await PathStartUtil.createPathStarts([glob]);
					expect(output).toBeDefined();
					expect(output as string[]).toHaveLength(expectation.size);
					for (const expectedPathStart of expectation.keys()) {
						expect(output as string[]).toContain(expectedPathStart);
					}
				})
			})
		});

		describe('Method matching', () => {
			it('When given a method in a file, returns that method and file', async () => {
				const input = path.join('.', 'undotted-file-1.txt#someMethod');
				const output = await PathStartUtil.createPathStarts([input]);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(1);
				expect((output as string[])[0]).toMatch('undotted-file-1.txt#someMethod');
			});
		});

		describe('Pathological cases', () => {
			it('Negative globs are not supported', async () => {
				const input = path.join('!', '.', '**', '*.cls');

				// Typically we'd use Jest's `expect().toThrow()` method, but since we need to assert specific things about
				// the error, we're doing this instead.
				let thrownError: Error | null = null;
				try {
					await PathStartUtil.createPathStarts([input]);
				} catch (e) {
					thrownError = e;
				}
				expect(thrownError).toBeInstanceOf(Error);
				// Expect the error message to mention globs in some capacity.
				expect((thrownError as Error).message).toContain('glob');
			});

			it('Method matching and globs are mutually exclusive', async () => {
				const input = path.join('.', '**', '*.cls#someMethod');

				// Typically we'd use Jest's `expect().toThrow()` method, but since we need to assert specific things about
				// the error, we're doing this instead.
				let thrownError: Error | null = null;
				try {
					await PathStartUtil.createPathStarts([input]);
				} catch (e) {
					thrownError = e;
				}
				expect(thrownError).toBeInstanceOf(Error);
				// Expect the error message to mention globs in some capacity.
				expect((thrownError as Error).message).toContain('glob');
			});
		});
	});
})

