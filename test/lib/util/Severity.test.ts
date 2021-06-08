import {expect} from 'chai';
import { CustomEslintEngine } from '../../../src/lib/eslint/CustomEslintEngine';
import { JavascriptEslintEngine, LWCEslintEngine, TypescriptEslintEngine } from '../../../src/lib/eslint/EslintEngine';
import { CustomPmdEngine, PmdEngine } from '../../../src/lib/pmd/PmdEngine';
import { RetireJsEngine } from '../../../src/lib/retire-js/RetireJsEngine';
import {RuleResult} from '../../../src/types';



describe('normalizeSeverity', () => 
{
    describe('test PmdEngine', () =>
    {
        const testEngine: PmdEngine = new PmdEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 3', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 4', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 5', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test CustomPmdEngine', () =>
    {
        const testEngine: CustomPmdEngine = new CustomPmdEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 3', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 4', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 4,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 5', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 5,
                            url: 'test'
                        },
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'pmd-custom',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                },
                {
                    engine: 'pmd-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                    ]
                }
            ]
            
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test CustomEslintEngine', () =>
    {
        const testEngine: CustomEslintEngine = new CustomEslintEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-custom',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test JavascriptEslintEngine', () =>
    {
        const testEngine: JavascriptEslintEngine = new JavascriptEslintEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test LWCEslintEngine', () =>
    {
        const testEngine: LWCEslintEngine = new LWCEslintEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-lwc',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-lwc',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-lwc',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-lwc',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test TypescriptEslintEngine', () =>
    {
        const testEngine: TypescriptEslintEngine = new TypescriptEslintEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-typescript',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-typescript',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'eslint-typescript',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'eslint-typescript',
                    fileName: 'test',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 12,
                            endColumn: 17,
                            endLine: 11,
                            line: 11,
                            message: "test",
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]
    
            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );

    describe('test RetireJsEngine', () =>
    {
        const testEngine: RetireJsEngine = new RetireJsEngine();

        it('test severity value 1', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 1,
                            url: 'test'
                        }
                    ]
                }
            ]

            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 2', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 2,
                            url: 'test'
                        }
                    ]
                }
            ]

            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );

        it('test severity value 3', async () =>
        {
            const inputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                }
            ]

            const expectedOutputResults:RuleResult[] = [
                {
                    engine: 'retire-js',
                    fileName: 'filename',
                    violations: [
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        },
                        {
                            category: 'test',
                            column: 27,
                            endColumn: 27,
                            endLine: 1,
                            line: 1,
                            message: 'test',
                            ruleName: 'test',
                            severity: 3,
                            url: 'test'
                        }
                    ]
                }
            ]

            const normalizedResults = await testEngine.normalizeSeverity(inputResults);
            expect(normalizedResults).to.eql(expectedOutputResults);
        }
        );
    }
    );
}
);
