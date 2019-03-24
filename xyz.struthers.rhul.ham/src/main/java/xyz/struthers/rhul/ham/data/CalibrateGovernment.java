/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 29-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class CalibrateGovernment {

	private static boolean DEBUG = false;

	// beans
	private CalibrationData data;
	private AustralianEconomy economy;

	// field variables
	private Map<String, Float> govtBalSht;
	private Map<String, Float> govtProfitLoss;
	private AustralianGovernment govtAgent;

	/**
	 * 
	 */
	public CalibrateGovernment() {
		super();
		this.init();
	}

	public void createGovernmentAgent() {
		this.govtBalSht = data.getGovtBalSht();
		this.govtProfitLoss = data.getGovtProfitLoss();

		if (DEBUG) {
			System.out.println("this.govtAgent: " + this.govtAgent);
			System.out.println("this.govtBalSht: " + this.govtBalSht);
			System.out.println("this.govtProfitLoss: " + this.govtProfitLoss);
		}

		this.govtAgent = new AustralianGovernment(this.govtBalSht, this.govtProfitLoss);
		this.govtAgent.setIndustryDivisionCode('O'); // Public Administration and Safety

		this.addAgentToEconomy();

		// release memory
		this.data.dropGovtFinancialData();
	}

	private void addAgentToEconomy() {
		this.economy.setGovernment(this.govtAgent);
	}

	private void init() {
		this.govtBalSht = null;
		this.govtProfitLoss = null;
		this.govtAgent = null;
	}

	/**
	 * @param data the calibration data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

	/**
	 * @return the govtAgent
	 */
	public AustralianGovernment getGovtAgent() {
		return govtAgent;
	}

}
