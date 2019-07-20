package xyz.struthers.rhul.ham.config;

import java.util.Date;

import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.data.Currencies;

/**
 * Tested and it both marshalls and unmarshalls correctly
 * 
 * @author Adam Struthers
 * @since 20-Jul-2019
 */
public class PropertiesXmlTestHarness {

	public PropertiesXmlTestHarness() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		System.out.println(new Date(System.currentTimeMillis()) + ": starting Properties XML test harness.");

		// writeBaselineToFile();
		readXmlFromFile("C:/diss-props/00_baseline.xml");

		System.out.println(new Date(System.currentTimeMillis()) + ": finished.");
	}

	private static void readXmlFromFile(String filename) {
		System.out.println(new Date(System.currentTimeMillis()) + ": reading XML from file.");
		PropertiesXml props = PropertiesXmlHandler.readPropertiesFromXmlFile(filename);
		System.out.println(new Date(System.currentTimeMillis()) + ": file read successfully.");

		System.out.println("VALUES ARE:");
		System.out.println("Scenario Name: " + props.getScenarioName());
		System.out.println("Save Iteration Flag 0: " + props.getSaveIterationSummary(0));
		System.out.println("Save Iteration Flag 1: " + props.getSaveIterationSummary(1));
		System.out.println("Save Iteration Flag 2: " + props.getSaveIterationSummary(2));
		System.out.println("Save Iteration Flag 3: " + props.getSaveIterationSummary(3));
		System.out.println("Save Iteration Flag 12: " + props.getSaveIterationSummary(12));
		System.out.println("Resource Directory: " + props.getResourceDirectory());
		System.out.println("SGC Rate: " + props.getSuperannuationGuaranteeRate());
		System.out.println("Foreign investment haircut: " + props.getForeignInvestmentHaircut());
		System.out.println("Random seed: " + props.getRandomSeed());
		System.out.println("Timestamp: " + props.getTimestamp());
		System.out.println("Interest Rate Custom Path 0: " + props.getInterestRateCustomPath(0));
		System.out.println("Interest Rate Custom Path 1: " + props.getInterestRateCustomPath(1));
		System.out.println("Interest Rate Custom Path 2: " + props.getInterestRateCustomPath(2));
		System.out.println("Interest Rate Custom Path 3: " + props.getInterestRateCustomPath(3));
		System.out.println("Interest Rate Custom Path 12: " + props.getInterestRateCustomPath(12));
	}

	private static void writeBaselineToFile() {
		System.out.println(new Date(System.currentTimeMillis()) + ": writing baseline to file.");
		PropertiesXml props = new PropertiesXml();

		// scenario descriptors
		props.setScenarioName("Baseline");
		props.setSaveIterationSummary(0, true);
		props.setSaveIterationSummary(1, true);
		props.setSaveIterationSummary(2, true);
		props.setSaveIterationSummary(3, true);
		props.setSaveIterationSummary(4, true);
		props.setSaveIterationSummary(5, true);
		props.setSaveIterationSummary(6, true);
		props.setSaveIterationSummary(7, true);
		props.setSaveIterationSummary(8, true);
		props.setSaveIterationSummary(9, true);
		props.setSaveIterationSummary(10, true);
		props.setSaveIterationSummary(11, true);
		props.setSaveIterationSummary(12, true);

		// static config constants
		props.setHomeDirectory("D:\\Git\\compham\\xyz.struthers.rhul.ham"); // NUC and new lappy
		props.setResourceDirectory(props.getHomeDirectory() + "\\src\\main\\resources");
		props.setOutputDirectory("D:\\compham-output\\");
		props.setCsvSeparator(",");

		// Socket networking parameters
		props.setCpvServerHost("Adam-E590");
		props.setEconomyClientHost("Adam-NUC");
		props.setCpvServerPort(1100);
		props.setEconomyClientPort(1100);
		props.setSocketBufferBytes(1000000000); // approx 1GB
		props.setSocketMessageBytes(10 * 1024 * 1024); // 10MB

		// unchanging simulation parameters
		props.setSuperannuationGuaranteeRate(0.095f); // 9.5%
		props.setRandomSeed(20180630L);

		// scenario parameters
		props.setMortgageRentConversionRatio(0.50f);
		props.setSuperannuationHaircut(0.30f);
		props.setInvestmentHaircut(0.50f);
		props.setForeignInvestmentHaircut(0.75f);
		props.setUnemploymentBenefitPerPerson(600f / 14f * 365f / 12f);
		props.setAdiHqlaProportion(0.75f); // proportion of investments that are liquid
		props.setAllowNegativerates(false); // allow negative interest rates?
		props.setFcsLimitPerAdi(15000000000f); // AUD 15Bn limit per ADI
		props.setFcsLimitPerDepositor(250000f); // AUD 250k limit per depositor
		props.setFxRateStrategy(Currencies.SAME);
		props.setInterestRateStrategy(ReserveBankOfAustralia.RATES_SAME);
		props.setInterestRateCustomPath(0, 1.50f);
		props.setInterestRateCustomPath(1, 1.50f);
		props.setInterestRateCustomPath(2, 1.50f);
		props.setInterestRateCustomPath(3, 1.25f);
		props.setInterestRateCustomPath(4, 1.25f);
		props.setInterestRateCustomPath(5, 1.00f);
		props.setInterestRateCustomPath(6, 1.00f);
		props.setInterestRateCustomPath(7, 0.75f);
		props.setInterestRateCustomPath(8, 0.75f);
		props.setInterestRateCustomPath(9, 0.50f);
		props.setInterestRateCustomPath(10, 0.50f);
		props.setInterestRateCustomPath(11, 0.25f);
		props.setInterestRateCustomPath(12, 0.25f);
		props.setHouseholdSavingRatio(3298f / 299456f); // about 1.1% per ABS 5206.0 Table 20

		System.out.println(new Date(System.currentTimeMillis()) + ": writing properties to XML file");

		PropertiesXmlHandler.writePropertiesToXmlFile(props, "C:/diss-props/00_baseline.xml");

		System.out.println(new Date(System.currentTimeMillis()) + ": baseline written to XML file");
	}
}
