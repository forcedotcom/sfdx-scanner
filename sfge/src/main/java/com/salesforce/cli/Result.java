package com.salesforce.cli;

import com.salesforce.rules.Violation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** Represents the result of an SFGE execution. */
public class Result {

    /** Violations detected during an execution * */
    private final List<Violation> violations;

    /** Exceptions that interrupted the execution * */
    private final List<Throwable> errorsThrown;

    /** Indicates if the execution completed successfully * */
    private boolean completedSuccessfully;

    public Result() {
        this.violations = new ArrayList<>();
        this.errorsThrown = new ArrayList<>();
        this.completedSuccessfully = false;
    }

    public void merge(Result result) {
        this.addViolations(result.getViolations());
        this.addThrowable(result.getErrorsThrown());
    }

    public void addViolations(Collection<Violation> violations) {
        this.violations.addAll(violations);
    }

    public void addViolation(Violation violation) {
        this.violations.add(violation);
    }

    public void setCompletedSuccessfully() {
        this.completedSuccessfully = true;
    }

    public void addThrowable(Collection<? extends Throwable> exceptions) {
        this.errorsThrown.addAll(exceptions);
    }

    public void addThrowable(Throwable ex) {
        this.errorsThrown.add(ex);
    }

    public List<Violation> getViolations() {
        return violations;
    }

    public boolean isCompletedSuccessfully() {
        return completedSuccessfully;
    }

    public List<Throwable> getErrorsThrown() {
        return errorsThrown;
    }
}
