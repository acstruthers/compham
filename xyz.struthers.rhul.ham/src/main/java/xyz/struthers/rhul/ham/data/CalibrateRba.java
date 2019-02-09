/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 29-Jan-2018
 */
public class CalibrateRba {

	private CalibrationData data;
	private Map<String, Double> rbaBalSht;
	private Map<String, Double> rbaProfitLoss;
	private ReserveBankOfAustralia rbaAgent;
	private AustralianEconomy economy;

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
		
		this.addAgentToEconomy();
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
	 * @return the rbaAgent
	 */
	public ReserveBankOfAustralia getRbaAgent() {
		return rbaAgent;
	}

}
