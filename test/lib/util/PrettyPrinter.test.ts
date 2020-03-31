import {expect} from 'chai';
import * as PrettyPrinter from '../../../src/lib/util/PrettyPrinter';

describe(('PrettyPrinter tests'), () => {
    it('should print Set<string>', () => {
        const { setOfString, expectedString } = createSet();
        expect(PrettyPrinter.stringifySet(setOfString)).equals(expectedString);
    });

    it('should print Map<string, Set<string>>', () => {
        const { mapOfSet, expectedMapString } = createMapOfSet();
        expect(PrettyPrinter.stringifyMapSet(mapOfSet)).equals(expectedMapString);
    });

    it('should print Map<string, Map<string, Set<string>>>', () => {
        const { mapOfMapOfSet, expectedMapOfMapString } = createMapOfMapOfSet();
        expect(PrettyPrinter.stringifyMapOfMap(mapOfMapOfSet)).equals(expectedMapOfMapString);
    });

    // TODO: add tests for RuleFilter stringify
});

function createSet() {
    const setOfString = new Set<string>();
    setOfString.add('value1');
    setOfString.add('value2');
    setOfString.add('value3');
    const expectedString = '[value1,value2,value3]';
    return { setOfString, expectedString };
}

function createMapOfSet() {
    const mapOfSet = new Map<string, Set<string>>();
    const { setOfString, expectedString } = createSet();
    mapOfSet.set('key1', setOfString);
    mapOfSet.set('key2', setOfString);
    const expectedMapString = `{key1 => ${expectedString}},{key2 => ${expectedString}}`;
    return { mapOfSet, expectedMapString };
}

function createMapOfMapOfSet() {
    const mapOfMapOfSet = new Map<string, Map<string, Set<string>>>();
    const { mapOfSet, expectedMapString } = createMapOfSet();
    mapOfMapOfSet.set('topKey1', mapOfSet);
    const expectedMapOfMapString = `{topKey1 => ${expectedMapString}}`
    return { mapOfMapOfSet, expectedMapOfMapString };
}

