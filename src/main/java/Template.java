import org.apache.commons.csv.CSVRecord;

import java.util.ArrayList;
import java.util.List;

/**
 * Class representation of a template
 */
public class Template {

    public String TemplateId;
    public String TemplateContent;
    public List<EntityPattern> parameterDict;
    public String subject;
    public String hash;
    public String templatingId;
    public String ottrTemplate;
    public String[] keywords;

    public Template(String TemplateId, String TemplateContent) {
        this.TemplateId = TemplateId;
        this.TemplateContent = TemplateContent;
        parameterDict = new ArrayList<>();
    }

    public Template(String TemplateId, String TemplateContent, String hash, String templatingId, String keywords) {
        this(TemplateId, TemplateContent);
        this.hash = hash;
        this.templatingId = templatingId;

        if(!keywords.isEmpty())
            this.keywords = keywords.split("|");
    }

    public static Template parse(CSVRecord template) {
        return new Template("", template.get(1), template.get(0), template.get(2), template.get(3));
    }

    public String getKeywordString(){
        String keywordString = "";
        boolean first = true;

        for(String keyword : this.keywords){
            if(!first)
                keywordString += "|";

            first = false;
            keywordString += keyword;
        }

        return keywordString;
    }
}
