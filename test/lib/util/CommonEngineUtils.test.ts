import { expect } from 'chai';
import * as engineUtils from '../../../src/lib/util/CommonEngineUtils';

describe('Tests for CommonEngineUtils', () => {
	describe('#isValueInFilter', () => {
		it('should detect if value is present in filter values', () => {
			const value = 'value1';
			const filterValues = ['value3', 'value1', 'value2'];
			expect(engineUtils.isValueInFilter(value, filterValues)).is.true;
		});

		it('should detect if value is not present in filter values', () => {
			const value = 'value5';
			const filterValues = ['value3', 'value1', 'value2'];
			expect(engineUtils.isValueInFilter(value, filterValues)).is.false;
		});
	});

	describe('#anyFilterValueStartsWith', () => {
		it('should detect if filter values starts with a given string', () => {
			const value = 'abc';
			const filterValues = ['xyz5', 'abc67', 'pqr89'];
			expect(engineUtils.anyFilterValueStartsWith(value, filterValues)).is.true;
		});

		it('should detect if none of the filter values start with a given string', () => {
			const value = 'hijk';
			const filterValues = ['xyz5', 'abc67', 'pqr89'];
			expect(engineUtils.isValueInFilter(value, filterValues)).is.false;
		});
	});

	describe('#isFilterEmptyOrNameInFilter', () => {
		it('should always return true if filter values list is empty', () => {
			const value = 'value1';
			const filterValues = [];
			expect(engineUtils.isFilterEmptyOrNameInFilter(value, filterValues)).is.true;
		});

		it('should check if value is present in the list if list is non empty', () => {
			const value1 = 'value1';
			const value2 = 'value2'
			const filterValues = ['value1', 'value3'];
			expect(engineUtils.isFilterEmptyOrNameInFilter(value1, filterValues)).is.true;
			expect(engineUtils.isFilterEmptyOrNameInFilter(value2, filterValues)).is.false;
		});
	});

	describe('#isFilterEmptyOrFilterValueStartsWith', () => {
		it('should always return true if filter values list is empty', () => {
			const value = 'abc';
			const filterValues = [];
			expect(engineUtils.isFilterEmptyOrFilterValueStartsWith(value, filterValues)).is.true;
		});

		it('should check if any of the filter values start with given string in the list if list is non empty', () => {
			const value1 = 'abc';
			const value2 = 'xyz'
			const filterValues = ['abc123', 'pqr678'];
			expect(engineUtils.isFilterEmptyOrFilterValueStartsWith(value1, filterValues)).is.true;
			expect(engineUtils.isFilterEmptyOrFilterValueStartsWith(value2, filterValues)).is.false;
		});
	});

	describe('#isCustomRun', () => {
		it('should return true of value exists in map', () => {
			const config = 'some config';
			const engineOptions = new Map<string, string>([
				['some config', 'configvalue']
			]);

			expect(engineUtils.isCustomRun(config, engineOptions)).is.true;
		});

		it('should return true of value exists in map', () => {
			const config = 'not me';
			const engineOptions = new Map<string, string>([
				['some config', 'configvalue']
			]);

			expect(engineUtils.isCustomRun(config, engineOptions)).is.false;
		});
	});
});