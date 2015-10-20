import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CasConsumer_ImplBase;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.util.FSCollectionFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceProcessException;

import rank.CompositeRanker;
import rank.IRanker;
import rank.NgramRanker;
import rank.OtherRanker;
import type.Measurement;
import type.Passage;
import type.Question;

/**
 * This CAS Consumer generates the report file with the method metrics
 */
public class PassageRankingWriter extends CasConsumer_ImplBase {
	final String PARAM_OUTPUTDIR = "OutputDir";

	final String NGRAM_OUTPUT_FILENAME = "NgramErrorAnalysis.csv";
	final String OTHER_OUTPUT_FILENAME =  "OtherErrorAnalysis.csv";
	final String COMPOSITE_OUTPUT_FILENAME = "CompositeErrorAnalysis.csv";

	File mOutputDir;

	IRanker ngramRanker, otherRanker;

	CompositeRanker compositeRanker;
	
	// How many of the top-ranked items to label as relevant.
	private int mRankThreshold = 2;

	@Override
	public void initialize() throws ResourceInitializationException {
		String mOutputDirStr = (String) getConfigParameterValue(PARAM_OUTPUTDIR);
		if (mOutputDirStr != null) {
			mOutputDir = new File(mOutputDirStr);
			if (!mOutputDir.exists()) {
				mOutputDir.mkdirs();
			}
		}

		/*// Initialize rankers
		compositeRanker = new CompositeRanker();
		ngramRanker = new NgramRanker();
		otherRanker = new OtherRanker();
		compositeRanker.addRanker(ngramRanker);
		compositeRanker.addRanker(otherRanker);
		*/
	}

	@Override
	public void processCas(CAS arg0) throws ResourceProcessException {
		System.out.println(">> Passage Ranking Writer Processing");
		// Import the CAS as a aJCas
		JCas aJCas = null;
		File ngramOutputFile = null;
		File otherOutputFile = null;
		File compositeOutputFile = null;
		PrintWriter ngramWriter = null;
		PrintWriter otherWriter = null;
		PrintWriter compositeWriter = null;
		ArrayList<PrintWriter> writers = new ArrayList<PrintWriter>();
		try {
			aJCas = arg0.getJCas();
			
			// Initialize rankers here rather than during initialize() above so we can calculate IDFs.
			compositeRanker = new CompositeRanker();
			ngramRanker = new NgramRanker();
			otherRanker = new OtherRanker(aJCas, 1.6, 0.75);
			compositeRanker.addRanker(ngramRanker);
			compositeRanker.addRanker(otherRanker);
			
			try {
				ngramOutputFile = new File(Paths.get(mOutputDir.getAbsolutePath(), NGRAM_OUTPUT_FILENAME).toString());
				ngramOutputFile.getParentFile().mkdirs();
				ngramWriter = new PrintWriter(ngramOutputFile);
			} catch (FileNotFoundException e) {
				System.out.printf("Output file could not be written: %s\n",
						Paths.get(mOutputDir.getAbsolutePath(), NGRAM_OUTPUT_FILENAME).toString());
				return;
			}
			
			try {
				otherOutputFile = new File(Paths.get(mOutputDir.getAbsolutePath(), OTHER_OUTPUT_FILENAME).toString());
				otherOutputFile.getParentFile().mkdirs();
				otherWriter = new PrintWriter(otherOutputFile);
			} catch (FileNotFoundException e) {
				System.out.printf("Output file could not be written: %s\n",
						Paths.get(mOutputDir.getAbsolutePath(), OTHER_OUTPUT_FILENAME).toString());
				return;
			}
			
			try {
				compositeOutputFile = new File(Paths.get(mOutputDir.getAbsolutePath(), COMPOSITE_OUTPUT_FILENAME).toString());
				compositeOutputFile.getParentFile().mkdirs();				
				compositeWriter = new PrintWriter(compositeOutputFile);
			} catch (FileNotFoundException e) {
				System.out.printf("Output file could not be written: %s\n",
						Paths.get(mOutputDir.getAbsolutePath(), COMPOSITE_OUTPUT_FILENAME).toString());
				return;
			}
			
			// Save some time later.
			writers.add(ngramWriter);
			writers.add(otherWriter);
			writers.add(compositeWriter);

			for (PrintWriter writer : writers)
				writer.println("question_id,tp,fn,fp,precision,recall,f1");

			// Retrieve all the questions for printout
			List<Question> allQuestions = UimaUtils.getAnnotations(aJCas, Question.class);
			List<Question> subsetOfQuestions = RandomUtils.getRandomSubset(allQuestions, 10);

			// TODO: Here one needs to sort the questions in ascending order of their question ID
			Comparator<Question> qComparator = new Comparator<Question>() {
				public int compare(Question o1, Question o2) {
					return o1.getId().compareTo(o2.getId());
				}
			};
			Collections.sort(subsetOfQuestions, qComparator);

			double[] mrrs = new double[3];
			double[] maps = new double[3];
			for (Question question : subsetOfQuestions) {
				List<Passage> passages = UimaUtils.convertFSListToList(question.getPassages(), Passage.class);

				// TODO: Use the following three lists of ranked passages for your error analysis
				List<Passage> ngramRankedPassages = ngramRanker.rank(question, passages);
				List<Passage> otherRankedPassages = otherRanker.rank(question, passages);
				List<Passage> compositeRankedPassages = compositeRanker.rank(question, passages);

				// Calculate score for each ranking. 
				List<List<Passage>> passagesLists = new ArrayList<List<Passage>>();
				passagesLists.add(ngramRankedPassages);
				passagesLists.add(otherRankedPassages);
				passagesLists.add(compositeRankedPassages);
				
				for (int i = 0; i < passagesLists.size(); i++) {
					int tp = 0, fp = 0, fn = 0, tn = 0;
					double p = 0.0, ap = 0.0, rr = 0.0;
					for (int j = 0; j < passagesLists.get(i).size(); j++) {
						boolean label = passagesLists.get(i).get(j).getLabel();
						if (label) {
							p += 1.0;
							ap += p / (j + 1.0);
							if (rr == 0.0)
								rr = 1.0 / (j + 1.0);
						} 
						if (j < mRankThreshold && label)
							tp++;
						else if (j < mRankThreshold && !label)
							fp++;
						else if (j >= mRankThreshold && label)
							fn++;
						else
							tn++;
					}
					if (p > 0.0)
						ap /= p;
					double precision = tp + fp > 0.0 ? ((double) tp) / (tp + fp) : 0.0;
					double recall = tp + fn > 0.0 ? ((double) tp) / (tp + fn) : 0.0;
					double f1 = precision + recall > 0.0 ? 2 * precision * recall /(precision + recall) : 0.0;
					writers.get(i).printf("%s,%d,%d,%d,%.3f,%.3f,%.3f\n", question.getId(), tp, fn, fp,
							precision, recall, f1);
					
					mrrs[i] += rr;
					maps[i] += ap;
				}
			}
			for (int i = 0; i < mrrs.length; i++) {
				mrrs[i] /= subsetOfQuestions.size();
				maps[i] /= subsetOfQuestions.size();
			}
			System.out.println("Ngram Ranker: MAP " + Double.toString(maps[0]) + " MRR " + Double.toString(mrrs[0]));
			System.out.println("Other Ranker: MAP " + Double.toString(maps[1]) + " MRR " + Double.toString(mrrs[1]));
			System.out.println("Composite Ranker: MAP " + Double.toString(maps[2]) + " MRR " + Double.toString(mrrs[2]));
		} catch (CASException e) {
			try {
				throw new CollectionException(e);
			} catch (CollectionException e1) {
				e1.printStackTrace();
			}
		} finally {
			for (PrintWriter writer : writers)
				if (writer != null)
					writer.close();
		}
	}
}
