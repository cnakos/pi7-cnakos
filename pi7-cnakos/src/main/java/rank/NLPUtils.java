package rank;
import java.io.StringReader;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;

public class NLPUtils {
	
	private NLPUtils() { }

	// Return an array of Tokens added to the jcas corresponding to the given text.
	public static String[] tokenize(String text) {
		TokenizerFactory<Word> factory = PTBTokenizerFactory.newTokenizerFactory();
	    Tokenizer<Word> tokenizer = factory.getTokenizer(new StringReader(text));
		List<Word> words = tokenizer.tokenize();
		String[] tokens = new String[words.size()];
		for (int i = 0; i < words.size(); i++) {
			tokens[i] = Morphology.stemStatic(words.get(i).word(), null).word();
		}
		return tokens;
	}
}
