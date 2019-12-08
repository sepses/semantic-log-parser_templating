import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.ling.tokensregex.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;
import java.util.List;
import java.util.Properties;
import java.util.regex.*;

public class EntityRecognitionDemo
{

    // My custom tokens
    public static class MyTokensAnnotation implements CoreAnnotation<List<? extends CoreMap>> {
        @Override
        public Class<List<? extends CoreMap>> getType() {
            return ErasureUtils.<Class<List<? extends CoreMap>>> uncheckedCast(List.class);
        }
    }

    // My custom type
    public static class MyTypeAnnotation implements CoreAnnotation<String> {
        @Override
        public Class<String> getType() {
            return ErasureUtils.<Class<String>> uncheckedCast(String.class);
        }
    }

    // My custom value
    public static class MyValueAnnotation implements CoreAnnotation<String> {
        @Override
        public Class<String> getType() {
            return ErasureUtils.<Class<String>> uncheckedCast(String.class);
        }
    }

    public static void main(String[] args) {

        // load settings from the command line
        Properties props = StringUtils.argsToProperties(args);

        // get the text to process

        // load sentences
        String exampleSentences = "There will be a big announcement by Apple Inc today at 5:00pm."; //IOUtils.stringFromFile(props.getProperty("inputText"));

        // build pipeline to get sentences and do basic tagging
        Properties pipelineProps = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos");
        pipelineProps.setProperty("annotators", props.getProperty("annotators"));
        pipelineProps.setProperty("ner.applyFineGrained", "false");
        pipelineProps.setProperty("ssplit.eolonly", "true");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(pipelineProps);

        // get sentences
        Annotation exampleSentencesAnnotation = new Annotation(exampleSentences);
        pipeline.annotate(exampleSentencesAnnotation);

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
        CoreMapExpressionExtractor
                extractor = CoreMapExpressionExtractor.createExtractorFromFiles(env, rulesFiles);

        // for each sentence in the input text, run the TokensRegex pipeline
        int sentNum = 0;
        for (CoreMap sentence : exampleSentencesAnnotation.get(CoreAnnotations.SentencesAnnotation.class)) {
            System.out.println("---");
            System.out.println("sentence number: "+sentNum);
            System.out.println("sentence text: "+sentence.get(CoreAnnotations.TextAnnotation.class));
            sentNum++;
            List<MatchedExpression> matchedExpressions = extractor.extractExpressions(sentence);
            // print out the results of the rules actions
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                System.out.println(token.word() + "\t" + token.tag() + "\t" + token.ner());
            }
            // print out the matched expressions
            for (MatchedExpression me : matchedExpressions) {
                System.out.println("matched expression: "+me.getText());
                System.out.println("matched expression value: "+me.getValue());
                System.out.println("matched expression char offsets: "+me.getCharOffsets());
                System.out.println("matched expression tokens:" +
                        me.getAnnotation().get(CoreAnnotations.TokensAnnotation.class));
            }
        }
    }

}