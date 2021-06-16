import os = require('os');
import path = require('path');

export const PMD_VERSION = '6.34.0';
export const CATALOG_FILE = 'Catalog.json';
export const CUSTOM_PATHS_FILE = 'CustomPaths.json';
export const CONFIG_FILE = 'Config.json';
export const PMD_CATALOG_FILE = 'PmdCatalog.json';
export const INTERNAL_ERROR_CODE = 500;

export interface EnvOverridable {
	getSfdxScannerPath(): string;
}

export class ProdOverrides implements EnvOverridable {
	public getSfdxScannerPath(): string {
		return path.join(os.homedir(), '.sfdx-scanner');
	}
}

export enum ENGINE {
	PMD = 'pmd',
	PMD_CUSTOM = 'pmd-custom',
	ESLINT = 'eslint',
	ESLINT_LWC = 'eslint-lwc',
	ESLINT_TYPESCRIPT = 'eslint-typescript',
	ESLINT_CUSTOM = 'eslint-custom',
	RETIRE_JS = 'retire-js'
}

/**
 * Main engine types that have more than one variation
 */
export const EngineBase = {
	PMD: 'pmd',
	ESLINT: 'eslint'
}

/**
 * These are the filter values that Users can filter by when using
 * --engine flag
 */
export const AllowedEngineFilters = [
	ENGINE.ESLINT,
	ENGINE.ESLINT_LWC,
	ENGINE.ESLINT_TYPESCRIPT,
	ENGINE.PMD,
	ENGINE.RETIRE_JS
]


export enum LANGUAGE {
	APEX = 'apex',
	JAVA = 'java',
	JAVASCRIPT = 'javascript',
	TYPESCRIPT = 'typescript',
	VISUALFORCE = 'vf'
}

export const Services = {
	Config: "Config",
	RuleManager: "RuleManager",
	RuleEngine: "RuleEngine",
	RuleCatalog: "RuleCatalog",
	RulePathManager: "RulePathManager",
	EnvOverridable: "EnvOverridable"
};

export enum CUSTOM_CONFIG {
	EslintConfig = "EslintConfig",
	PmdConfig = "PmdConfig"
}

export const HARDCODED_RULES = {
	FILES_MUST_COMPILE: {
		name: 'files-must-compile',
		category: 'Scanner Internal'
	}
};

export enum Severity {
	NONE = 0,
	LOW = 3,
	MODERATE = 2,
	HIGH = 1
}

export const severityMap = new Map([
	[ENGINE.PMD.toString(), new Map([[1, Severity.HIGH],[2, Severity.MODERATE],[3, Severity.LOW],[4, Severity.LOW],[5, Severity.LOW]])],
    [ENGINE.PMD_CUSTOM.toString(), new Map([[1, Severity.HIGH],[2, Severity.MODERATE],[3, Severity.LOW],[4, Severity.LOW],[5, Severity.LOW]])],
	[ENGINE.ESLINT.toString(), new Map([[1, Severity.MODERATE],[2, Severity.HIGH]])],
	[ENGINE.ESLINT_LWC.toString(), new Map([[1, Severity.MODERATE],[2, Severity.HIGH]])],
	[ENGINE.ESLINT_TYPESCRIPT.toString(), new Map([[1, Severity.MODERATE],[2, Severity.HIGH]])],
	[ENGINE.ESLINT_CUSTOM.toString(), new Map([[1, Severity.MODERATE],[2, Severity.HIGH]])],
	[ENGINE.RETIRE_JS.toString(), new Map([[1, Severity.HIGH],[2, Severity.MODERATE],[3, Severity.LOW]])]
]);
