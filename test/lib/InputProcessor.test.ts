import {InputProcessor, InputProcessorImpl} from "../../src/lib/InputProcessor";
import {assert, expect} from "chai";
import * as path from "path";
import {Inputs} from "../../src/types";
import {BundleName, getMessage} from "../../src/MessageCatalog";
import untildify = require("untildify");
import {FakeDisplay} from "./FakeDisplay";

describe("InputProcessorImpl Tests", async () => {
	let display: FakeDisplay;
	let inputProcessor: InputProcessor;
	beforeEach(async () => {
		display = new FakeDisplay();
		inputProcessor = new InputProcessorImpl("2.11.8", display);
	});

	describe("resolveTargetPaths Tests", async () => {
		it("Specified glob target stays as glob", async () => {
			// Note that we may want to change this behavior in the future instead of waiting to resolve the globs
			// in the DefaultRuleManager. But for now, adding this test.
			const inputs: Inputs = {
				target: ['test\\**\\*.page', '!test/code-fixtures/cpd', '~/*.class']
			};
			const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths(inputs);
			expect(resolvedTargetPaths).to.have.length(3);
			expect(resolvedTargetPaths).to.contain('test/**/*.page');
			expect(resolvedTargetPaths).to.contain('!test/code-fixtures/cpd');
			expect(resolvedTargetPaths).to.contain(untildify('~/*.class'))
		})

		it("Specified target with method specifier", async () => {
			const inputs: Inputs = {
				target: ['test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts']
			};
			const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths(inputs);
			expect(resolvedTargetPaths).to.have.length(1);
			expect(resolvedTargetPaths).to.contain('test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts');
		})

		it("Unspecified target resolves to current directory", async () => {
			const inputs: Inputs = {}
			const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths(inputs);
			expect(resolvedTargetPaths).to.have.length(1);
			expect(resolvedTargetPaths).to.contain('.');
		})
	})

	describe("resolveProjectDirPath Tests", async () => {
		it("Specified relative projectdir", async () => {
			const inputs: Inputs = {
				projectdir: 'test/code-fixtures'
			};
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath(inputs);
			expect(resolvedProjectDir).to.equal(path.resolve('test/code-fixtures'))
		})

		it("Specified absolute projectdir", async () => {
			const inputs: Inputs = {
				projectdir: path.resolve('test/code-fixtures')
			};
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath(inputs);
			expect(resolvedProjectDir).to.equal(path.resolve('test/code-fixtures'))
		})

		it("Specified tildified projectdir", async () => {
			const inputs: Inputs = {
				projectdir: '~/someFolder'
			};
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath(inputs);
			expect(resolvedProjectDir).to.equal(untildify('~/someFolder'))
		})

		it("Unspecified projectdir and unspecified target", async() => {
			const inputs: Inputs = {}
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath(inputs);
			expect(resolvedProjectDir).to.equal(path.resolve('.'));
		})

		it("Unspecified projectdir with non-glob relative targets supplied", async () => {
			const inputs: Inputs = {
				target: ['test/code-fixtures/apex', 'test/catalog-fixtures/DefaultCatalogFixture.json']
			};
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath(inputs);
			expect(resolvedProjectDir).to.equal(path.resolve('test'));

			expect(display.getOutputText()).to.equal('[VerboseInfo]: ' +
				getMessage(BundleName.CommonRun, 'info.resolvedProjectDir', [path.resolve('test')]))
		})

		it("Unspecified projectdir with glob targets supplied (with sfdx-project.json in parents)", async () => {
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath({
				target: ['test/**/*.page', '!test/code-fixtures/cpd']
			});
			// Note that test/code-fixtures/projects/app/force-app/main/default/pages is the first most common parent
			// but test/code-fixtures/projects/app contains a sfdx-project.json and so we return this instead
			expect(resolvedProjectDir).to.equal(path.resolve('test/code-fixtures/projects/app'));
		})

		it("Unspecified projectdir with glob targets supplied (with no sfdx-project.json in parents)", async () => {
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath({
				target: ['test/code-fixtures/**/*.cls']
			});
			expect(resolvedProjectDir).to.equal(path.resolve('test/code-fixtures'));
		})

		it("Unspecified projectdir with target containing method specifiers", async () => {
			const resolvedProjectDir: string = inputProcessor.resolveProjectDirPath({
				target: [
					'test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts',
					'test/code-fixtures/apex/SomeOtherTestClass.cls#someTestMethodWithoutAsserts',
				]
			});
			expect(resolvedProjectDir).to.equal(path.resolve('test/code-fixtures/apex'));
		})

		it("Unspecified projectdir with non-glob target that resolves to no files", async () => {
			const inputs: Inputs = {
				target: ['thisFileDoesNotExist.xml', 'thisFileAlsoDoesNotExist.json']
			};
			try {
				inputProcessor.resolveProjectDirPath(inputs);
				assert.fail("Expected error to be thrown")
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.noFilesFoundInTarget'));
			}
		})

		it("Unspecified projectdir with glob target that resolves to no files", async () => {
			const inputs: Inputs = {
				target: ['**.filesOfThisTypeShouldNotExist']
			};
			try {
				inputProcessor.resolveProjectDirPath(inputs);
				assert.fail("Expected error to be thrown")
			} catch (e) {
				expect(e.message).to.equal(getMessage(BundleName.CommonRun, 'validations.noFilesFoundInTarget'));
			}
		})
	})
})
