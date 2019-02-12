/**
 * 
 */
package xyz.struthers.rhul.ham.config;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * @author Adam Struthers
 * @since 19-Nov-2018
 */
@Component
@Scope(value = "singleton")
public class Properties {

	// static config constants
	/*
	 * Use a classpath resource loader instead. One of these two options:
	 * InputStream in = Foobar.class.getClassLoader().getResourceAsStream("data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
	 * InputStream in = Foobar.class.getResourceAsStream("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv");
	 */
	public static final String HOME_DIRECTORY = "F:\\OneDrive\\Dissertation\\STS\\xyz.struthers.rhul.ham"; // lappy
	//public static final String HOME_DIRECTORY = "D:\\Git\\compham\\xyz.struthers.rhul.ham"; // NUC
	public static final String RESOURCE_DIRECTORY = HOME_DIRECTORY + "\\src\\main\\resources";
	

	// static data fields
	public static int peoplePerAgent = 1; // change to 1000 if 1 is too slow.
	public static int totalPopulationAU = 25000000;

	private Properties() {
		super();
	}

	/**
	 * @return the peoplePerAgent
	 */
	public static int getPeoplePerAgent() {
		return peoplePerAgent;
	}

	/**
	 * @param peoplePerAgent
	 *            the peoplePerAgent to set
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
	 * @param totalPopulationAU
	 *            the totalPopulationAU to set
	 */
	public static void setTotalPopulationAU(int totalPopulationAU) {
		Properties.totalPopulationAU = totalPopulationAU;
	}
}
