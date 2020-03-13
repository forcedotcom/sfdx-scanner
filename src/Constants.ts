import os = require('os');
import path = require('path');

export const SFDX_SCANNER_PATH = path.join(os.homedir(), '.sfdx-scanner');
export const PMD_CATALOG = 'PmdCatalog.json';
export const CUSTOM_PATHS = 'CustomPaths.json';
export const CONFIG = 'Config.json';