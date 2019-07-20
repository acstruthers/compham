package xyz.struthers.rhul.ham.config;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

/**
 * FIXME: convert Properties to be class variables so that it can be written to
 * XML and read from XML. This will allow me to have all the parameters for my
 * scenarios saved to file and included in the Git repository, which makes the
 * results reproducible. It also means I can run several scenarios sequentially
 * without any manual intervention.
 * 
 * @author Adam Struthers
 * @since 19-Nov-2018
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class PropertiesXml implements Serializable {

	private static final long serialVersionUID = 1L;

	// scenario descriptors
	private String scenarioName;
	private ArrayList<SaveIterationSummary> saveIterationSummary;
	private int numberOfIterations; // first iteration is zero

	// static config constants
	/*
	 * Use a classpath resource loader instead. One of these two options:
	 * InputStream in = Foobar.class.getClassLoader().getResourceAsStream(
	 * "data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv"); InputStream in =
	 * Foobar.class.getResourceAsStream(
	 * "/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
	 */
	private String homeDirectory;
	private String resourceDirectory;// = homeDirectory + "\\src\\main\\resources";
	private String outputDirectory;// = "D:\\compham-output\\";
	private String csvSeparator;// = ",";

	// Socket networking parameters
	private String cpvServerHost;// = "Adam-E590";
	private String economyClientHost;// = "Adam-NUC";
	private int cpvServerPort;// = 1100;
	private int economyClientPort;// = 1100;
	private int socketBufferBytes;// = 1000000000; // approx 1GB
	private int socketMessageBytes;// = 10 * 1024 * 1024; // 10MB

	// unchanging simulation parameters
	private float superannuationGuaranteeRate;// = 0.095f; // 9.5%
	private long randomSeed;// = 20180630L;
	// private int peoplePerAgent = 1; // change to 1000 if 1 is too slow.
	// private int totalPopulationAU = 25000000;

	// scenario parameters
	/**
	 * In the event of Household default mortgage repayments are switched over to be
	 * rent payments that are this proportion of the original mortgage repayment
	 * amount. Assume a 30% drop in property prices, and rent being cheaper than
	 * loan repayments.
	 */
	private float mortgageRentConversionRatio;// = 0.50f;
	/**
	 * In the event of a Household default they can draw down on their
	 * superannuation. However, due to the financial crisis its liquidation value
	 * has fallen and they must subtract this haircut from it when realising the
	 * cash.
	 */
	private float superannuationHaircut;// = 0.30f;
	/**
	 * Due to the financial crisis the liquidation value of any financial
	 * investments has fallen and agents must subtract this haircut from it when
	 * realising the cash.
	 */
	private float investmentHaircut;// = 0.50f;
	/**
	 * Due to the financial crisis the liquidation value of foreign investments has
	 * fallen and agents must subtract this haircut from it when realising the cash.
	 */
	private float foreignInvestmentHaircut;// = 0.75f;
	/**
	 * If an Individual loses their job, their wages income is replaced by
	 * unemployment benefits. The monthly amount of these unemployment benefits is
	 * defined by this constant.
	 * 
	 * It is currently about $600 per fortnight.
	 */
	private float unemploymentBenefitPerPerson;// = 600f / 14f * 365f / 12f;
	private float adiHqlaProportion;// = 0.75f; // proportion of investments that are liquid
	private boolean allowNegativerates;// = false; // allow negative interest rates?
	private float fcsLimitPerAdi;// = 15000000000f; // AUD 15Bn limit per ADI
	private float fcsLimitPerDepositor;// = 250000f; // AUD 250k limit per depositor
	private int fxRateStrategy;// = Currencies.SAME;
	private int interestRateStrategy;// = ReserveBankOfAustralia.RATES_SAME;
	private ArrayList<InterestRate> interestRateCustomPath;
	private float householdSavingRatio;// = 3298f / 299456f; // about 1.1% per ABS 5206.0 Table 20

	// transient fields
	@XmlTransient
	private String timestamp; // when the properties were created
	@XmlTransient
	public Random random;

	public PropertiesXml() {
		super();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmm");
		timestamp = simpleDateFormat.format(new Date(System.currentTimeMillis()));
	}

	/**
	 * @return the scenarioName
	 */
	public String getScenarioName() {
		return scenarioName;
	}

	/**
	 * @param scenarioName the scenarioName to set
	 */
	public void setScenarioName(String scenarioName) {
		this.scenarioName = scenarioName;
	}

	/**
	 * @return the saveIterationSummary
	 */
	public ArrayList<SaveIterationSummary> getSaveIterationSummary() {
		return saveIterationSummary;
	}

	/**
	 * @param saveIterationSummary the saveIterationSummary to set
	 */
	public void setSaveIterationSummary(ArrayList<SaveIterationSummary> saveIterationSummary) {
		this.saveIterationSummary = saveIterationSummary;
		this.numberOfIterations = this.saveIterationSummary.size();
	}

	public boolean getSaveIterationSummary(int iteration) {
		return this.saveIterationSummary.get(iteration).isSaveFlag();
	}

	/**
	 * Assumes that iteration flags are added in order. If you try to add a flag to
	 * an index larger than the current size, it will just add it to the next
	 * available index instead.
	 * 
	 * @param iteration
	 * @param flag
	 */
	public void setSaveIterationSummary(int iteration, boolean flag) {
		if (this.saveIterationSummary == null) {
			this.saveIterationSummary = new ArrayList<SaveIterationSummary>(iteration);
		}
		if (this.saveIterationSummary.size() > iteration) {
			this.saveIterationSummary.set(iteration, new SaveIterationSummary(flag));
		} else {
			this.saveIterationSummary.add(new SaveIterationSummary(flag));
		}
		this.numberOfIterations = this.saveIterationSummary.size();
	}

	/**
	 * @return the numberOfIterations
	 */
	public int getNumberOfIterations() {
		return numberOfIterations;
	}

	/**
	 * @param numberOfIterations the numberOfIterations to set
	 * @deprecated
	 */
	public void setNumberOfIterations(int numberOfIterations) {
		this.numberOfIterations = numberOfIterations;
	}

	/**
	 * @return the homeDirectory
	 */
	public String getHomeDirectory() {
		return homeDirectory;
	}

	/**
	 * @param homeDirectory the homeDirectory to set
	 */
	public void setHomeDirectory(String homeDirectory) {
		this.homeDirectory = homeDirectory;
	}

	/**
	 * @return the resourceDirectory
	 */
	public String getResourceDirectory() {
		return resourceDirectory;
	}

	/**
	 * @param resourceDirectory the resourceDirectory to set
	 */
	public void setResourceDirectory(String resourceDirectory) {
		this.resourceDirectory = resourceDirectory;
	}

	/**
	 * @return the outputDirectory
	 */
	public String getOutputDirectory() {
		return outputDirectory;
	}

	/**
	 * @param outputDirectory the outputDirectory to set
	 */
	public void setOutputDirectory(String outputDirectory) {
		this.outputDirectory = outputDirectory;
	}

	/**
	 * @return the csvSeparator
	 */
	public String getCsvSeparator() {
		return csvSeparator;
	}

	/**
	 * @param csvSeparator the csvSeparator to set
	 */
	public void setCsvSeparator(String csvSeparator) {
		this.csvSeparator = csvSeparator;
	}

	/**
	 * @return the cpvServerHost
	 */
	public String getCpvServerHost() {
		return cpvServerHost;
	}

	/**
	 * @param cpvServerHost the cpvServerHost to set
	 */
	public void setCpvServerHost(String cpvServerHost) {
		this.cpvServerHost = cpvServerHost;
	}

	/**
	 * @return the economyClientHost
	 */
	public String getEconomyClientHost() {
		return economyClientHost;
	}

	/**
	 * @param economyClientHost the economyClientHost to set
	 */
	public void setEconomyClientHost(String economyClientHost) {
		this.economyClientHost = economyClientHost;
	}

	/**
	 * @return the cpvServerPort
	 */
	public int getCpvServerPort() {
		return cpvServerPort;
	}

	/**
	 * @param cpvServerPort the cpvServerPort to set
	 */
	public void setCpvServerPort(int cpvServerPort) {
		this.cpvServerPort = cpvServerPort;
	}

	/**
	 * @return the economyClientPort
	 */
	public int getEconomyClientPort() {
		return economyClientPort;
	}

	/**
	 * @param economyClientPort the economyClientPort to set
	 */
	public void setEconomyClientPort(int economyClientPort) {
		this.economyClientPort = economyClientPort;
	}

	/**
	 * @return the socketBufferBytes
	 */
	public int getSocketBufferBytes() {
		return socketBufferBytes;
	}

	/**
	 * @param socketBufferBytes the socketBufferBytes to set
	 */
	public void setSocketBufferBytes(int socketBufferBytes) {
		this.socketBufferBytes = socketBufferBytes;
	}

	/**
	 * @return the socketMessageBytes
	 */
	public int getSocketMessageBytes() {
		return socketMessageBytes;
	}

	/**
	 * @param socketMessageBytes the socketMessageBytes to set
	 */
	public void setSocketMessageBytes(int socketMessageBytes) {
		this.socketMessageBytes = socketMessageBytes;
	}

	/**
	 * @return the superannuationGuaranteeRate
	 */
	public float getSuperannuationGuaranteeRate() {
		return superannuationGuaranteeRate;
	}

	/**
	 * @param superannuationGuaranteeRate the superannuationGuaranteeRate to set
	 */
	public void setSuperannuationGuaranteeRate(float superannuationGuaranteeRate) {
		this.superannuationGuaranteeRate = superannuationGuaranteeRate;
	}

	/**
	 * @return the randomSeed
	 */
	public long getRandomSeed() {
		return randomSeed;
	}

	/**
	 * @param randomSeed the randomSeed to set
	 */
	public void setRandomSeed(long seed) {
		this.randomSeed = seed;
		this.setRandom(seed);
	}

	/**
	 * @return the timestamp
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * @param timestamp the timestamp to set
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * @return the mortgageRentConversionRatio
	 */
	public float getMortgageRentConversionRatio() {
		return mortgageRentConversionRatio;
	}

	/**
	 * @param mortgageRentConversionRatio the mortgageRentConversionRatio to set
	 */
	public void setMortgageRentConversionRatio(float mortgageRentConversionRatio) {
		this.mortgageRentConversionRatio = mortgageRentConversionRatio;
	}

	/**
	 * @return the superannuationHaircut
	 */
	public float getSuperannuationHaircut() {
		return superannuationHaircut;
	}

	/**
	 * @param superannuationHaircut the superannuationHaircut to set
	 */
	public void setSuperannuationHaircut(float superannuationHaircut) {
		this.superannuationHaircut = superannuationHaircut;
	}

	/**
	 * @return the investmentHaircut
	 */
	public float getInvestmentHaircut() {
		return investmentHaircut;
	}

	/**
	 * @param investmentHaircut the investmentHaircut to set
	 */
	public void setInvestmentHaircut(float investmentHaircut) {
		this.investmentHaircut = investmentHaircut;
	}

	/**
	 * @return the foreignInvestmentHaircut
	 */
	public float getForeignInvestmentHaircut() {
		return foreignInvestmentHaircut;
	}

	/**
	 * @param foreignInvestmentHaircut the foreignInvestmentHaircut to set
	 */
	public void setForeignInvestmentHaircut(float foreignInvestmentHaircut) {
		this.foreignInvestmentHaircut = foreignInvestmentHaircut;
	}

	/**
	 * @return the unemploymentBenefitPerPerson
	 */
	public float getUnemploymentBenefitPerPerson() {
		return unemploymentBenefitPerPerson;
	}

	/**
	 * @param unemploymentBenefitPerPerson the unemploymentBenefitPerPerson to set
	 */
	public void setUnemploymentBenefitPerPerson(float unemploymentBenefitPerPerson) {
		this.unemploymentBenefitPerPerson = unemploymentBenefitPerPerson;
	}

	/**
	 * @return the adiHqlaProportion
	 */
	public float getAdiHqlaProportion() {
		return adiHqlaProportion;
	}

	/**
	 * @param adiHqlaProportion the adiHqlaProportion to set
	 */
	public void setAdiHqlaProportion(float adiHqlaProportion) {
		this.adiHqlaProportion = adiHqlaProportion;
	}

	/**
	 * @return the allowNegativerates
	 */
	public boolean isAllowNegativerates() {
		return allowNegativerates;
	}

	/**
	 * @param allowNegativerates the allowNegativerates to set
	 */
	public void setAllowNegativerates(boolean allowNegativerates) {
		this.allowNegativerates = allowNegativerates;
	}

	/**
	 * @return the fcsLimitPerAdi
	 */
	public float getFcsLimitPerAdi() {
		return fcsLimitPerAdi;
	}

	/**
	 * @param fcsLimitPerAdi the fcsLimitPerAdi to set
	 */
	public void setFcsLimitPerAdi(float fcsLimitPerAdi) {
		this.fcsLimitPerAdi = fcsLimitPerAdi;
	}

	/**
	 * @return the fcsLimitPerDepositor
	 */
	public float getFcsLimitPerDepositor() {
		return fcsLimitPerDepositor;
	}

	/**
	 * @param fcsLimitPerDepositor the fcsLimitPerDepositor to set
	 */
	public void setFcsLimitPerDepositor(float fcsLimitPerDepositor) {
		this.fcsLimitPerDepositor = fcsLimitPerDepositor;
	}

	/**
	 * @return the fxRateStrategy
	 */
	public int getFxRateStrategy() {
		return fxRateStrategy;
	}

	/**
	 * @param fxRateStrategy the fxRateStrategy to set
	 */
	public void setFxRateStrategy(int fxRateStrategy) {
		this.fxRateStrategy = fxRateStrategy;
	}

	/**
	 * @return the interestRateStrategy
	 */
	public int getInterestRateStrategy() {
		return interestRateStrategy;
	}

	/**
	 * @param interestRateStrategy the interestRateStrategy to set
	 */
	public void setInterestRateStrategy(int interestRateStrategy) {
		this.interestRateStrategy = interestRateStrategy;
	}

	/**
	 * @return the interestRateCustomPath
	 */
	public ArrayList<InterestRate> getInterestRateCustomPath() {
		return interestRateCustomPath;
	}

	/**
	 * @param interestRateCustomPath the interestRateCustomPath to set
	 */
	public void setInterestRateCustomPath(ArrayList<InterestRate> interestRateCustomPath) {
		this.interestRateCustomPath = interestRateCustomPath;
	}

	public float getInterestRateCustomPath(int iteration) {
		return this.interestRateCustomPath.get(iteration).getInterestRate();
	}

	public void setInterestRateCustomPath(int iteration, float rate) {
		if (this.interestRateCustomPath == null) {
			this.interestRateCustomPath = new ArrayList<InterestRate>(iteration);
		}
		if (this.interestRateCustomPath.size() > iteration) {
			this.interestRateCustomPath.set(iteration, new InterestRate(rate));
		} else {
			this.interestRateCustomPath.add(new InterestRate(rate));
		}
	}

	/**
	 * @return the householdSavingRatio
	 */
	public float getHouseholdSavingRatio() {
		return householdSavingRatio;
	}

	/**
	 * @param householdSavingRatio the householdSavingRatio to set
	 */
	public void setHouseholdSavingRatio(float householdSavingRatio) {
		this.householdSavingRatio = householdSavingRatio;
	}

	/**
	 * The XoRoShiRo128PlusRandom random number generator. It is a sub-class of
	 * Random, so will work anywhere that Random will, but it's faster and with a
	 * better statistical distribution.
	 * 
	 * SOURCES:<br>
	 * https://stackoverflow.com/questions/29193371/fast-real-valued-random-generator-in-java<br>
	 * http://dsiutils.di.unimi.it/docs/it/unimi/dsi/util/XoRoShiRo128PlusRandom.html<br>
	 * 
	 * @return the random
	 */
	public Random getRandom() {
		if (this.random == null) {
			// this.random = new Random(this.randomSeed);
			this.random = new XoRoShiRo128PlusRandom(this.randomSeed);
		}
		return this.random;
	}

	public void setRandom(Random random) {
		this.random = random;
	}

	/**
	 * The XoRoShiRo128PlusRandom random number generator. It is a sub-class of
	 * Random, so will work anywhere that Random will, but it's faster and with a
	 * better statistical distribution.
	 * 
	 * SOURCES:<br>
	 * https://stackoverflow.com/questions/29193371/fast-real-valued-random-generator-in-java<br>
	 * http://dsiutils.di.unimi.it/docs/it/unimi/dsi/util/XoRoShiRo128PlusRandom.html<br>
	 */
	public void setRandom(long seed) {
		this.randomSeed = seed;
		this.random = new XoRoShiRo128PlusRandom(this.randomSeed);
	}
}
