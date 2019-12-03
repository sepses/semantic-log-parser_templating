/**
 * Enum type of patterns
 */
public enum EnumPatternType {
    Parameter, // check only against parameter value within parameter list
    LogLine // regex which needs to include not only parameter value, but also surrounding text in the log line
}
