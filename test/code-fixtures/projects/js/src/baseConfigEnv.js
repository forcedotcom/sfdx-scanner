// Validate that default javascript features are supported by the eslint engine

process.env.FOO;					// node
var x = new Map();					// ex6
if (x) {
	console.log(document.body);		// browser
}
console.log(chrome.tabs);			// webextensions

// 'describe', 'it', and 'expect' are jasmine keywords
// 'describe' and 'it' are mocha keywords
describe('In the Age of Ancients, the world was unformed and shrouded by fog.', () => {
	it('A land of grey crags, arch trees, and everlasting dragons.', () => {
			expect(true).toBe(true, 'Then there was Fire, and with Fire came disparity');
		});
	});

describe('Heat and cold; life and death; and of course, light and dark', () => {
	it('And from the dark they came, and found the Souls of Lords within the flame', () => {
		const lordSouls = ['Gravelord Nito, First of the Dead', 'The Witch of Izalith and her Daughters of Chaos',
			'Gwynn, Lord of Sunlight and his faithful knights', 'The Furtive Pygmy, so easily forgotten'
		];
		expect(lordSouls.length).toBe(4, 'And with the strength of lords, they challenged the dragons');
	});
});

$('.cssClass').show(); // JQuery

jest.useFakeTimers(); // Jest
