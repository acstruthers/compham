package xyz.struthers.rhul.ham.config;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

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

		// set properties filename
		String filename = args[0];
		filename = "D:/compham-config/00_baseline-test-harness.xml";

		writeBaselineToFile(filename); // "D:/compham-config/00_baseline-test-harness.xml"
		// readXmlFromFile("D:/compham-config/00_baseline.xml");

		System.out.println(new Date(System.currentTimeMillis()) + ": finished.");
	}

	public static void readXmlFromFile(String filename) {
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

	private static void writeBaselineToFile(String filename) {
		System.out.println(new Date(System.currentTimeMillis()) + ": writing baseline to file.");
		PropertiesXml props = new PropertiesXml();

		// scenario descriptors
		props.setScenarioName("Baseline");
		props.setSaveIterationSummary(0, true);
		props.setSaveIterationSummary(1, false);
		props.setSaveIterationSummary(2, false);
		props.setSaveIterationSummary(3, false);
		props.setSaveIterationSummary(4, false);
		props.setSaveIterationSummary(5, false);
		props.setSaveIterationSummary(6, false);
		props.setSaveIterationSummary(7, false);
		props.setSaveIterationSummary(8, false);
		props.setSaveIterationSummary(9, false);
		props.setSaveIterationSummary(10, false);
		props.setSaveIterationSummary(11, false);
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

		// scenario parameters - bank crash only
		props.setBankCrashScenario(true);
		props.setFcsAdiLimit(20000000000f);
		props.setFcsCustomerLimit(250000f);
		props.setCrashedBankShortNames("CBA");
		props.setCrashedBankIndustry("Mutual ADI");

		// scenario parameters
		props.setExogeneousIncomeMultiplier(0.5f);
		props.setExogeneousExpenseMultiplier(0.5f);
		props.setInflationRatePerAnnum(0.1f);
		props.setMortgageRentConversionRatio(0.50f);
		props.setSuperannuationHaircut(0.30f);
		props.setInvestmentHaircut(0.50f);
		props.setForeignInvestmentHaircut(0.75f);
		props.setUnemploymentBenefitPerPerson(600f / 14f * 365f / 12f);
		props.setAdiHqlaProportion(0.75f); // proportion of investments that are liquid
		props.setAllowNegativerates(false); // allow negative interest rates?
		props.setFcsLimitPerAdi(15000000000f); // AUD 15Bn limit per ADI
		props.setFcsLimitPerDepositor(250000f); // AUD 250k limit per depositor
		props.setFxRateStrategy(Currencies.SAME); // SAME = 0, CUSTOM = 7

		props.addFxRateCustomPath("USD", 0.7391f);
		props.addFxRateCustomPath("USD", 0.7391f);
		props.addFxRateCustomPath("USD", 0.7391f);
		props.addFxRateCustomPath("CAD", 0.9771f);
		props.addFxRateCustomPath("CAD", 0.9771f);
		props.addFxRateCustomPath("CAD", 0.9771f);

		// FIXME: add code elsewhere to actually read the custom FX rates and use them
		// in the model

		props.setInterestRateStrategy(ReserveBankOfAustralia.RATES_SAME); // SAME = 0, CUSTOM = 1
		props.setInitialCashRate(1.5f);
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
		props.setUseActualWages(true);

		// data sources
		// props.setDataSubFolder("2016"); // the census year the data corresponds to
		// (for calibration)
		props.setFilenames(new HashMap<String, String>());
		props.putFilename("ABS", "/data/2016/ABS/");
		props.putFilename("ABS/CensusTableBuilder", "/data/2016/ABS/CensusTableBuilder2016/");
		props.putFilename("ABS/1270.0.55.001_AbsMeshblock", "/data/2016/ABS/1270.0.55.001_AbsMeshblock/MB_2016");
		// props.putFilename("ABS/1292.0.55.002_ANZSIC",
		// "/data/2016/ABS/1292.0.55.002_ANZSIC/");
		props.putFilename("ABS/1270.0.55.003_NonAbsMeshblock/LGA",
				"/data/2016/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018");
		props.putFilename("ABS/1270.0.55.003_NonAbsMeshblock/POA",
				"/data/2016/ABS/1270.0.55.003_NonAbsMeshblock/POA_2016");
		props.putFilename("ABS/2074.0_MeshblockCounts", "/data/2016/ABS/2074.0_MeshblockCounts/2016");
		// props.putFilename("ABS/3222.0_PopnProjections",
		// "/data/2016/ABS/3222.0_PopnProjections/");
		// props.putFilename("ABS/5368.0_IntlTrade",
		// "/data/2016/ABS/5368.0_IntlTrade/");
		// props.putFilename("ABS/5368.0.55.006_Exporters",
		// "/data/2016/ABS/5368.0.55.006_Exporters/");
		// props.putFilename("ABS/5512.0_GovtFinStats",
		// "/data/2016/ABS/5512.0_GovtFinStats/");
		// props.putFilename("ABS/5676.0_BusinessIndicators",
		// "/data/2016/ABS/5676.0_BusinessIndicators/");
		// props.putFilename("ABS/6524.0.55.002_IncomeByLGA",
		// "/data/2016/ABS/6524.0.55.002_IncomeByLGA/");
		// props.putFilename("ABS/8155.0_IndustryByDivision",
		// "/data/2016/ABS/8155.0_IndustryByDivision/");
		// props.putFilename("ABS/8165.0_CountOfBusinesses",
		// "/data/2016/ABS/8165.0_CountOfBusinesses/");
		// props.putFilename("ABS/8167.0_BusMktAndComp",
		// "/data/2016/ABS/8167.0_BusMktAndComp/");
		props.putFilename("ADI", "/data/2016/ADI/");
		props.putFilename("ATO", "/data/2016/ATO/");
		// props.putFilename("ATO/Company", "/data/2016/ATO/Company/");
		// props.putFilename("ATO/Individual", "/data/2016/ATO/Individual/");
		props.putFilename("FxRates", "/data/2016/FxRates/");
		props.putFilename("RBA", "/data/2016/RBA/");
		// props.putFilename("RBA/E_HouseholdBusiness",
		// "/data/2016/RBA/E_HouseholdBusiness/");
		props.setRbaE1DateString("Jun-2018");
		props.setAbs1410Year("2016");
		props.setAbs8155Year("2016-17");
		props.setCalibrationDateAbs("01/06/2018");
		props.setCalibrationDateAto("01/06/2018");
		props.setCalibrationDateRba("01/06/2018");
		props.setHouseholdMultiplier(4f);
		props.setPopulationMultiplier(1.2f);

		System.out.println(new Date(System.currentTimeMillis()) + ": writing properties to XML file");

		PropertiesXmlHandler.writePropertiesToXmlFile(props, filename);

		System.out.println(new Date(System.currentTimeMillis()) + ": baseline written to XML file");
	}
}
