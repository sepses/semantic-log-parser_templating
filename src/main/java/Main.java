import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.*;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
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

    static String NS_CORE = "http://w3id.org/sepses/vocab/log/core#";
    static String NS_PARSER = "http://w3id.org/sepses/vocab/log/parser#";
    static String NS_INSTANCE = "http://w3id.org/sepses/id/";

    // TODO: will be replaced by JAVA args later on
    private static String logTemplate = "./input/openSSH/OpenSSH_2k_A.log_templates.csv";
    private static String logData = "./input/openSSH/OpenSSH_2k_A.log_structured.csv";
    private static String existingTemplatesPath = "./input/templates_mapping.csv";
    private static String instancesStottr = "./input/instances.stottr";
    private static String parserFilePath = "src/main/resources/parser.ttl";

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
        logLines.forEach(logLine -> logLineList.add(LogLine.fromOpenSSH(logLine)));

        List<Template> templatesList = loadExistingTemplates(existingTemplates);

        try {
            processTemplates(templates, logLineList, templatesList);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        OntModel dataModel = ModelFactory.createOntologyModel(OntModelSpec.OWL_DL_MEM);
        InputStream is = Main.class.getClassLoader().getResourceAsStream(parserFilePath.substring(parserFilePath.lastIndexOf("/") + 1));
        RDFDataMgr.read(dataModel, is, Lang.TURTLE);

        parseLogLines(logLineList, templatesList, dataModel);
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
                    e.printStackTrace();
                }
            });
        }
    }

      private static void processTemplates(Iterable<CSVRecord> csvTemplates, List<LogLine> logLineList, List<Template> templatesList) throws NoSuchAlgorithmException, IOException {
        boolean change = false;

        // Annotate template parameters
        for (CSVRecord csvTemplate : csvTemplates) {
            Template template = new Template(csvTemplate.get(0), csvTemplate.get(1));

            final MessageDigest digest = MessageDigest.getInstance("SHA-256");
            final byte[] hashbytes = digest.digest(template.TemplateContent.getBytes(StandardCharsets.UTF_8));
            String hash = DigestUtils.sha256Hex(hashbytes);

            template.hash = hash;
            template.templatingId = "sepses:LogLine";

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

            templatesList.add(template); // add it to the in memory list for this run
            templateIdMappings.put(template.TemplateId, template.hash);
        }

        if(change) {
            writeExistingTemplatesCSV(templatesList);
        }
    }

    /**
     * (1) take each log line and produce instance of LogEntry in the KG;
     * <p>
     * (2) based on the template parameters, add additional information on URL, HOST, USER, DOMAIN, and PORT
     *
     * @param logLines
     * @param templatesList
     * @param dataModel
     */
    private static void parseLogLines(List<LogLine> logLines, List<Template> templatesList, OntModel dataModel) {
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

            String paramValues = logline.ParameterList.substring(1, logline.ParameterList.length() - 1); //[...]

            //sepses:LogLine(instance:Line1, \"2019-12-10\", \"Failed\", \"23f2f23f\", (\"aekelhart\", \"127.0.0.1\")) ."
            String eventTime = "";

            try {
                eventTime = getDate(logline.EventMonth, logline.EventDay, logline.EventTime);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            tmpLine = currentTemplate.templatingId + "(instance:Logline_" + UUID.randomUUID() + ",\"" + eventTime + "\",\"" + logline.Content + "\",\"" + currentTemplate.hash + "\"";

            if (!paramValues.isEmpty()) {
                String[] parameterValues = paramValues.split(",");

                tmpLine += ", (";
                for (int counter = 0; counter < parameterValues.length; counter++) {
                    String parameter = parameterValues[counter].trim();
                    String paramValue = parameter.replaceAll("'", "");

                    tmpLine += "\"" + paramValue + "\"";
                    if(counter < parameterValues.length - 1)
                        tmpLine += ",";
                }
                tmpLine += ")";
            }
            else{
                tmpLine += ", ()";
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
