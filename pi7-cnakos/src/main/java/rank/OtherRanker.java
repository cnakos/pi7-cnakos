package rank;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;

import type.Passage;
import type.Question;

public class OtherRanker extends AbstractRanker {
	
	private HashMap<String, Double> idfs;
	private HashMap<Question, Double> avgdls;
	private double k1;
	private double b;
	
	public OtherRanker(JCas jcas, double k1, double b) {
		this.k1 = k1;
		this.b = b;
		
		// Initialize IDFs using JCas.
		HashMap<String, Integer> dfs = new HashMap<String, Integer>();
		// Average document length for each question.
		avgdls = new HashMap<Question, Double>();
		// Can't use UimaUtils because it's in the default package.
		ArrayList<Question> questions = new ArrayList<Question>(JCasUtil.select(jcas, Question.class));
		int docCount = 0;
		String[] tokens;
		for (Question question : questions) {
			docCount++;
			avgdls.put(question, 0.0);
			tokens = NLPUtils.tokenize(question.getSentence());
			for (String token : tokens)
				dfs.put(token, dfs.getOrDefault(token, 0) + 1);
			// Can't use UimaUtils because it's in the default package.
			Collection<Passage> passages = FSCollectionFactory.create(question.getPassages(), Passage.class);
			for (Passage passage : passages) {
				tokens = NLPUtils.tokenize(passage.getText());
				for (String token : tokens)
					dfs.put(token, dfs.getOrDefault(token, 0) + 1);
				avgdls.put(question, avgdls.get(question) + tokens.length);
			}
			avgdls.put(question, avgdls.get(question) / passages.size());
		}
		idfs = calculateIdfs(dfs, docCount);
	}
	
	private HashMap<String, Double> calculateIdfs(HashMap<String, Integer> dfs, int docCount) {
		// Finalize IDFs from counts.
		HashMap<String, Double> idfs = new HashMap<String, Double>();
		for (String key : dfs.keySet()) {
			double count = dfs.get(key);
			idfs.put(key, Math.log((docCount - count + 0.5) / (count + 0.5)));
		}
		return idfs;
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
		// Calculate score using Okapi BM25.
		double score = 0.0;
		String[] qTokens = NLPUtils.tokenize(question.getSentence());
		String[] pTokens = NLPUtils.tokenize(passage.getText());
		HashMap<String, Double> tfs = new HashMap<String, Double>();
		for (String token : pTokens)
			tfs.put(token, tfs.getOrDefault(token, 0.0) + 1.0);
		for (String token : qTokens) {
			double idf = idfs.getOrDefault(token, 0.0);
			double tf = tfs.getOrDefault(token, 0.0);
			score += (idf * tf * (k1 + 1.0)) / (tf + k1 * (1 - b + b * pTokens.length / avgdls.get(question)));
		}

		return score;
	}

}
