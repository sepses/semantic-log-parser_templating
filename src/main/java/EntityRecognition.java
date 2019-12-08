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
        //                props.getProperty("rulesFiles").split(",");

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
        //String exampleSentences = "There will be a big announcement by Apple Inc today at 5:00pm."; //IOUtils.stringFromFile(props.getProperty("inputText"));
        Annotation exampleSentencesAnnotation = new Annotation(inputSentence);
        pipeline.annotate(exampleSentencesAnnotation);

        // for each sentence in the input text, run the TokensRegex pipeline
        int sentNum = 0;

        HashMap<String, String> nerList = new HashMap<>();

        CoreMap sentence = exampleSentencesAnnotation.get(CoreAnnotations.SentencesAnnotation.class).get(0);

//            System.out.println("---");
//            System.out.println("sentence number: "+sentNum);
//            System.out.println("sentence text: "+sentence.get(CoreAnnotations.TextAnnotation.class));
            sentNum++;
            List<MatchedExpression> matchedExpressions = extractor.extractExpressions(sentence);
            // print out the results of the rules actions

            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
//                System.out.println(token.word() + "\t" + token.tag() + "\t" + token.ner());
                if(token.ner() != null){
                    nerList.put(token.word(), token.ner());
                }
            }

            // print out the matched expressions
//            for (MatchedExpression me : matchedExpressions) {
//                System.out.println("matched expression: "+me.getText());
//                System.out.println("matched expression value: "+me.getValue());
//                System.out.println("matched expression char offsets: "+me.getCharOffsets());
//                System.out.println("matched expression tokens:" +
//                        me.getAnnotation().get(CoreAnnotations.TokensAnnotation.class));
//            }

        return nerList;
    }
}
