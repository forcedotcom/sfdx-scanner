import {expect} from 'chai';
import {PathMatcher} from '../../../src/lib/util/PathMatcher';

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
			const results = pm.filterPathsByPatterns(targets);

			it('INCLUDES paths matching ANY positive pattern', () => {
				expect(results).to.include(targets[0], 'Paths that match a single positive pattern should be included');
				expect(results).to.include(targets[1], 'Paths that match a single positive pattern should be included');
			});

			it('EXCLUDES paths matching NO positive patterns', () => {
				expect(results).to.not.include(targets[2], 'Paths that match no positive patterns should be excluded');
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
			const results = pm.filterPathsByPatterns(targets);

			it('INCLUDES paths matching EVERY negative pattern', () => {
				expect(results).to.include(targets[0], 'Paths that match all negative patterns should be included');
				expect(results).to.include(targets[1], 'Paths that match all negative patterns should be included');
			});

			it('EXCLUDES paths matching NOT EVERY negative pattern', () => {
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
				'path/to/SomeClass.java'
			];
			const patterns = [
				'**/*.js', '!**/node_modules/**',
				'**/*.cls', '!/Users/jfeingold/code/CPQ/pkg/**'
			];
			const pm = new PathMatcher(patterns);
			const results = pm.filterPathsByPatterns(targets);

			it('INCLUDES paths matching ANY positive AND ALL negative patterns', () => {
				expect(results).to.include(targets[0], 'Path wrongly excluded.');
				expect(results).to.include(targets[2], 'Path wrongly excluded.')
			});

			it('EXCLUDES paths matching ANY positive patterns but NOT ALL negative patterns', () => {
				expect(results).to.not.include(targets[1], 'Path wrongly included');
				expect(results).to.not.include(targets[3], 'Path wrongly included');
			});

			it('EXCLUDES paths matching ALL negative patterns but NO positive patterns', () => {
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
			const results = pm.filterPathsByPatterns(targets);

			it('INCLUDES paths matching ANY positive AND ALL negative patterns', () => {
				expect(results).to.include(targets[0], 'Path wrongly excluded.');
				expect(results).to.include(targets[2], 'Path wrongly excluded.')
			});

			it('EXCLUDES paths matching ANY positive patterns but NOT ALL negative patterns', () => {
				expect(results).to.not.include(targets[1], 'Path wrongly included');
				expect(results).to.not.include(targets[3], 'Path wrongly included');
			});

			it('EXCLUDES paths matching ALL negative patterns but NO positive patterns', () => {
				expect(results).to.not.include(targets[4], 'Path wrongly included');
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

			it('MATCHES paths matching ANY positive pattern', () => {
				expect(pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Paths matching a single positive pattern should return true.');
				expect(pm.pathMatchesPatterns(targets[1])).to.equal(true, 'Paths matching a single positive pattern should return true.');
			});

			it('DOES NOT MATCH paths matching NO positive patterns', () => {
				expect(pm.pathMatchesPatterns(targets[2])).to.equal(false, 'Paths not matching any positive pattern should return false.');
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

			it('MATCHES paths matching EVERY negative pattern', () => {
				expect(pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Paths that match all negative patterns should return true.');
				expect(pm.pathMatchesPatterns(targets[1])).to.equal(true, 'Paths that match all negative patterns should return true.');
			});

			it('DOES NOT MATCH paths matching NOT EVERY negative pattern', () => {
				expect(pm.pathMatchesPatterns(targets[2])).to.equal(false, 'Paths that match not all negative patterns should return false.');
				expect(pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Paths that match not all negative patterns should return false.');
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

			it('MATCHES paths matching positive and negative patterns', () => {
				expect(pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Path wrongly matched.');
				expect(pm.pathMatchesPatterns(targets[2])).to.equal(true, 'Path wrongly matched.');
			});

			it('DOES NOT MATCH paths matching positive patterns but not negative patterns', () => {
				expect(pm.pathMatchesPatterns(targets[1])).to.equal(false, 'Path wrongly not matched.');
				expect(pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Path wrongly not matched.');
			});

			it('DOES NOT MATCH paths matching negative patterns but not positive patterns', () => {
				expect(pm.pathMatchesPatterns(targets[4])).to.equal(false, 'Path wrongly not matched.');
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

			it('MATCHES paths matching positive and negative patterns', () => {
				expect(pm.pathMatchesPatterns(targets[0])).to.equal(true, 'Path wrongly matched.');
				expect(pm.pathMatchesPatterns(targets[2])).to.equal(true, 'Path wrongly matched.');
			});

			it('DOES NOT MATCH paths matching positive patterns but not negative patterns', () => {
				expect(pm.pathMatchesPatterns(targets[1])).to.equal(false, 'Path wrongly not matched.');
				expect(pm.pathMatchesPatterns(targets[3])).to.equal(false, 'Path wrongly not matched.');
			});

			it('DOES NOT MATCH paths matching negative patterns but not positive patterns', () => {
				expect(pm.pathMatchesPatterns(targets[4])).to.equal(false, 'Path wrongly not matched.');
			});
		})
	})
});
