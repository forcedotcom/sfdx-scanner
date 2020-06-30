// Validate that default javascript features are supported by the eslint engine

process.env.FOO;					// node
var x = new Map();					// ex6
if (x) {
	console.log(document.body);		// browser
}
console.log(chrome.tabs);			// webextensions