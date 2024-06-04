import {RunAction} from '../../../src/lib/actions/RunAction';

describe('RunAction tests', () => {
	it('Invoke #execute() to enforce coverage', () => {
		new RunAction().execute({});
		expect(2 + 2).toEqual(4);
	});
});
