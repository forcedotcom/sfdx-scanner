import {SfdxError} from '@salesforce/core';
import {FileHandler} from './FileHandler';
import {ElementCompact, xml2js} from 'xml-js';


export enum StaticResourceType {
	ZIP = 'zip',
	JS = 'js',
	OTHER = 'other'
}

interface StaticResourceJson extends ElementCompact  {
	StaticResource: {
		contentType: {
			_text: string;
		};
	};
}

export class StaticResourceHandler {

	private resultCache: Map<string, StaticResourceType>;

	constructor() {
		this.resultCache = new Map();
	}

	private isStaticResourceJson(fileContents: ElementCompact): fileContents is StaticResourceJson {
		return !!(fileContents.StaticResource
			&& fileContents.StaticResource.contentType
			&& fileContents.StaticResource.contentType._text);

	}

	public async identifyStaticResourceType(filename: string): Promise<StaticResourceType> {
		// If we've already cached a value for this file, we can return early.
		if (this.resultCache.has(filename)) {
			return this.resultCache.get(filename);
		}
		// Otherwise, we need to do some work.
		const fh = new FileHandler();
		const metafileName = filename + '-meta.xml';

		// If there's no corresponding `-meta.xml` file, we should assume that this isn't a static resource at all.
		// Rather than throwing an error, we'll just return null. That way, upstream callers can decide whether to throw
		// an error.
		if (!await fh.exists(metafileName)) {
			this.resultCache.set(filename, null);
			return null;
		}
		// If there's a `-meta.xml` file, we should read it and parse it into a JSON.
		let metaContents: string = null;
		let metaJson: ElementCompact = null;

		try {
			// If there's a `-meta.xml` file, we should read it and parse it into a JSON.
			metaContents = await fh.readFile(metafileName);
			metaJson = xml2js(metaContents, {compact: true, ignoreDeclaration: true});
		} catch (e) {
			// We'll throw an error here, because it's always a problem if something that looks like a static resource
			// can't be processed.
			throw new SfdxError(`Could not parse ${metafileName} into JSON: ${e.message || e}`);
		}

		if (!this.isStaticResourceJson(metaJson)) {
			// We'll throw an error here, because it's always a problem if something that looks like a static resource
			// can't be processed.
			throw new SfdxError(`${metafileName} did not match expected format`);
		}

		// use the MIME type to determine what type of static resource we're looking at.
		switch (metaJson.StaticResource.contentType._text) {
			case 'application/zip':
			case 'application/x-zip-compressed':
				this.resultCache.set(filename, StaticResourceType.ZIP);
				break;
			case 'application/javascript':
			case 'text/javascript':
			case 'application/x-javascript':
				this.resultCache.set(filename, StaticResourceType.JS);
				break;
			default:
				this.resultCache.set(filename, StaticResourceType.OTHER);
		}
		return this.resultCache.get(filename);
	}
}
