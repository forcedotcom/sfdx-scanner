module.exports = {
	parser: "@typescript-eslint/parser",
	extends: [
		"eslint:recommended",
		"plugin:@typescript-eslint/recommended",
		"plugin:@typescript-eslint/recommended-requiring-type-checking",
		"plugin:sf-plugin/migration"
	],
	parserOptions: {
		"sourceType": "module",
		"ecmaVersion": 2018,
		"project": "./tsconfig.json",
		"tsconfigRootDir": __dirname
	},
	plugins: [
		"@typescript-eslint"
	]
}
