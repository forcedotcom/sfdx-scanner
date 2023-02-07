import {SfError} from '@salesforce/core';
import {FileHandler} from './FileHandler';
import {isBinaryFileSync} from 'isbinaryfile';
import isZip = require('is-zip');


export enum StaticResourceType {
	ZIP = 'zip',
	TEXT = 'text',
	OTHER = 'other'
}

export class StaticResourceHandler {

	private resultCache: Map<string, StaticResourceType>;

	constructor() {
		this.resultCache = new Map();
	}

	public async identifyStaticResourceType(filename: string): Promise<StaticResourceType> {
		// If we've already cached a value for this file, we can return early.
		if (this.resultCache.has(filename)) {
			return this.resultCache.get(filename);
		}
		// Otherwise, we need to do some work. Spin up a FileHandler and use it to get the contents of the file as a buffer.
		const fh = new FileHandler();
		let buffer: Buffer = null;
		try {
			buffer = await fh.readFileAsBuffer(filename);
		} catch (e) {
			// We'll throw an error here, because being unable to parse a static resource is definitely a problem that
			// should be surfaced to the user.
			const message: string = e instanceof Error ? e.message : e as string;
			throw new SfError(`Could not read ${filename}: ${message}`);
		}

		const fileType: StaticResourceType = this.identifyBufferType(buffer);
		this.resultCache.set(filename, fileType);
		return fileType;
	}

	public identifyBufferType(buffer: Buffer): StaticResourceType {
		// There's no typing available for the `is-zip` package, but we know that this method accepts a buffer and returns
		// a boolean. So we'll just ignore the linting rule that complains about it.
		// eslint-disable-next-line @typescript-eslint/no-unsafe-call
		if (isZip(buffer)) {
			return StaticResourceType.ZIP;
		} else if (!isBinaryFileSync(buffer)) {
			return StaticResourceType.TEXT;
		} else {
			return StaticResourceType.OTHER;
		}
	}
}
