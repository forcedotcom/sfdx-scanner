import os = require('os');
import path = require('path');

export const SFDX_SCANNER_PATH = path.join(os.homedir(), '.sfdx-scanner');
export const CATALOG_FILE = 'Catalog.json';
export const CUSTOM_PATHS_FILE = 'CustomPaths.json';
export const CONFIG_FILE = 'Config.json';

export const TYPESCRIPT_RULE_PREFIX = '@typescript';

export enum ENGINE {
	PMD = 'pmd',
	ESLINT = 'eslint',
	ESLINT_TYPESCRIPT = 'eslint-typescript'
}

export enum LANGUAGE {
	APEX = 'apex',
	JAVA = 'java',
	JAVASCRIPT = 'javascript',
	PLSQL = 'plsql',
	TYPESCRIPT = 'typescript'
}

export const INTERNAL_ERROR_CODE = 500;
