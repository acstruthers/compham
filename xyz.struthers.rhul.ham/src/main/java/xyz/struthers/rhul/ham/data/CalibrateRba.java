/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 29-Jan-2018
 */
@Component
@Scope(value = "singleton")
public class CalibrateRba {

	// beans
	private CalibrationData data;
	private AustralianEconomy economy;

	// field variables
	private Map<String, Float> rbaBalSht;
	private Map<String, Float> rbaProfitLoss;
	private ReserveBankOfAustralia rbaAgent;

	/**
	 * Default constructor
	 */
	public CalibrateRba() {
		super();
		init();
	}

	public void createRbaAgent() {
		this.rbaBalSht = data.getRbaBalSht();
		this.rbaProfitLoss = data.getRbaProfitLoss();
		this.rbaAgent = new ReserveBankOfAustralia(this.rbaBalSht, this.rbaProfitLoss);
		this.rbaAgent.setIndustryDivisionCode('K'); // Financial and Insurance Services

		this.addAgentToEconomy();

		// release memory
		this.data.dropRbaFinancialData();
	}

	private void addAgentToEconomy() {
		this.economy.setRba(this.rbaAgent);
	}

	private void init() {
		this.rbaBalSht = null;
		this.rbaProfitLoss = null;
		this.rbaAgent = null;
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
	 * @return the rbaAgent
	 */
	public ReserveBankOfAustralia getRbaAgent() {
		return rbaAgent;
	}

}
