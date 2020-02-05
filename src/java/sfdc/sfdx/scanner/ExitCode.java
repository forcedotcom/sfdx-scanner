package sfdc.sfdx.scanner;

public enum ExitCode {
  PMD_WRONG_ARG_COUNT (1),
  PMD_MULTIPLE_RULE_DESCRIPTIONS (2),
  PMD_RULESET_RECURSION_LIMIT_REACHED (3),
  XML_IO_EXCEPTION (4),
  XML_PARSER_EXCEPTION (5),
  XML_SAXE_EXCEPTION (6),
  JSON_WRITE_EXCEPTION (7),
  PMD_NO_SUCH_JAR (8),
  PMD_JAR_READ_FAILED (9);


  private final int code;

  ExitCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
