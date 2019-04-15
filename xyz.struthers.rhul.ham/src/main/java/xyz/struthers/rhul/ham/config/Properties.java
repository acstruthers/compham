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
	public static final String NETWORK_IPADDRESS_SERVER = "192.168.0.17";
	public static final int NETWORK_PORT_TCP = 54555;
	public static final int NETWORK_PORT_UDP = 54777;
	public static final int NETWORK_TIMEOUT_MILLIS = 5000;

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
