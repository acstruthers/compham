/**
 * 
 */
package xyz.struthers.rhul.ham.config;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import it.unimi.dsi.util.XoRoShiRo128PlusRandom;

/**
 * @author Adam Struthers
 * @since 19-Nov-2018
 */
@Component
@Scope(value = "singleton")
public class Properties implements Serializable {

	private static final long serialVersionUID = 1L;

	// scenario parameters
	/**
	 * In the event of Household default mortgage repayments are switched over to be
	 * rent payments that are this proportion of the original mortgage repayment
	 * amount. Assume a 30% drop in property prices, and rent being cheaper than
	 * loan repayments.
	 */
	public static final float MTG_RENT_CONVERSION_RATIO = 0.50f;
	/**
	 * In the event of a Household default they can draw down on their
	 * superannuation. However, due to the financial crisis its liquidation value
	 * has fallen and they must subtract this haircut from it when realising the
	 * cash.
	 */
	public static final float SUPERANNUATION_HAIRCUT = 0.30f;
	/**
	 * Due to the financial crisis the liquidation value of any financial
	 * investments has fallen and agents must subtract this haircut from it when
	 * realising the cash.
	 */
	public static final float INVESTMENT_HAIRCUT = 0.50f;
	/**
	 * Due to the financial crisis the liquidation value of foreign investments has
	 * fallen and agents must subtract this haircut from it when realising the cash.
	 */
	public static final float FOREIGN_INVESTMENT_HAIRCUT = 0.75f;
	/**
	 * If an Individual loses their job, their wages income is replaced by
	 * unemployment benefits. The monthly amount of these unemployment benefits is
	 * defined by this constant.
	 * 
	 * It is currently about $600 per fortnight.
	 */
	public static final float UNEMPLOYMENT_BENEFIT_PER_PERSON = 600f / 14f * 365f / 12f;

	// static config constants
	/*
	 * Use a classpath resource loader instead. One of these two options:
	 * InputStream in = Foobar.class.getClassLoader().getResourceAsStream(
	 * "data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv"); InputStream in =
	 * Foobar.class.getResourceAsStream(
	 * "/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
	 */
	public static final String HOME_DIRECTORY = "F:\\OneDrive\\Dissertation\\STS\\xyz.struthers.rhul.ham"; // lappy
	// public static final String HOME_DIRECTORY =
	// "D:\\Git\\compham\\xyz.struthers.rhul.ham"; // NUC
	public static final String RESOURCE_DIRECTORY = HOME_DIRECTORY + "\\src\\main\\resources";
	public static final String OUTPUT_DIRECTORY = "C:\\tmp\\";
	public static final String CSV_SEPARATOR = ",";

	// Kryonet networking parameters
	public static final String NETWORK_IPADDRESS_SERVER = "192.168.1.74";
	public static final int NETWORK_PORT_TCP = 1099; // 54555 default value
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;
	public static final int NETWORK_BUFFER_BYTES = 2000000000; // approx 2GB

	public static final float FCS_LIMIT_PER_ADI = 15000000000f; // AUD 15Bn limit per ADI
	public static final float FCS_LIMIT_PER_DEPOSITOR = 250000f; // AUD 250k limit per depositor
	public static final float ADI_HQLA_PROPORTION = 0.75f; // proportion of investments that are liquid
	public static final boolean ALLOW_NEGATIVE_RATES = false; // allow negative interest rates?
	public static final float SUPERANNUATION_RATE = 0.095f; // 9.5%
	public static final long RANDOM_SEED = 20190315L;

	// static data fields
	public static int peoplePerAgent = 1; // change to 1000 if 1 is too slow.
	public static int totalPopulationAU = 25000000;
	public static String timestamp;

	// data fields
	public Random random;

	private Properties() {
		super();
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd_HHmm");
		timestamp = simpleDateFormat.format(new Date(System.currentTimeMillis()));
	}

	/**
	 * @return the peoplePerAgent
	 */
	public static int getPeoplePerAgent() {
		return peoplePerAgent;
	}

	/**
	 * @param peoplePerAgent the peoplePerAgent to set
	 */
	public static void setPeoplePerAgent(int peoplePerAgent) {
		Properties.peoplePerAgent = peoplePerAgent;
	}

	/**
	 * @return the totalPopulationAU
	 */
	public static int getTotalPopulationAU() {
		return totalPopulationAU;
	}

	/**
	 * @param totalPopulationAU the totalPopulationAU to set
	 */
	public static void setTotalPopulationAU(int totalPopulationAU) {
		Properties.totalPopulationAU = totalPopulationAU;
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
			// this.random = new Random(RANDOM_SEED);
			this.random = new XoRoShiRo128PlusRandom(RANDOM_SEED);
		}
		return random;
	}

}
