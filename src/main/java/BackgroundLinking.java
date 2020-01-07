import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class BackgroundLinking {

    public static final String OUTPUT_FOLDER = "./output/";
    public static final String LOG_FILE = "./input/ottr/out.stottr";
    public static final String BG_FILE = "./ontology/background-knowledge.ttl";
    public static final String DHCP_QUERY = "./ontology/dhcp-lease.sparql";
    public static final String PERSON_QUERY = "./ontology/person-account.sparql";
    public static final String UNKNOWN_IP_QUERY = "./ontology/unknown-ip.sparql";

    private Model logModel;

    BackgroundLinking() {
        logModel = ModelFactory.createDefaultModel();
    }

    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return new String(encoded, encoding);
    }

    public static void main(String[] args) throws IOException {
        BackgroundLinking backgroundLinking = new BackgroundLinking();
        backgroundLinking.link();
    }

    public void link() throws IOException {

        RDFDataMgr.read(logModel, new FileInputStream(BG_FILE), Lang.TURTLE);
        RDFDataMgr.read(logModel, new FileInputStream(LOG_FILE), Lang.TURTLE);

        Query dhcpQuery = QueryFactory.create(readFile(DHCP_QUERY, Charset.forName("UTF-8")));
        QueryExecution dhcpQueryExecution = QueryExecutionFactory.create(dhcpQuery, logModel);
        Model dhcpModel = dhcpQueryExecution.execConstruct();
        RDFDataMgr.write(new FileOutputStream(OUTPUT_FOLDER + "dhcp.ttl"), dhcpModel, Lang.TURTLE);
        dhcpModel.close();

        Query personQuery = QueryFactory.create(readFile(PERSON_QUERY, Charset.forName("UTF-8")));
        QueryExecution personQueryExecution = QueryExecutionFactory.create(personQuery, logModel);
        Model personModel = personQueryExecution.execConstruct();
        RDFDataMgr.write(new FileOutputStream(OUTPUT_FOLDER + "person.ttl"), personModel, Lang.TURTLE);
        personModel.close();

        Query unknownIPs = QueryFactory.create(readFile(UNKNOWN_IP_QUERY, Charset.forName("UTF-8")));
        QueryExecution unknownIPExecution = QueryExecutionFactory.create(unknownIPs, logModel);
        Model unknownIPModel = unknownIPExecution.execConstruct();
        RDFDataMgr.write(new FileOutputStream(OUTPUT_FOLDER + "unknownIPs.ttl"), unknownIPModel, Lang.TURTLE);
        unknownIPModel.close();
    }

}
