package sfdc.sfdx.scanner;

public enum ExitCode {
  MAIN_INVALID_ARGUMENT(1),
  PMD_MULTIPLE_RULE_DESCRIPTIONS (2),
  PMD_RULESET_RECURSION_LIMIT_REACHED (3),
  XML_IO_EXCEPTION (4),
  XML_PARSER_EXCEPTION (5),
  XML_SAXE_EXCEPTION (6),
  JSON_WRITE_EXCEPTION (7),
  JAR_READ_FAILED(8),
  DIRECTORY_READ_EXCEPTION(9),
  LANGUAGE_MISSING_ERROR(10),
  CLASSPATH_DOES_NOT_EXIST(11);

  private final int code;

  ExitCode(int code) {
    this.code = code;
  }

  public int getCode() {
    return this.code;
  }
}
