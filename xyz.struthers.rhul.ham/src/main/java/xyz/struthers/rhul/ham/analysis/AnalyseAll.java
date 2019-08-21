package xyz.struthers.rhul.ham.analysis;

/**
 * @author Adam Struthers
 * @since 2019-08-20
 */
public class AnalyseAll {

	public AnalyseAll() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		AnalyseExogeneous.main(args);
		AnalyseMetrics.main(args);
		AnalyseLgas.main(args);
		AnalyseHomeOwnership.main(args);
		// AnalyseRealIncome.main(args);
		AnalyseDefaults.main(args);
	}
}
