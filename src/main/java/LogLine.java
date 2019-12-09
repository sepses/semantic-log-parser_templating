import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        this.ParameterString = ParameterString;

        this.ParameterList = new ArrayList<Pair>();

        Pattern pattern = Pattern.compile("\'([^\'^,]*)\'");
        Matcher matcher = pattern.matcher(ParameterString);
        while(matcher.find()) {
            //System.out.println(matcher.group(1));
            this.ParameterList.add(new Pair(matcher.group(1), null));
        }

//        for (String param : Arrays.asList(ParameterString.substring(1, ParameterString.length() - 1).split(","))) {
//            if(!param.trim().equals(""))
//                this.ParameterList.add(new Pair(param.trim().replaceAll("'", ""), null));
//        }
    }

    public static LogLine createFromLogline(CSVRecord logLine) {
        return new LogLine(logLine.get(0), logLine.get(1), logLine.get(2), logLine.get(3), logLine.get(4),
                logLine.get(5), logLine.get(6), logLine.get(7), logLine.get(8), logLine.get(9));
    }

    class Pair{
        public String key;
        public String value;

        public Pair(String key, String value){
            this.key = key;
            this.value = value;
        }
    }
}
