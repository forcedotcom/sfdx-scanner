import os = require('os');
import path = require('path');

// Keep this in sync with <repoRoot>/pmd7/build.gradle.kts > pmd7Version
export const PMD7_VERSION = '7.7.0';

export const PMD_APPEXCHANGE_RULES_VERSION = '0.16';

// Keep this in sync with <repoRoot>/sfge/build.gradle.kts > version
export const SFGE_VERSION = '1.0.1-pilot';

export const DEFAULT_SCANNER_PATH = path.join(os.homedir(), '.sfdx-scanner');
export const CATALOG_FILE = 'Catalog.json';
export const CUSTOM_PATHS_FILE = 'CustomPaths.json';
export const CONFIG_PILOT_FILE = 'Config-pilot.json';
export const CONFIG_FILE = 'Config.json';

// TODO: We should flesh this one-off solution out into one that handles all the various env vars we use.
//       E.g., the ones defined in `EnvironmentVariable.ts` and `dfa.ts`.
export const ENV_VAR_NAMES = {
	SCANNER_PATH_OVERRIDE: 'SCANNER_PATH_OVERRIDE',
	SCANNER_INTERNAL_OUTFILE: 'SCANNER_INTERNAL_OUTFILE'
};

export const INTERNAL_ERROR_CODE = 1;

export enum ENGINE {
	PMD = 'pmd',
	PMD_APPEXCHANGE = 'pmd-appexchange',
	PMD_CUSTOM = 'pmd-custom',
	ESLINT = 'eslint',
	ESLINT_LWC = 'eslint-lwc',
	ESLINT_TYPESCRIPT = 'eslint-typescript',
	ESLINT_CUSTOM = 'eslint-custom',
	RETIRE_JS = 'retire-js',
	CPD = 'cpd',
	SFGE = 'sfge'
}

export enum RuleType {
	PATHLESS = "pathless",
	DFA = "dfa"
}

export enum TargetType {
	FILE,
	DIRECTORY,
	GLOB
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
 * (e.g., `scanner rule list`).
 */
export const AllowedEngineFilters = [
	ENGINE.ESLINT,
	ENGINE.ESLINT_LWC,
	ENGINE.ESLINT_TYPESCRIPT,
	ENGINE.PMD,
	ENGINE.PMD_APPEXCHANGE,
	ENGINE.RETIRE_JS,
	ENGINE.CPD,
	ENGINE.SFGE
]

/**
 * These are the filter values that can be used with the --engine flag in contexts where only non-path engines should be
 * available.
 * (e.g., `scanner run`).
 */
export const PathlessEngineFilters = [
	ENGINE.ESLINT,
	ENGINE.ESLINT_LWC,
	ENGINE.ESLINT_TYPESCRIPT,
	ENGINE.PMD,
	ENGINE.PMD_APPEXCHANGE,
	ENGINE.RETIRE_JS,
	ENGINE.SFGE,
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
	},
	FILE_IGNORED: {
		name: 'file-ignored',
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
export const PMD7_LIB = path.join(__dirname, '..', 'dist', 'pmd7', 'lib');

// Here, current dir __dirname = <base_dir>/sfdx-scanner/src
export const APPEXCHANGE_PMD_LIB = path.join(__dirname, '..', 'pmd-appexchange', 'lib');
