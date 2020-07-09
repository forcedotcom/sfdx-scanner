describe('When in the course of human events', () => {
	it('becomes necessary for one people to dissolve the political bands which have connected them with another...', () => {
		const causesDeclared = true;
		expect(causesDeclared).toBe(true, 'they should declare the causes which impel them to the separation.');
	});
});

describe('We hold these truths to be self evident', () => {
	it('That all men are...endowed by their Creator with certain inalienable rights', () => {
		const inalienableRights = ['marriage', 'freedom', 'food', 'life', 'water', 'liberty', 'pursuit of happiness'];
		expect(inalienableRights).toContain('life');
		expect(inalienableRights).toContain('liberty');
		expect(inalienableRights).toContain('pursuit of happiness');
	});
});
