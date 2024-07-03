import {InputProcessor, InputProcessorImpl} from "../../src/lib/InputProcessor";
import {expect} from "chai";
import * as path from "path";
import {Inputs} from "../../src/types";
import {BundleName, getMessage} from "../../src/MessageCatalog";
import untildify = require("untildify");
import normalize = require("normalize-path");
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
			expect(resolvedTargetPaths).to.contain(normalize(untildify('~/*.class')))
		})

		it("Specified target with method specifier", async () => {
			const inputs: Inputs = {
				target: ['test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts']
			};
			const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths(inputs);
			expect(resolvedTargetPaths).to.have.length(1);
			expect(resolvedTargetPaths).to.contain('test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts');

			expect(display.getOutputText()).to.equal('')
		})

		it("Unspecified target resolves to current directory", async () => {
			const inputs: Inputs = {}
			const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths(inputs);
			expect(resolvedTargetPaths).to.have.length(1);
			expect(resolvedTargetPaths).to.contain('.');

			expect(display.getOutputText()).to.equal('[Info]: ' +
				getMessage(BundleName.CommonRun, 'info.resolvedTarget'))
		})
	})

	describe("resolveProjectDirPath Tests", async () => {
		it("Specified relative projectdir", async () => {
			const inputs: Inputs = {
				projectdir: ['test/code-fixtures']
			};
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(resolvedProjectDirs).to.contain(toAbsPath('test/code-fixtures'))
		})

		it("Specified absolute projectdir", async () => {
			const inputs: Inputs = {
				projectdir: [toAbsPath('test/code-fixtures')]
			};
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(resolvedProjectDirs).to.contain(toAbsPath('test/code-fixtures'))
		})

		it("Specified tildified projectdir", async () => {
			const inputs: Inputs = {
				projectdir: ['~/someFolder']
			};
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(resolvedProjectDirs).to.contain(toAbsPath(normalize(untildify('~/someFolder'))))
		})

		it("Unspecified projectdir and unspecified target", async() => {
			const inputs: Inputs = {}
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(resolvedProjectDirs).to.contain(toAbsPath('.'));

			expect(display.getOutputArray()).to.have.length(2)
			expect(display.getOutputArray()).to.contain('[Info]: ' +
				getMessage(BundleName.CommonRun, 'info.resolvedTarget'))
			expect(display.getOutputArray()).to.contain('[Info]: ' +
				getMessage(BundleName.CommonRun, 'info.resolvedProjectDir', [toAbsPath('')]))
		})

		it("Unspecified projectdir with non-glob relative targets supplied", async () => {
			const inputs: Inputs = {
				target: ['test/code-fixtures/apex', 'test/catalog-fixtures/DefaultCatalogFixture.json']
			};
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(resolvedProjectDirs).to.contain(toAbsPath('test'));

			expect(display.getOutputText()).to.equal('[Info]: ' +
				getMessage(BundleName.CommonRun, 'info.resolvedProjectDir', [toAbsPath('test')]))
		})

		it("Unspecified projectdir with glob targets supplied (with sfdx-project.json in parents)", async () => {
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths({
				target: ['test/**/*.page', '!test/code-fixtures/cpd']
			});
			// Note that test/code-fixtures/projects/app/force-app/main/default/pages is the first most common parent
			// but test/code-fixtures/projects/app contains a sfdx-project.json and so we return this instead
			expect(resolvedProjectDirs).to.contain(toAbsPath('test/code-fixtures/projects/app'));
		})

		it("Unspecified projectdir with glob targets supplied (with no sfdx-project.json in parents)", async () => {
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths({
				target: ['test/code-fixtures/**/*.cls']
			});
			expect(resolvedProjectDirs).to.contain(toAbsPath('test/code-fixtures'));
		})

		it("Unspecified projectdir with target containing method specifiers", async () => {
			const resolvedProjectDirs: string[] = inputProcessor.resolveProjectDirPaths({
				target: [
					'test/code-fixtures/apex/SomeTestClass.cls#testMethodWithoutAsserts',
					'test/code-fixtures/apex/SomeOtherTestClass.cls#someTestMethodWithoutAsserts',
				]
			});
			expect(resolvedProjectDirs).to.contain(toAbsPath('test/code-fixtures/apex'));
		})

		it("Unspecified projectdir with non-glob target that resolves to no files", async () => {
			const inputs: Inputs = {
				target: ['thisFileDoesNotExist.xml', 'thisFileAlsoDoesNotExist.json']
			};
			const projectDirPaths: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(projectDirPaths).to.deep.equal([process.cwd()]);
		})

		it("Unspecified projectdir with glob target that resolves to no files", async () => {
			const inputs: Inputs = {
				target: ['**.filesOfThisTypeShouldNotExist']
			};
			const projectDirPaths: string[] = inputProcessor.resolveProjectDirPaths(inputs);
			expect(projectDirPaths).to.deep.equal([process.cwd()]);
		})
	})
})

function toAbsPath(fileOrFolder: string): string {
	return path.resolve(fileOrFolder)
}
