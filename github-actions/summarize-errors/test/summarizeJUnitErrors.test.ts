import { expect } from "chai";
import {summarizeErrors} from "../lib/summarizeJUnitErrors";

describe("#summarizeErrors", () => {
	it('does the thing', async () => {
		const t = summarizeErrors("/Users/jfeingold/Code/sfdx-scanner/sfge");
		expect(t).to.exist;
	});
});
