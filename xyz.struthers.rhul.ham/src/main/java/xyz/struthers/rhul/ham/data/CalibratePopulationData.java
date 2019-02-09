/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.util.CollectionTools;

/**
 * Loads population data, and calibrates number and percentage of people in each
 * LGA.
 * 
 * DATA SOURCES: ABS 3222.0 ABS 1270.0.55.001 (indirectly) ABS 1270.0.55.003
 * (indirectly)
 * 
 * @author Adam Struthers
 * @since 03-Dec-2018
 */
public class CalibratePopulationData {

	@Autowired
	private CalibrationData data;
	@Autowired
	private AreaMapping mapping;
	
	Map<String, Integer> peopleByLga;
	Map<String, Double> peopleByLgaPercent;

	/**
	 * 
	 */
	public CalibratePopulationData() {
		super();
		init();
	}

	/**
	 * Gets the percentage of all people living in each LGA, per census data.
	 * 
	 * @return a map of doubles between 0 and 1.
	 */
	public Map<String, Double> populationPercentByLga() {
		// get people by LGA (per census)
		Map<String, Integer> peopleByLga = this.mapping.getCensusPeopleByLga();

		// calculate % of people in each LGA
		return CollectionTools.calculatePercentageFromInteger(peopleByLga);
	}

	/**
	 * Gets the number of people in each LGA, using the census population
	 * distribution, adjusted for the current total population.
	 * 
	 * @return
	 */
	public Map<String, Integer> populationByLga(Date date) {

		// get percentage of people in each LGA
		Map<String, Double> lgaPercent = this.populationPercentByLga();

		// get total population
		this.data = new CalibrationData();
		double totalPop = Double.valueOf(this.data.getTotalPopulation(date));

		// multiply total by percentage to get current number of people in each LGA
		Set<String> lgaSet = lgaPercent.keySet();
		Map<String, Integer> lgaPeopleCount = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			lgaPeopleCount.put(lga, (int) Math.round(lgaPercent.get(lga) * totalPop));
		}

		return lgaPeopleCount;
	}

	/**
	 * Gets the number of representative agents per LGA, using the People Per Agent
	 * parameter from the Properties class. It uses the census population
	 * distribution, adjusted for the current total population.
	 * 
	 * @param date
	 * @return the number of representative agents per LGA
	 */
	public Map<String, Integer> householdAgentsByLga(Date date) {
		int peoplePerAgent = Properties.getPeoplePerAgent();
		Map<String, Integer> popByLga = this.populationByLga(date);
		Set<String> lgaSet = popByLga.keySet();
		Map<String, Integer> householdAgents = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			householdAgents.put(lga, (int) Math.round(popByLga.get(lga) / Double.valueOf(peoplePerAgent)));
		}
		return householdAgents;
	}

	@PostConstruct
	private void init() {
		this.peopleByLga = null;
		this.peopleByLgaPercent = null;
	}

}
