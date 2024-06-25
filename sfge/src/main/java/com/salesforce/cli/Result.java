package com.salesforce.cli;

import com.salesforce.rules.Violation;
import java.util.*;
import java.util.stream.Collectors;

/** Represents the result of an SFGE execution. */
public class Result {

    /** Violations detected during an execution * */
    private final List<Violation> violations;

    /** Exceptions that interrupted the execution * */
    private final List<Throwable> errorsThrown;

    /** Indicates if the execution completed successfully * */
    private boolean completedSuccessfully;

    /** Mapping of file names to entry points that lead to paths that traverse these paths * */
    private final FilesToEntriesMap filesToEntriesMap;

    public Result() {
        this.violations = new ArrayList<>();
        this.errorsThrown = new ArrayList<>();
        this.completedSuccessfully = false;
        this.filesToEntriesMap = new FilesToEntriesMap();
    }

    public void merge(Result result) {
        this.addViolations(result.violations);
        this.addThrowable(result.errorsThrown);
        this.filesToEntriesMap.merge(result.filesToEntriesMap);
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

    public void addFileToEntryPoint(String filename, String entryFile, String entryMethod) {
        this.filesToEntriesMap.put(filename, entryFile, entryMethod);
    }

    public List<Violation> getOrderedViolations() {
        final TreeSet<Violation> orderedViolations = new TreeSet<>();
        orderedViolations.addAll(violations);
        return orderedViolations.stream().collect(Collectors.toList());
    }

    public boolean isCompletedSuccessfully() {
        return completedSuccessfully;
    }

    public List<Throwable> getErrorsThrown() {
        return errorsThrown;
    }

    public FilesToEntriesMap getFilesToEntriesMap() {
        return this.filesToEntriesMap;
    }
}
