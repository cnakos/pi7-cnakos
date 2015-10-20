package rank;

import java.util.ArrayList;
import java.util.List;

import type.Passage;
import type.Question;

public class CompositeRanker extends AbstractRanker implements IAggregator {

	/** Individual rankers */
	private List<IRanker> rankers;

	public CompositeRanker() {
		rankers = new ArrayList<IRanker>();
	}

	public void addRanker(IRanker ranker) {
		rankers.add(ranker);
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
		List<Double> scores = new ArrayList<Double>();
		for (IRanker r : rankers) {
			scores.add(r.score(question, passage));
		}
		return aggregateScores(scores);
	}

	@Override
	public Double aggregateScores(List<Double> scores) {
		// Just average scores for this project.
		Double score = 0.0;
		for (int i = 0; i < scores.size(); i++) {
			score += scores.get(i);
		}
		score /= scores.size();

		return score;
	}

}
