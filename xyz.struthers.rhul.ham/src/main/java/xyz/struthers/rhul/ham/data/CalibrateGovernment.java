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

	// beans
	private CalibrationData data;
	private AustralianEconomy economy;
	
	// field variables
	private Map<String, Double> govtBalSht;
	private Map<String, Double> govtProfitLoss;
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
		this.govtAgent = new AustralianGovernment(this.govtBalSht, this.govtProfitLoss);
		
		this.addAgentToEconomy();
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
	 * @param data
	 *            the calibration data to set
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
