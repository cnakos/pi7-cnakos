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
	private boolean mCumulative;
	
	public NgramRanker() {
		this(3);
	}
	
	public NgramRanker(int n) {
		this(n, false);
	}
	
	// Set the n-gram size and whether to aggregate over all smaller n-grams as well.
	public NgramRanker(int n, boolean cumulative) {
		mN = n;
		mCumulative = cumulative;
	}

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
		String[] qTokens = NLPUtils.tokenize(question.getSentence());
		String[] pTokens = NLPUtils.tokenize(passage.getText());
		
		double score = 0.0;
		if (mCumulative) {
			// Calculate sum of percentages of Question n-grams in Passage for n = 1..N.
			// I may not keep this cumulative approach.
			
			double[] scores = new double[mN];
			for (int n = 1; n <= mN; n++) {
				scores[n-1] = ngram_sim(qTokens, pTokens, n);
			}
			for (int i = 0; i < scores.length; i++) {
				score += scores[i];
			}
			score /= scores.length;
		} else {
			// Calculate n-gram score for just mN.
			score = ngram_sim(qTokens, pTokens, mN);
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
}
