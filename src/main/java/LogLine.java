import org.apache.commons.csv.CSVRecord;

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
    public String ParameterList;

    public LogLine(String LineId, String EventMonth, String EventDay, String EventTime, String Level,
            String Component, String Content, String EventId, String EventTemplate, String ParameterList) {
        this.LineId = Integer.parseInt(LineId);
        this.EventMonth = EventMonth;
        this.EventDay = EventDay;
        this.EventTime = EventTime;
        this.Level = Level;
        this.Component = Component.replaceAll("[\\[\\]]", "");
        this.Content = Content;
        this.EventId = EventId;
        this.EventTemplate = EventTemplate;
        this.ParameterList = ParameterList;
    }

    public static LogLine fromOpenSSH(CSVRecord logLine) {
        return new LogLine(logLine.get(0), logLine.get(1), logLine.get(2), logLine.get(3), logLine.get(4),
                logLine.get(5), logLine.get(6), logLine.get(7), logLine.get(8), logLine.get(9));
    }
}
