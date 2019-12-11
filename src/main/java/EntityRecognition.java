import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.*;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

public class EntityRecognition {

    // *** rules for text extraction
    private final static String ruleFile = "src/main/resources/ner.rules";

    private static EntityRecognition singleton = null;
    private StanfordCoreNLP pipeline = null;
    private CoreMapExpressionExtractor extractor = null;

    private EntityRecognition() {
        Properties pipelineProps = new Properties();

        pipelineProps.setProperty("annotators", "tokenize,ssplit,pos");
        pipelineProps.setProperty("ner.applyFineGrained", "false");
        pipelineProps.setProperty("ssplit.eolonly", "true");
        pipeline = new StanfordCoreNLP(pipelineProps);

        // set up the TokensRegex pipeline

        // get the rules files
        String[] rulesFiles = new String[1];
        rulesFiles[0] = ruleFile;

        // set up an environment with reasonable defaults
        Env env = TokenSequencePattern.getNewEnv();

        // set to case insensitive
        env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
        env.setDefaultStringPatternFlags(Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

        // build the CoreMapExpressionExtractor
        extractor = CoreMapExpressionExtractor.createExtractorFromFiles(env, rulesFiles);
    }

    public static EntityRecognition getInstance() {
        if (singleton == null) {
            singleton = new EntityRecognition();
        }

        return singleton;
    }

    /**
     * Annotate a sentence according to ruleFile.
     *
     * @param inputSentence
     * @return a map of <param-value, param-type>
     */
    public HashMap<String, String> annotateSentence(String inputSentence) {
        Annotation exampleSentencesAnnotation = new Annotation(inputSentence);
        pipeline.annotate(exampleSentencesAnnotation);

        // for each sentence in the input text, run the TokensRegex pipeline
        HashMap<String, String> nerList = new HashMap<>();

        CoreMap sentence = exampleSentencesAnnotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);
        List<MatchedExpression> matchedExpressions = extractor.extractExpressions(sentence);

        for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
            if (token.ner() != null) {
                nerList.put(token.word(), token.ner());
            }
        }

        return nerList;
    }
}
