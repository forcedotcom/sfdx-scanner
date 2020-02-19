package sfdc.sfdx.scanner.pmd;

/**
 * Custom exceptions for PMD-based scanner to handle errors in a special way
 */
public class ScannerPmdException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
    public ScannerPmdException(String message) {
        super(message);
    }

    public ScannerPmdException(String message, Throwable throwable) {
        super(message, throwable);
    }
}