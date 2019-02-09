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
	public static final String HOME_DIRECTORY = "F:\\OneDrive\\Dissertation\\STS\\xyz.struthers.rhul.ham";
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
