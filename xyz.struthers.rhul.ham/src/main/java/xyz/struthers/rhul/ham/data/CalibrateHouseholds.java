/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * Calibrates the P&L and Bal Shts of households by grouping together the P&L
 * and Bal Shts of individuals.
 * 
 * @author Adam Struthers
 * @since 10-Dec-2018
 */
@Component
@Scope(value = "singleton")
public class CalibrateHouseholds {

	// beans
	private CalibrationData data;
	private AreaMapping area;
	private AustralianEconomy economy;

	// households should have an LGA
	List<Household> householdAgents;

	/**
	 * Default constructor
	 */
	public CalibrateHouseholds() {
		super();
		this.init();
	}

	/**
	 * Works out household composition and how many of each to create, calibrates
	 * household financials, then adds them to the economy.
	 */
	public void createHouseholdAgents() {
		// TODO: implement me

		this.addAgentsToEconomy();
	}

	private void addAgentsToEconomy() {
		this.economy.setHouseholds(this.householdAgents);
	}

	private void init() {
		this.data = null;
		this.area = null;
		this.economy = null;

		this.householdAgents = null;
	}

	/**
	 * @param data
	 *            the data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param area
	 *            the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param economy
	 *            the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

}
