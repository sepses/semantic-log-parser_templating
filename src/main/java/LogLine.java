import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representation of a logline
 */
public class LogLine {

    public Integer LineId;
    public String EventMonth;
    public String EventDay;
    public String EventTime;
    public String Level;
    public String Component;
    public String Content;
    public String EventId;
    public String EventTemplate;
    public List<Pair> ParameterList;

    public LogLine(String LineId, String EventMonth, String EventDay, String EventTime, String Level,
            String Component, String Content, String EventId, String EventTemplate, String ParameterString) {
        this.LineId = Integer.parseInt(LineId);
        this.EventMonth = EventMonth;
        this.EventDay = EventDay;
        this.EventTime = EventTime;
        this.Level = Level;
        this.Component = Component.replaceAll("[\\[\\]]", "");
        this.Content = Content;
        this.EventId = EventId;
        this.EventTemplate = EventTemplate;
        this.ParameterList = splitParameter(ParameterString);
    }

    public static LogLine createFromLogline(CSVRecord logLine) {
        return new LogLine(logLine.get(0), logLine.get(1), logLine.get(2), logLine.get(3), logLine.get(4),
                logLine.get(5), logLine.get(6), logLine.get(7), logLine.get(8), logLine.get(9));
    }

    public static List<Pair> splitParameter(String paramList) {

        List<Pair> finalParams = new ArrayList<>();
        paramList = cleanString(paramList);
        if (!paramList.isEmpty()) {

            String otherThanQuote = " [^'] ";
            String quotedString = String.format(" ' %s* ' ", otherThanQuote);
            String regex = String.format("(?x) " + // enable comments, ignore white spaces
                            ",                         " + // match a comma
                            "(?=                       " + // start positive look ahead
                            "  (?:                     " + //   start non-capturing group 1
                            "    %s*                   " + //     match 'otherThanQuote' zero or more times
                            "    %s                    " + //     match 'quotedString'
                            "  )*                      " + //   end group 1 and repeat it zero or more times
                            "  %s*                     " + //   match 'otherThanQuote'
                            "  $                       " + // match the end of the string
                            ")                         ", // stop positive look ahead
                    otherThanQuote, quotedString, otherThanQuote);

            String[] paramValues = paramList.split(regex, -1);

            for (String paramValue : paramValues) {
                paramValue = cleanString(paramValue);
                if (paramValue.contains(" ")) {
                    String[] internalParamValues = paramValue.split(" ");
                    for (String internalParamValue : internalParamValues) {
                        if (!internalParamValue.isEmpty())
                            finalParams.add(Pair.of(internalParamValue, null));
                    }
                } else {
                    finalParams.add(Pair.of(paramValue, null));
                }
            }
        }

        return finalParams;
    }

    private static String cleanString(String string) {
        string = string.trim().substring(1);
        return string.substring(0, string.length() - 1).trim();
    }
}
