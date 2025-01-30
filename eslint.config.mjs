import { FlatCompat } from "@eslint/eslintrc";
import { fixupConfigRules } from "@eslint/compat";
import eslint from '@eslint/js';
import tseslint from 'typescript-eslint';

const compat = new FlatCompat({
	baseDirectory: import.meta.dirname
});

export default [
	...tseslint.config(
		eslint.configs.recommended,
		...tseslint.configs.recommendedTypeChecked,
		{
			languageOptions: {
				parserOptions: {
					projectService: true,
					tsconfigRootDir: import.meta.dirname
				}
			}
		},
		{
			rules: {
				"@typescript-eslint/no-unused-vars": ["error", {
					"argsIgnorePattern": "^_",
					"varsIgnorePattern": "^_",
					"caughtErrorsIgnorePattern": "^_"}],
				"@typescript-eslint/unbound-method": ["error", {"ignoreStatic":  true}]
			}
		}
	),
	...fixupConfigRules(compat.extends("plugin:sf-plugin/recommended")),
	{
		ignores: ["lib/**", "node_modules/**", "github-actions/**"]
	}
];
