import {expect} from 'chai';
import {PathMatcher} from '../../../src/lib/util/PathMatcher';
import path = require('path');

describe('PathMatcher', () => {
	describe('#filterPathsByPatterns()', () => {
		describe('Positive patterns only', () => {
			const targets = [
				'path/to/some/javascriptfile.js',
				'/Users/jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/some/javafile.java'
			];
			const patterns = ['**/*.js', '**/*.ts', '/Users/jfeingold/code/CPQ/**/*.cls'];
			const pm = new PathMatcher(patterns);

			it('INCLUDES paths matching ANY positive pattern', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.include(targets[0], 'Paths that match a single positive pattern should be included');
				expect(results).to.include(targets[1], 'Paths that match a single positive pattern should be included');
			});

			it('EXCLUDES paths matching NO positive patterns', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[2], 'Paths that match no positive patterns should be excluded');
			});
		});

		describe('Complex Patterns only', () => {
			const targets = [
				'path/to/some/fileAlpha.js',
				'path/to/some/fileAlpha.ts',
				'path/to/matchableDir/fileBeta.js',
				'path/to/matchableDir/fileBeta.ts',
				'path/to/nonMatchableDir/fileBeta.js',
				'path/to/nonMatchableDir/fileBeta.ts'
			];

			const patterns = [{
				// This pattern will effectively only match `**/fileAlpha.js`.
				basePatterns: ['**/*.js'],
				advancedMatcher: async (t: string): Promise<boolean> => {
					return path.basename(t, path.extname(t)) === 'fileAlpha';
				}
			}, {
				// This pattern will effectively only match `**/matchableDir/**/*.ts`.
				basePatterns: ['**/*.ts'],
				advancedMatcher: async (t: string): Promise<boolean> => {
					return t.includes('/matchableDir/');
				}
			}];

			const pm = new PathMatcher(patterns);

			it('INCLUDES paths matching ANY complex pattern', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.include(targets[0], 'Paths that match a single complex pattern should be included');
				expect(results).to.include(targets[3], 'Paths that match a single complex pattern should be included');
			});

			it('EXCLUDES paths matching NO complex pattern', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[1], 'Paths matching no complex patterns should be excluded');
				expect(results).to.not.include(targets[2], 'Paths matching no complex patterns should be excluded');
				expect(results).to.not.include(targets[4], 'Paths matching no complex patterns should be excluded');
				expect(results).to.not.include(targets[5], 'Paths matching no complex patterns should be excluded');
			});
		});

		describe('Negative patterns only', () => {
			const targets = [
				'path/to/some/javascriptfile.js',
				'~/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'root/node_modules/SomeModule/main.js',
				'~/code/CPQ/pkg/main/default/components/SomeComp.component-meta.xml'
			];
			const patterns = ['!**/node_modules/**', '!**/*.component-meta.xml'];
			const pm = new PathMatcher(patterns);

			it('INCLUDES paths matching EVERY negative pattern', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.include(targets[0], 'Paths that match all negative patterns should be included');
				expect(results).to.include(targets[1], 'Paths that match all negative patterns should be included');
			});

			it('EXCLUDES paths matching NOT EVERY negative pattern', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[2], 'Paths not matching every negative pattern should be excluded');
				expect(results).to.not.include(targets[3], 'Paths not matching every negative pattern should be excluded');
			});
		});

		describe('Mixed patterns', () => {
			const targets = [
				'path/to/a/JSScript.js',
				'path/to/node_modules/lib/JSScript.js',
				'path/to/QuoteTrigger.cls',
				'/Users/jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/SomeClass.java',
				'path/to/someFile.resource',
				'/Users/jfeingold/code/CPQ/pkg/someFile.resource',
			];
			const patterns = [
				'**/*.js', '!**/node_modules/**',
				'**/*.cls', '!/Users/jfeingold/code/CPQ/pkg/**',
				{
					// This effectively matches `**/**.resource`.
					basePatterns: ['**/**'],
					advancedMatcher: async (t: string): Promise<boolean> => {
						return path.extname(t) === '.resource';
					}
				}
			];
			const pm = new PathMatcher(patterns);

			it('INCLUDES paths matching ANY positive or complex AND ALL negative patterns', async() => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.include(targets[0], 'Path wrongly excluded.');
				expect(results).to.include(targets[2], 'Path wrongly excluded.');
				expect(results).to.include(targets[5], 'Path wrongly excluded.');
			});

			it('EXCLUDES paths matching ANY positive or complex patterns but NOT ALL negative patterns', async() => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[1], 'Path wrongly included');
				expect(results).to.not.include(targets[3], 'Path wrongly included');
				expect(results).to.not.include(targets[7], 'Path wrongly included');
			});

			it('EXCLUDES paths matching ALL negative patterns but NO positive or complex patterns', async() => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[4], 'Path wrongly included');
			});
		});

		describe('De-normalized paths', () => {
			const targets = [
				'path\\to\\a/JSScript.js',
				'path/to\\node_modules/lib\\JSScript.js',
				'path/to/QuoteTrigger.cls',
				'C:\\Users\\jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/SomeClass.java'
			];
			const patterns = [
				'**\\*.js', '!**/node_modules/**',
				'**/*.cls', '!C:/Users\\jfeingold/code/CPQ/pkg/**'
			];

			const pm = new PathMatcher(patterns);

			it('INCLUDES paths matching ANY positive AND ALL negative patterns', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.include(targets[0], 'Path wrongly excluded.');
				expect(results).to.include(targets[2], 'Path wrongly excluded.')
			});

			it('EXCLUDES paths matching ANY positive patterns but NOT ALL negative patterns', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[1], 'Path wrongly included');
				expect(results).to.not.include(targets[3], 'Path wrongly included');
			});

			it('EXCLUDES paths matching ALL negative patterns but NO positive patterns', async () => {
				const results = await pm.filterPathsByPatterns(targets);
				expect(results).to.not.include(targets[4], 'Path wrongly included');
			});
		});

		describe('Hidden dot folders', () => {
			it('When projectDir contains a .dotFolder then the files underneath it are not matched', async () => {
				const pm: PathMatcher = new PathMatcher(['**/*.js', '/some/projectFolder/**/*.cls'], '/some/projectFolder');
				const targets: string[] = [
					'/some/projectFolder/subFolder/.dotFolder/a.js',
					'/some/projectFolder/subFolder/nonDotFolder/b.js',
					'/some/projectFolder/subFolder/.dotFolder/c.cls',
					'/some/projectFolder/subFolder/nonDotFolder/d.cls'
				];

				const results: string[] = await pm.filterPathsByPatterns(targets);
				expect(results).to.deep.equal([targets[1],targets[3]]);
			});

			it('When projectDir contains is under a .dotFolder then the projectDir is not excluded', async () => {
				const pm: PathMatcher = new PathMatcher(['**/*.js', '/some/.dotFolder/projectFolder/**/*.cls'], '/some/.dotFolder/projectFolder');
				const targets: string[] = [
					'/some/.dotFolder/projectFolder/subFolder/.dotFolder/a.js',
					'/some/.dotFolder/projectFolder/subFolder/nonDotFolder/b.js',
					'/some/.dotFolder/projectFolder/subFolder/.dotFolder/c.cls',
					'/some/.dotFolder/projectFolder/subFolder/nonDotFolder/d.cls'
				];

				const results: string[] = await pm.filterPathsByPatterns(targets);
				expect(results).to.deep.equal([targets[1],targets[3]]);
			});
		});
	});

	describe('#pathMatchesPatterns()', () => {
		describe('Positive patterns only', () => {
			const targets = [
				'path/to/some/javascriptfile.js',
				'/Users/jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/some/javafile.java'
			];
			const patterns = ['**/*.js', '**/*.ts', '/Users/jfeingold/code/CPQ/**/*.cls'];
			const pm = new PathMatcher(patterns);

			it('MATCHES paths matching ANY positive pattern', async () => {
				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Paths matching a single positive pattern should return true.');
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(true, 'Paths matching a single positive pattern should return true.');
			});

			it('DOES NOT MATCH paths matching NO positive patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(false, 'Paths not matching any positive pattern should return false.');
			});
		});

		describe('Negative patterns only', () => {
			const targets = [
				'path/to/some/javascriptfile.js',
				'~/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'root/node_modules/SomeModule/main.js',
				'~/code/CPQ/pkg/main/default/components/SomeComp.component-meta.xml'
			];
			const patterns = ['!**/node_modules/**', '!**/*.component-meta.xml'];
			const pm = new PathMatcher(patterns);

			it('MATCHES paths matching EVERY negative pattern', async () => {
				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Paths that match all negative patterns should return true.');
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(true, 'Paths that match all negative patterns should return true.');
			});

			it('DOES NOT MATCH paths matching NOT EVERY negative pattern', async () => {
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(false, 'Paths that match not all negative patterns should return false.');
				expect(await pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Paths that match not all negative patterns should return false.');
			});
		});

		describe('Mixed patterns', () => {
			const targets = [
				'path/to/a/JSScript.js',
				'path/to/node_modules/lib/JSScript.js',
				'path/to/QuoteTrigger.cls',
				'/Users/jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/SomeClass.java'
			];
			const patterns = [
				'**/*.js', '!**/node_modules/**',
				'**/*.cls', '!/Users/jfeingold/code/CPQ/pkg/**'
			];
			const pm = new PathMatcher(patterns);

			it('MATCHES paths matching positive and negative patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Path wrongly matched.');
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(true, 'Path wrongly matched.');
			});

			it('DOES NOT MATCH paths matching positive patterns but not negative patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(false, 'Path wrongly not matched.');
				expect(await pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Path wrongly not matched.');
			});

			it('DOES NOT MATCH paths matching negative patterns but not positive patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[4])).to.equal(false, 'Path wrongly not matched.');
			});
		});

		describe('De-normalized paths', () => {
			const targets = [
				'path\\to\\a/JSScript.js',
				'path/to\\node_modules/lib\\JSScript.js',
				'path/to/QuoteTrigger.cls',
				'C:\\Users\\jfeingold/code/CPQ/pkg/main/default/classes/QuoteTrigger.cls',
				'path/to/SomeClass.java'
			];
			const patterns = [
				'**\\*.js', '!**/node_modules/**',
				'**/*.cls', '!C:/Users\\jfeingold/code/CPQ/pkg/**'
			];
			const pm = new PathMatcher(patterns);

			it('MATCHES paths matching positive and negative patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Path wrongly matched.');
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(true, 'Path wrongly matched.');
			});

			it('DOES NOT MATCH paths matching positive patterns but not negative patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(false, 'Path wrongly not matched.');
				expect(await pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Path wrongly not matched.');
			});

			it('DOES NOT MATCH paths matching negative patterns but not positive patterns', async () => {
				expect(await pm.pathMatchesPatterns(targets[4])).to.equal(false, 'Path wrongly not matched.');
			});
		});

		describe('Hidden dot folders', () => {
			it('When projectDir contains a .dotFolder then the files underneath it are not matched', async () => {
				const pm: PathMatcher = new PathMatcher(['**/*.js', '/some/projectFolder/**/*.cls'], '/some/projectFolder');
				const targets: string[] = [
					'/some/projectFolder/subFolder/.dotFolder/a.js',
					'/some/projectFolder/subFolder/nonDotFolder/b.js',
					'/some/projectFolder/subFolder/.dotFolder/c.cls',
					'/some/projectFolder/subFolder/nonDotFolder/d.cls'
				];

				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(false);
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(true);
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(false);
				expect(await pm.pathMatchesPatterns(targets[3])).to.equal(true);
			});

			it('When projectDir contains is under a .dotFolder then the projectDir is not excluded', async () => {
				const pm: PathMatcher = new PathMatcher(['**/*.js', '/some/.dotFolder/projectFolder/**/*.cls'], '/some/.dotFolder/projectFolder/');
				const targets: string[] = [
					'/some/.dotFolder/projectFolder/.dotFolder/a.js',
					'/some/.dotFolder/projectFolder/nonDotFolder/b.js',
					'/some/.dotFolder/projectFolder/.dotFolder/c.cls',
					'/some/.dotFolder/projectFolder/nonDotFolder/d.cls'
				];

				expect(await pm.pathMatchesPatterns(targets[0])).to.equal(false);
				expect(await pm.pathMatchesPatterns(targets[1])).to.equal(true);
				expect(await pm.pathMatchesPatterns(targets[2])).to.equal(false);
				expect(await pm.pathMatchesPatterns(targets[3])).to.equal(true);
			});
		});
	});
});
