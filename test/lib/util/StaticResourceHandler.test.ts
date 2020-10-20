import 'reflect-metadata';
import {StaticResourceHandler, StaticResourceType} from '../../../src/lib/util/StaticResourceHandler';
import {FileHandler} from '../../../src/lib/util/FileHandler';
import {expect} from 'chai';
import Sinon = require('sinon');

describe('StaticResourceHandler', () => {
	describe('identifyStaticResourceType', () => {
		describe('JS-type resources', () => {
			const fakeJsMeta = `<?xml version="1.0" encoding="UTF-8"?>\n<StaticResource xmlns="http://soap.sforce.com/2006/04/metadata">\n\t<cacheControl>Public</cacheControl>
\t<contentType>text/javascript</contentType>\n\t<description>JsStaticResource1</description>\n</StaticResource>`;

			before(() => {
				Sinon.createSandbox();
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'readFile').resolves(fakeJsMeta);
			});

			after(() => {
				Sinon.restore();
			});

			it('Properly identifies JS-type resources', async () => {
				const srh = new StaticResourceHandler();
				const res: StaticResourceType = await srh.identifyStaticResourceType('meaningless/path/to/file.resource');
				expect(res).to.equal(StaticResourceType.JS, 'Resource should be recognized as JS-type');
			});
		});

		describe('ZIP-type resources', () => {
			const fakeZipMeta = `<?xml version="1.0" encoding="UTF-8"?>\n<StaticResource xmlns="http://soap.sforce.com/2006/04/metadata">\n\t<cacheControl>Public</cacheControl>
\t<contentType>application/zip</contentType>\n\t<description>ZipStaticResource1</description>\n</StaticResource>`;

			before(() => {
				Sinon.createSandbox();
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'readFile').resolves(fakeZipMeta);
			});

			after(() => {
				Sinon.restore();
			});

			it('Properly identifies ZIP-type resources', async () => {
				const srh = new StaticResourceHandler();
				const res: StaticResourceType = await srh.identifyStaticResourceType('meaningless/path/to/file.resource');
				expect(res).to.equal(StaticResourceType.ZIP, 'Resource should be recognized as ZIP-type');
			});
		});

		describe('Other resource types', () => {
			const fakeHtmlMeta = `<?xml version="1.0" encoding="UTF-8"?>\n<StaticResource xmlns="http://soap.sforce.com/2006/04/metadata">\n\t<cacheControl>Public</cacheControl>
\t<contentType>text/html</contentType>\n\t<description>HtmlStaticResource1</description>\n</StaticResource>`;

			before(() => {
				Sinon.createSandbox();
				Sinon.stub(FileHandler.prototype, 'exists').resolves(true);
				Sinon.stub(FileHandler.prototype, 'readFile').resolves(fakeHtmlMeta);
			});

			after(() => {
				Sinon.restore();
			});

			it('Properly identifies other resources as OTHER', async () => {
				const srh = new StaticResourceHandler();
				const res: StaticResourceType = await srh.identifyStaticResourceType('meaningless/path/to/file.resource');
				expect(res).to.equal(StaticResourceType.OTHER, 'Resource should be recognized as OTHER');
			});
		});

		describe('Non-resource files', () => {
			before(() => {
				Sinon.createSandbox();
				Sinon.stub(FileHandler.prototype, 'exists').resolves(false);
			});

			after(() => {
				Sinon.restore();
			});

			it('When no resource-meta.xml file can be found, returns null', async () => {
				const srh = new StaticResourceHandler();
				const res: StaticResourceType = await srh.identifyStaticResourceType('meaningless/path/to/file.resource');
				expect(res).to.equal(null, 'Null value returned when no meta file exists');
			});
		});










	});
})
