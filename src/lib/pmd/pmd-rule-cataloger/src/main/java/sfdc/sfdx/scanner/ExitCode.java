package sfdc.sfdx.scanner;

public enum ExitCode {
  WRONG_ARG_COUNT (1),
  MULTIPLE_RULE_DESCRIPTIONS (2),
  RULESET_RECURSION_LIMIT_REACHED (3),
  XML_IO_EXCEPTION (4),
  XML_PARSER_EXCEPTION (5),
  XML_SAXE_EXCEPTION (6),
  JSON_WRITE_EXCEPTION (7),
  NO_SUCH_JAR (8),
  JAR_READ_FAILED (9);


  private final int code;

  ExitCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
