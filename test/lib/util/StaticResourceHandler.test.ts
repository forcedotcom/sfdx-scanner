import 'reflect-metadata';
import {StaticResourceHandler, StaticResourceType} from '../../../src/lib/util/StaticResourceHandler';
import {expect} from 'chai';
import path = require('path');

describe('StaticResourceHandler', () => {
	describe('identifyStaticResourceType', () => {
		it('Text-type static resources are properly identified', async () => {
			const srh = new StaticResourceHandler();
			const paths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'HtmlStaticResource1.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource1.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-e', 'JsStaticResource2.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-g', 'JsResWithOddExt.foo'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-g', 'JsResWithoutExt')
			];

			for (const p of paths) {
				const srType: StaticResourceType = await srh.identifyStaticResourceType(p);
				expect(srType).to.equal(StaticResourceType.TEXT, 'File should be identified as text');
			}
		});

		it('ZIP-type resources are properly identified', async () => {
			const srh = new StaticResourceHandler();
			const paths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileAsResource.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileWithNoExt'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ZipFileWithOddExt.foo')
			];

			for (const p of paths) {
				const srType: StaticResourceType = await srh.identifyStaticResourceType(p);
				expect(srType).to.equal(StaticResourceType.ZIP, 'File should be identified as ZIP');
			}
		});

		it('Other types of resources are properly identified as OTHER', async () => {
			const srh = new StaticResourceHandler();
			const paths = [
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileAsResource.resource'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileWithNoExt'),
				path.resolve('test', 'code-fixtures', 'projects', 'dep-test-app', 'folder-f', 'ImageFileWithOddExt.foo')
			];

			for (const p of paths) {
				const srType: StaticResourceType = await srh.identifyStaticResourceType(p);
				expect(srType).to.equal(StaticResourceType.OTHER, 'File should be identified as OTHER');
			}
		});
	});
});
