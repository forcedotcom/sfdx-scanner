import {InputProcessor, InputProcessorImpl} from "../../src/lib/InputProcessor";
import {expect} from "chai";

describe("InputProcessorImpl Tests", async () => {
	it("Test that missing target resolves to current directory", async () => {
		const inputProcessor: InputProcessor = new InputProcessorImpl("2.11.8")
		const resolvedTargetPaths: string[] = inputProcessor.resolveTargetPaths({})
		expect(resolvedTargetPaths).to.have.length(1)
		expect(resolvedTargetPaths).to.contain('.')
	})
})
