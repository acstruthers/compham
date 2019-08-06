package xyz.struthers.rhul.ham;

public class ExampleMultiConfig {

	public ExampleMultiConfig() {
		super();
	}

	public static void main(String[] args) {
		ExampleMultiConfigSubClass.main(new String[] { "D:/compham-config/00_baseline.xml" });
		System.gc();
		ExampleMultiConfigSubClass.main(new String[] { "D:/compham-config/03_census2016_seed-01.xml" });
		System.gc();
		ExampleMultiConfigSubClass.main(new String[] { "D:/compham-config/03_census2016_seed-02.xml" });
		System.gc();
		ExampleMultiConfigSubClass.main(new String[] { "D:/compham-config/03_census2016_seed-03.xml" });
	}

}
