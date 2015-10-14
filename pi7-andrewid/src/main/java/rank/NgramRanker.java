package rank;

import java.io.StringReader;
import java.util.List;

import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.Morphology;
import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import type.Passage;
import type.Question;

public class NgramRanker extends AbstractRanker {
	
	private int mN;
	private boolean mComposite;

	/**
	 * Returns a score of the given passage associated with the given question.
	 * 
	 * @param question
	 * @param passage
	 * @return a score of the passage
	 */
	@Override
	public Double score(Question question, Passage passage) {
		// Tokenizing the same question every time isn't very efficient.
		// For the time being, we'll let it slide.
		String[] qTokens = tokenize(question.getSentence());
		String[] pTokens = tokenize(passage.getText());
		
		// Calculate sum of percentages of Question n-grams in Passage for n = 1..N.
		// I may not keep this cumulative approach.
		double[] scores = new double[mN];
		for (int n = 1; n <= mN; n++) {
			scores[n-1] = ngram_sim(qTokens, pTokens, n);
		}
		double score = 0.0;
		for (int i = 0; i < scores.length; i++) {
			score += scores[i];
		}
		passage.setScore(score);
		
		return score;
	}
	
	public static double ngram_sim(String[] qTokens, String[] pTokens, int n) {
		// Calculate percentage of Question n-grams in Passage.
		// There are many more advanced ways to do this.		
		double score = 0.0;
		for (int i = 0; i < qTokens.length - n + 1; i++) {
			for (int j = 0; j < pTokens.length - n + 1; j++) {
				boolean mismatch = false;
				for (int k = 0; k < n; k++) {
					if (!qTokens[i + k].equals(pTokens[j + k])) {
						mismatch = true;
						break;
					}
				}
				if (!mismatch) {
					score += 1.0;
					break;
				}
			}
		}
		score /= qTokens.length - n + 1;
		return score;
	}
	
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
