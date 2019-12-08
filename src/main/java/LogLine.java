import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.Arrays;
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
    public String ParameterString;
    public List<String> ParameterList;

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
        this.ParameterString = ParameterString;

        this.ParameterList = new ArrayList();
        this.ParameterList = Arrays.asList(ParameterString.substring(1, ParameterString.length() - 1).split(","));

        for(int i = 0; i < this.ParameterList.size(); i++){
            this.ParameterList.set(i, this.ParameterList.get(i).trim().replaceAll("'", ""));
        }
    }

    public static LogLine createFromLogline(CSVRecord logLine) {
        return new LogLine(logLine.get(0), logLine.get(1), logLine.get(2), logLine.get(3), logLine.get(4),
                logLine.get(5), logLine.get(6), logLine.get(7), logLine.get(8), logLine.get(9));
    }
}
