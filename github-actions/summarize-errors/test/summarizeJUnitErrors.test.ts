import { expect } from "chai";
import {summarizeErrors} from "../lib/summarizeJUnitErrors";

describe("#summarizeErrors", () => {
	it('does the thing', async () => {
		const t = await summarizeErrors("/Users/jfeingold/Code/sfdx-scanner/sfge");
		console.log(t);
		expect(t).to.exist;
	});
});
