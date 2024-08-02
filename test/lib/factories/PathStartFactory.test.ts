import path from 'node:path';
import {PathStartFactory} from '../../../src/lib/factories/PathStartFactory';


describe('PathStartFactory implementations', () => {
	const ORIGINAL_TEST_DIR = process.cwd();
	const PATH_TO_WORKSPACE = path.join(__dirname, '..', '..', 'fixtures', 'example-workspaces', 'workspace-with-dotted-items');

	beforeEach(() => {
		process.chdir(PATH_TO_WORKSPACE);
	});

	afterEach(() => {
		process.chdir(ORIGINAL_TEST_DIR);
	})

	it('When passed undefined, returns undefined', async () => {
		const factory = new PathStartFactory();
		const output = await factory.create(undefined);
		expect(output).toBeUndefined();
	});

	it('When passed an empty array, returns an empty array', async () => {
		const factory = new PathStartFactory();
		const output = await factory.create([]);
		expect(output).toEqual([]);
	});

	describe('File matching', () => {
		it.each([
			{scenario: 'a single file', expectation: 'just that file', input: ['undotted-file-1.txt']},
			{scenario: 'a list of files', expectation: 'just those files', input: [path.join('.', 'undotted-directory-a', 'undotted-file-2a.txt'), '.dotted-file-2.txt']}
		])('Given $scenario, returns $expectation', async ({input}) => {
			const factory = new PathStartFactory();
			const output = await factory.create(input);
			expect(output).toBeDefined();
			expect(output as string[]).toHaveLength(input.length);
			expect(output as string[]).toEqual(input);
		})
	});

	describe('Glob matching', () => {
		it.each([
			{
				scenario: 'a single relative file',
				glob: path.join('.', 'undotted*1.txt'),
				expectation: [
					expect.stringMatching('undotted-file-1\\.txt')
				]
			},
			{
				scenario: 'a list of relative files',
				glob: path.join('.', 'undotted*'),
				expectation: [
					expect.stringMatching('undotted-file-1\\.txt'),
					expect.stringMatching('undotted-file-2\\.txt')
				]
			},
			{
				scenario: 'a single absolute file',
				glob: path.resolve(PATH_TO_WORKSPACE, 'undotted*1.txt'),
				expectation: [
					expect.stringMatching('undotted-file-1\\.txt')
				]
			},
			{
				scenario: 'a list of absolute files',
				glob: path.resolve(PATH_TO_WORKSPACE, 'undotted*'),
				expectation: [
					expect.stringMatching('undotted-file-1\\.txt'),
					expect.stringMatching('undotted-file-2\\.txt')
				]
			}
		])('Given a glob that matches $scenario, returns the correct files', async ({glob, expectation}) => {
			const factory = new PathStartFactory();
			const output = await factory.create([glob]);
			expect(output).toBeDefined();
			expect(output as string[]).toHaveLength(expectation.length);
			expect(output as string[]).toEqual(expect.arrayContaining(expectation));
		});

		describe('Dotted file handling', () => {
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
					glob: path.join('.', '.*'),
					expectation: [
						expect.stringMatching('\\.dotted-file-1\\.txt'),
						expect.stringMatching('\\.dotted-file-2\\.txt')
					]
				},
				{
					scenario: 'the current directory',
					glob: path.join('.', '.*'),
					expectation: [
						expect.stringMatching('\\.dotted-file-1\\.txt'),
						expect.stringMatching('\\.dotted-file-2\\.txt')
					]
				}
			])('A glob explicitly seeking dotted files in $scenario will find them', async ({glob, expectation}) => {
				const factory = new PathStartFactory();
				const output = await factory.create([glob]);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(expectation.length);
				expect(output as string[]).toEqual(expect.arrayContaining(expectation));
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
					glob: path.join('.', '.**', '*'),
					expectation: [
						expect.stringMatching('undotted-file-1a\\.txt'),
						expect.stringMatching('undotted-file-1b\\.txt')
					]
				},
				{
					scenario: 'undotted subfolders of the current directory',
					glob: path.join('.', '*', '*'),
					expectation: [
						expect.stringMatching('undotted-file-2a\\.txt'),
						expect.stringMatching('undotted-file-2b\\.txt')
					]
				},
			])('A glob NOT explicitly seeking dotted items in $scenario will find only undotted items', async ({glob, expectation}) => {
				const factory = new PathStartFactory();
				const output = await factory.create([glob]);
				expect(output).toBeDefined();
				expect(output as string[]).toHaveLength(expectation.length);
				expect(output as string[]).toEqual(expect.arrayContaining(expectation));
			})
		})
	});

	describe('Method matching', () => {
		it('When given a method in a file, returns that method and file', async () => {
			const input = path.join('.', 'undotted-file-1.txt#someMethod');
			const factory = new PathStartFactory();
			const output = await factory.create([input]);
			expect(output).toBeDefined();
			expect(output as string[]).toHaveLength(1);
			expect((output as string[])[0]).toMatch('undotted-file-1.txt#someMethod');
		});
	});

	describe('Pathological cases', () => {
		it('Method matching and globs are mutually exclusive', async () => {
			const input = path.join('.', '**', '*.cls#someMethod');
			const factory = new PathStartFactory();

			// Typically we'd use Jest's `expect().toThrow()` method, but since we need to assert specific things about
			// the error, we're doing this instead.
			let thrownError: Error|null = null;
			try {
				await factory.create([input]);
			} catch (e) {
				thrownError = e;
			}
			expect(thrownError).toBeInstanceOf(Error);
			// Expect the error message to mention globs in some capacity.
			expect((thrownError as Error).message).toContain('glob');
		});
	})
})

