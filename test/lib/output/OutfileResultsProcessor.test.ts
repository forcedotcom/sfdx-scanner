import {FakeResults} from "./FakeResults";
import {Results} from "../../../src/lib/output/Results";
import {ResultsProcessor} from "../../../src/lib/output/ResultsProcessor";
import {OutfileResultsProcessor} from "../../../src/lib/output/OutfileResultsProcessor";
import * as os from "os";
import * as path from "path";
import {expect} from "chai";
import {OutputFormat} from "../../../src/lib/output/OutputFormat";
import fs = require('fs');

describe('OutfileResultsProcessor Tests', () => {
	let tmpDir: string = null;
	const results: Results = new FakeResults()
		.withFormattedOutputForFormat(OutputFormat.CSV, "dummy csv contents")
		.withFormattedOutputForFormat(OutputFormat.HTML, "dummy html contents")
		.withFormattedOutputForFormat(OutputFormat.JSON, "dummy json contents")
		.withFormattedOutputForFormat(OutputFormat.JUNIT, "dummy junit contents")
		.withFormattedOutputForFormat(OutputFormat.SARIF, "dummy sarif contents")
		.withFormattedOutputForFormat(OutputFormat.XML, "dummy xml contents");
	beforeEach(() => {
		tmpDir = fs.mkdtempSync(path.join(os.tmpdir(), 'OutfileResultsProcessorTest-'));
	});
	afterEach(() => {
		fs.rmSync(tmpDir, {recursive: true, force: true});
	});

	it('csv file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.csv");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.CSV, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy csv contents');
	});

	it('html file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.HTML");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.HTML, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy html contents');
	});

	it('json file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.json");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.JSON, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy json contents');
	});

	it('junit xml file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.xml");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.JUNIT, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy junit contents');
	});

	it('sarif file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.sarif");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.SARIF, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy sarif contents');
	});

	it('xml file', async () => {
		const outfile: string = path.join(tmpDir, "out_file.xMl");
		const resultsProcessor: ResultsProcessor = new OutfileResultsProcessor(OutputFormat.XML, outfile, false);
		await resultsProcessor.processResults(results);
		expect(fs.readFileSync(outfile).toString()).to.equal('dummy xml contents');
	});
});
