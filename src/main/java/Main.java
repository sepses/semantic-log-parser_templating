import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Main {

    private static final Logger LOG = LoggerFactory.getLogger(Main.class);
    private static String logTemplate = "./input/Logs/openSSH/OpenSSH_2k_A.log_templates.csv";
    private static String logData = "./input/Logs/openSSH/OpenSSH_2k_A.log_structured.csv";
    private static String existingTemplatesPath = "./input/templatesMapping.csv";
    private static String ottrTemplatesPath = "./input/ottr/templates.stottr";
    private static String instancesStottr = "./input/ottr/instances.stottr";
    private static Map<String, String> templateIdMappings = new HashMap<>();
    static String[] HEADERS = { "hash", "content", "template"};

    /**
     * Main function - will be updated later to allow args parameterization
     *
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Reader templateReader = new FileReader(Paths.get(logTemplate).toFile());
        Reader dataReader = new FileReader(Paths.get(logData).toFile());
        Reader existingTemplateReader = new FileReader(Paths.get(existingTemplatesPath).toFile());

        Iterable<CSVRecord> templates = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(templateReader);
        Iterable<CSVRecord> logLines = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(dataReader);
        Iterable<CSVRecord> existingTemplates = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(existingTemplateReader);

        List<LogLine> logLineList = new ArrayList<>();
        logLines.forEach(logLine -> logLineList.add(LogLine.createFromLogline(logLine)));

        List<Template> templatesList = loadExistingTemplates(existingTemplates);

        try {
            processTemplates(templates, logLineList, templatesList);
        } catch (NoSuchAlgorithmException e) {
            LOG.error(e.toString());
        }

        parseLogLines(logLineList, templatesList);
    }

    private static List<Template>  loadExistingTemplates(Iterable<CSVRecord> existingTemplates) {
        List<Template> templatesList = new ArrayList<>();
        existingTemplates.forEach(existingTemplate ->
                        templatesList.add(Template.parse(existingTemplate)));

        return templatesList;
    }

    private static void writeExistingTemplatesCSV(List<Template> templates) throws IOException {
        FileWriter out = new FileWriter(existingTemplatesPath);
        try (CSVPrinter printer = new CSVPrinter(out, CSVFormat.DEFAULT
                .withHeader(HEADERS))) {
            templates.forEach(template -> {
                try {
                    printer.printRecord(template.hash, template.TemplateContent, template.templatingId);
                } catch (IOException e) {
                    LOG.error(e.toString());
                }
            });
        }
    }

    private static void processTemplates(Iterable<CSVRecord> csvTemplates, List<LogLine> logLineList, List<Template> templatesList) throws NoSuchAlgorithmException, IOException {
        boolean change = false;
        EntityRecognition er = EntityRecognition.getInstance();
        StringBuilder ottrStringBuilding = new StringBuilder();

        // Annotate template parameters
        for (CSVRecord csvTemplate : csvTemplates) {
            Template template = new Template(csvTemplate.get(0), csvTemplate.get(1));

            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashbytes = digest.digest(template.TemplateContent.getBytes(StandardCharsets.UTF_8));
            String hash = DigestUtils.sha256Hex(hashbytes);

            template.hash = hash;
            template.templatingId = "sepses:LogLine"; // Default

            boolean exists = false;
            for (Template existingTemplate : templatesList) {
                if (existingTemplate.hash.equals(template.hash)) {
                    exists = true;

                    // Store mappings from templateId to Hash for this run
                    templateIdMappings.put(template.TemplateId, existingTemplate.hash);
                    break;
                }
            }

            if (exists)
                continue;

            change = true;
            String specificParams = "";
            StringBuilder ottrBody = new StringBuilder();

            // Generate new template
            for (LogLine logLine : logLineList) {
                if(logLine.EventId.equals(template.TemplateId)){

                    HashMap<String, String> matchedExpressions = er.process(logLine.Content);

                    // If there are parameters, we create a custom ottr template, otherwise use the default log template
                    if(logLine.ParameterList.size() > 0){
                        generateOttrTemplate(ottrStringBuilding, template, specificParams, ottrBody, logLine, matchedExpressions);
                    }

                    break;
                }
            }

            templatesList.add(template); // add it to the in memory list for this run
            templateIdMappings.put(template.TemplateId, template.hash);
        }

        if(change) {
            writeExistingTemplatesCSV(templatesList);
            writeOttrTemplates(ottrStringBuilding.toString());
        }
    }

    private static void generateOttrTemplate(StringBuilder ottrStringBuilding, Template template, String specificParams, StringBuilder ottrBody, LogLine logLine, HashMap<String, String> matchedExpressions) {
        String ottrTemplateName = "sepses:LogLine_" + template.TemplateId;
        template.templatingId = ottrTemplateName;

        ottrBody.append("\t sepses:BasicLogLineInformation(?id, ?timeStamp, ?message, ?templateHash),\n");
        ottrBody.append("\t sepses:Type(?id, :Logline)");
        int paramCnt = 0;

        if(logLine.ParameterList.size() > 0)
            ottrBody.append(",");

        for(LogLine.Pair paramPair : logLine.ParameterList){
            boolean found = false;
            String type = "";
            paramCnt++;

            for (String key : matchedExpressions.keySet()) {
                type = matchedExpressions.get(key);

                if(paramPair.key.contains(key))
                {
                    LOG.info("Found key");
                    found = true;
                    //logLine.ParameterList.put(param, type);
                    break;
                }
            }

            if(found){
                ottrBody.append("\n\t sepses:" + type + "(?id, ?" + type.toLowerCase() + "),");
                specificParams += ", xsd:String ?" +  type.toLowerCase();
            }else{
                ottrBody.append("\n\t sepses:UnknownConnection" + "(?id, ?param" + paramCnt + "),");
                specificParams += ", xsd:String ?param" + paramCnt;
            }
        }

        if(specificParams.length() > 0)
            ottrBody.deleteCharAt(ottrBody.length()-1);

        ottrStringBuilding.append("\n");
        ottrStringBuilding.append(ottrTemplateName + "[ottr:IRI ?id, xsd:datetime ?timeStamp, xsd:string ?message, xsd:string ?templateHash" + specificParams + "] :: {\n");

        ottrStringBuilding.append(ottrBody.toString());
        ottrStringBuilding.append("\n} .\n");
    }

    private static void writeOttrTemplates(String templatesString) {
        try (FileWriter f = new FileWriter(ottrTemplatesPath, true);
             BufferedWriter b = new BufferedWriter(f);
             PrintWriter p = new PrintWriter(b)) {

            p.print("\n#New Templates: " + DateTime.now().toString());
            p.print(templatesString);

        } catch (IOException i) {
            LOG.error(i.toString());
        }
    }

    /**
     * (1) take each log line and produce instance of LogEntry in the KG;
     * <p>
     * (2) based on the template parameters, add additional information on URL, HOST, USER, DOMAIN, and PORT
     *
     * @param logLines
     * @param templatesList
     */
    private static void parseLogLines(List<LogLine> logLines, List<Template> templatesList) {
        ArrayList<String> instanceLines = new ArrayList<>();

        for (LogLine logline : logLines) {
            LOG.info("Process logline: " + logline.LineId);

            String tmpLine = "";

            // get the template for the log line
            String loglineTemplateHash = templateIdMappings.get(logline.EventId); // Get hash from mapping
            Template currentTemplate = templatesList.stream()
                    .filter(template -> template.hash.equals(loglineTemplateHash))
                    .findFirst()
                    .orElse(null);

            if(currentTemplate == null)
                return;

            String paramValues = logline.ParameterString.substring(1, logline.ParameterString.length() - 1); //[...]

            //sepses:LogLine(instance:Line1, \"2019-12-10\", \"Failed\", \"23f2f23f\", (\"aekelhart\", \"127.0.0.1\")) ."
            String eventTime = "";

            try {
                eventTime = getDate(logline.EventMonth, logline.EventDay, logline.EventTime);
            } catch (ParseException e) {
                LOG.error(e.toString());
            }

            tmpLine = currentTemplate.templatingId + "(instance:Logline_" + UUID.randomUUID() + ",\"" + eventTime + "\",\"" + logline.Content + "\",\"" + currentTemplate.hash + "\"";

            if (!paramValues.isEmpty()) {
                String[] parameterValues = paramValues.split(",");

                if(currentTemplate.templatingId.equals("sepses:LogLine")) { // default template put all in a list
                    tmpLine += ", (";
                    for (int counter = 0; counter < parameterValues.length; counter++) {
                        String parameter = parameterValues[counter].trim();
                        String paramValue = parameter.replaceAll("'", "");

                        tmpLine += "\"" + paramValue + "\"";
                        if (counter < parameterValues.length - 1)
                            tmpLine += ",";
                    }
                    tmpLine += ")";
                }else{ // specific template, add parameters as variables
                    tmpLine += ", ";
                    for (int counter = 0; counter < parameterValues.length; counter++) {
                        String parameter = parameterValues[counter].trim();
                        String paramValue = parameter.replaceAll("'", "");

                        tmpLine += "\"" + paramValue + "\"";
                        if (counter < parameterValues.length - 1)
                            tmpLine += ",";
                    }
                }
            }
            else{
                //tmpLine += ", ()";
            }

            tmpLine += ") .\n";

            instanceLines.add(tmpLine);
        }

        FileWriter out = null;
        try {
            out = new FileWriter(instancesStottr);
            out.write("@prefix instance: \t <http://w3id.org/sepses/id/> .\n" +
                          "@prefix sepses: \t  <http://sepses.com/ns#> . \n");

            for (String line : instanceLines) {
                out.write(line);
            }

            out.close();
        } catch (Exception e) {
            LOG.error("Error writing stottr file: " + e.toString());
        }
    }

    private static String getDate(String month, String day, String time) throws ParseException {

        day = StringUtils.leftPad(day, 2, "0");

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, DateTime.now().getYear());
        cal.set(Calendar.MONTH, new SimpleDateFormat("MMM", Locale.ENGLISH).parse(month).getMonth());
        cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(day));
        Date dateRepresentation = cal.getTime();

        SimpleDateFormat xmlDateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        String dateString;

        dateString = xmlDateFormatter.format(dateRepresentation);

        return dateString;
    }
}
