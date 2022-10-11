import os = require('os');
import path = require('path');

export const PMD_VERSION = '6.50.0';
export const SFGE_VERSION = '1.0.1-pilot';
export const CATALOG_FILE = 'Catalog.json';
export const CUSTOM_PATHS_FILE = 'CustomPaths.json';
export const CONFIG_PILOT_FILE = 'Config-pilot.json';
export const CONFIG_FILE = 'Config.json';
export const PMD_CATALOG_FILE = 'PmdCatalog.json';

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
	RETIRE_JS = 'retire-js',
	CPD = 'cpd',
	SFGE = 'sfge'
}

/**
 * Main engine types that have more than one variation
 */
export const EngineBase = {
	PMD: 'pmd',
	ESLINT: 'eslint'
}

/**
 * These are the filter values that can be used with the --engine flag in contexts where all engines are available.
 * (e.g., `scanner:rule:list`).
 */
export const AllowedEngineFilters = [
	ENGINE.ESLINT,
	ENGINE.ESLINT_LWC,
	ENGINE.ESLINT_TYPESCRIPT,
	ENGINE.PMD,
	ENGINE.RETIRE_JS,
	ENGINE.CPD,
	ENGINE.SFGE
]

/**
 * These are the filter values that can be used with the --engine flag in contexts where only non-path engines should be
 * available.
 * (e.g., `scanner:run`).
 */
export const PathlessEngineFilters = [
	ENGINE.ESLINT,
	ENGINE.ESLINT_LWC,
	ENGINE.ESLINT_TYPESCRIPT,
	ENGINE.PMD,
	ENGINE.RETIRE_JS,
	ENGINE.CPD
]

export const DfaEngineFilters = [
	ENGINE.SFGE
]


export enum LANGUAGE {
	APEX = 'apex',
	JAVA = 'java',
	JAVASCRIPT = 'javascript',
	TYPESCRIPT = 'typescript',
	VISUALFORCE = 'visualforce',
	ECMASCRIPT = 'ecmascript',
	XML = 'xml'
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
	PmdConfig = "PmdConfig",
	SfgeConfig = "SfgeConfig",
	VerboseViolations = "VerboseViolations"
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

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src
export const PMD_LIB = path.join(__dirname, '..', 'dist', 'pmd', 'lib');
