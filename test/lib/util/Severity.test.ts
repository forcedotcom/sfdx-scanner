import {expect} from 'chai';
import { ENGINE } from '../../../src/Constants';
import {CustomEslintEngine} from '../../../src/lib/eslint/CustomEslintEngine';
import {JavascriptEslintEngine, LWCEslintEngine, TypescriptEslintEngine} from '../../../src/lib/eslint/EslintEngine';
import {CustomPmdEngine, PmdEngine} from '../../../src/lib/pmd/PmdEngine';
import {RetireJsEngine} from '../../../src/lib/retire-js/RetireJsEngine';

describe('normalizeSeverity', () => 
{
    describe('test PmdEngine', () =>
    {
        const testEngine: PmdEngine = new PmdEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.PMD)).to.equal(1);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.PMD)).to.equal(2);
        }
        );

        it('test severity value 3', () =>
        {
            expect(testEngine.getNormalSeverity(3, ENGINE.PMD)).to.equal(3);
        }
        );

        it('test severity value 4', () =>
        {
            expect(testEngine.getNormalSeverity(4, ENGINE.PMD)).to.equal(3);
        }
        );

        it('test severity value 5', () =>
        {
            expect(testEngine.getNormalSeverity(5, ENGINE.PMD)).to.equal(3);
        }
        );
    }
    );

    describe('test CustomPmdEngine', () =>
    {
        const testEngine: CustomPmdEngine = new CustomPmdEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.PMD_CUSTOM)).to.equal(1);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.PMD_CUSTOM)).to.equal(2);
        }
        );

        it('test severity value 3', () =>
        {
            expect(testEngine.getNormalSeverity(3, ENGINE.PMD_CUSTOM)).to.equal(3);
        }
        );

        it('test severity value 4', () =>
        {
            expect(testEngine.getNormalSeverity(4, ENGINE.PMD_CUSTOM)).to.equal(3);
        }
        );

        it('test severity value 5', () =>
        {
            expect(testEngine.getNormalSeverity(5, ENGINE.PMD_CUSTOM)).to.equal(3);
        }
        );
    }
    );

    describe('test CustomEslintEngine', () =>
    {
        const testEngine: CustomEslintEngine = new CustomEslintEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.ESLINT_CUSTOM)).to.equal(2);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.ESLINT_CUSTOM)).to.equal(1);
        }
        );
    }
    );

    describe('test JavascriptEslintEngine', () =>
    {
        const testEngine: JavascriptEslintEngine = new JavascriptEslintEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.ESLINT)).to.equal(2);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.ESLINT)).to.equal(1);
        }
        );
    }
    );

    describe('test LWCEslintEngine', () =>
    {
        const testEngine: LWCEslintEngine = new LWCEslintEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.ESLINT_LWC)).to.equal(2);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.ESLINT_LWC)).to.equal(1);
        }
        );
    }
    );

    describe('test TypescriptEslintEngine', () =>
    {
        const testEngine: TypescriptEslintEngine = new TypescriptEslintEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.ESLINT_TYPESCRIPT)).to.equal(2);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.ESLINT_TYPESCRIPT)).to.equal(1);
        }
        );
    }
    );

    describe('test RetireJsEngine', () =>
    {
        const testEngine: RetireJsEngine = new RetireJsEngine();

        it('test severity value 1', () =>
        {
            expect(testEngine.getNormalSeverity(1, ENGINE.RETIRE_JS)).to.equal(1);
        }
        );

        it('test severity value 2', () =>
        {
            expect(testEngine.getNormalSeverity(2, ENGINE.RETIRE_JS)).to.equal(2);
        }
        );

        it('test severity value 3', () =>
        {
            expect(testEngine.getNormalSeverity(3, ENGINE.RETIRE_JS)).to.equal(3);
        }
        );
    }
    );
}
);
