import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.tokensregex.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

public class EntityRecognition {
    private static EntityRecognition singleton = null;
    StanfordCoreNLP pipeline = null;
    CoreMapExpressionExtractor extractor = null;

    private EntityRecognition(){
        Properties pipelineProps = new Properties();

        pipelineProps.setProperty("annotators", "tokenize,ssplit,pos");
        pipelineProps.setProperty("ner.applyFineGrained", "false");
        pipelineProps.setProperty("ssplit.eolonly", "true");
        pipeline = new StanfordCoreNLP(pipelineProps);

        // set up the TokensRegex pipeline

        // get the rules files
        String[] rulesFiles = new String[1];
        rulesFiles[0] = "src/main/resources/ner.rules";

        // set up an environment with reasonable defaults
        Env env = TokenSequencePattern.getNewEnv();
        // set to case insensitive
        env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        env.setDefaultStringPatternFlags(Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        // build the CoreMapExpressionExtractor
        extractor = CoreMapExpressionExtractor.createExtractorFromFiles(env, rulesFiles);
    }

    public static EntityRecognition getInstance(){
        if(singleton == null) {
            singleton = new EntityRecognition();
        }

        return singleton;
    }

    public HashMap<String, String> process(String inputSentence){
        Annotation exampleSentencesAnnotation = new Annotation(inputSentence);
        pipeline.annotate(exampleSentencesAnnotation);

        // for each sentence in the input text, run the TokensRegex pipeline
        HashMap<String, String> nerList = new HashMap<>();

        CoreMap sentence = exampleSentencesAnnotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);
            List<MatchedExpression> matchedExpressions = extractor.extractExpressions(sentence);

            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                if(token.ner() != null){
                    nerList.put(token.word(), token.ner());
                }
            }

        return nerList;
    }
}
