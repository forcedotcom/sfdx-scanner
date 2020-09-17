import os = require('os');
import path = require('path');

export interface EnvOverridable {
	getSfdxScannerPath(): string;
}

export class ProdOverrides implements EnvOverridable {
	public getSfdxScannerPath(): string {
		return path.join(os.homedir(), '.sfdx-scanner');
	}
}

export const CATALOG_FILE = 'Catalog.json';
export const CUSTOM_PATHS_FILE = 'CustomPaths.json';
export const CONFIG_FILE = 'Config.json';
export const PMD_CATALOG_FILE = 'PmdCatalog.json';

export enum ENGINE {
	PMD = 'pmd',
	ESLINT = 'eslint',
	ESLINT_LWC = 'eslint-lwc',
	ESLINT_TYPESCRIPT = 'eslint-typescript'
}

export enum DEPCHECK {
	RETIRE_JS = 'retire-js'
}

export enum LANGUAGE {
	APEX = 'apex',
	JAVA = 'java',
	JAVASCRIPT = 'javascript',
	PLSQL = 'plsql',
	TYPESCRIPT = 'typescript'
}

export enum OUTPUT_FORMAT {
	CSV = 'csv',
	HTML = 'html',
	JSON = 'json',
	JUNIT = 'junit',
	TABLE = 'table',
	XML = 'xml'
}

export const Services = {
	Config: "Config",
	DependencyChecker: "DependencyChecker",
	EnvOverridable: "EnvOverridable",
	RuleCatalog: "RuleCatalog",
	RuleEngine: "RuleEngine",
	RuleManager: "RuleManager",
	RulePathManager: "RulePathManager"
};

export const INTERNAL_ERROR_CODE = 500;
