package com.example.pmd;

import net.sourceforge.pmd.lang.apex.ast.ASTUserClass;
import net.sourceforge.pmd.lang.apex.rule.AbstractApexRule;

/**
 * This class is an example for how one can write a custom PMD rule for Apex.
 */
public class ExampleCustomRule extends AbstractApexRule {

    /**
     * This is a sample implementation for a `visit()` method. It throws a violation
     * for every Apex class it encounters. Your rule will probably be more interesting than this.
     */
    @Override
    public Object visit(ASTUserClass someClass, Object data) {
        asCtx(data).addViolation(someClass);
        return data;
    }
}
