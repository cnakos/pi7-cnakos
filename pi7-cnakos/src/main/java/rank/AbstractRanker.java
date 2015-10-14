package rank;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import type.Passage;
import type.Question;

/**
 * This class provides a skeletal implementation of interface IRanker.
 */
public abstract class AbstractRanker implements IRanker {

	/**
	 * Sorts the given list of passages associated with the given question, and returns a ranked list
	 * of passages. A subclass needs to implement this method.
	 * 
	 * @param question
	 * @param passages
	 */
	public List<Passage> rank(Question question, List<Passage> passages) {
		// Score all the given passages and sort them in List object 'rankedPassages' below.
		List<Passage> rankedPassages = new ArrayList<Passage>();
		HashMap<Passage, Double> scores = new HashMap<Passage, Double>();
		for (Passage passage : passages) {
			scores.put(passage, score(question, passage));
			rankedPassages.add(passage);
		}
		
		Comparator<Passage> comp = new Comparator<Passage>() {
			public int compare(Passage o1, Passage o2) {
				if (scores.get(o1) > scores.get(o2))
					return -1;
				else if (scores.get(o1) == scores.get(o2))
					return 0;
				else
					return 1;
			}
		};
		Collections.sort(rankedPassages, comp);
		return rankedPassages;
	}

	/**
	 * Returns a score of the given passage associated with the given question. A subclass needs to
	 * implement this method.  A subclass needs to implement this method.
	 * 
	 * @param question
	 * @param passage
	 * @return
	 */
	public abstract Double score(Question question, Passage passage);

}
