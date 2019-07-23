package xyz.struthers.rhul.ham.config;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Adam Struthers
 * @since 23-Jul-2019
 */
//@XmlAccessorType(XmlAccessType.FIELD)
public class ExchangeRateList implements Serializable {

	private static final long serialVersionUID = 1L;

	// @XmlElementWrapper(name = "exchangeRateList")
	private ArrayList<Float> fxRate;

	public ExchangeRateList() {
		super();
		fxRate = null;
	}

	public ExchangeRateList(int size) {
		super();
		fxRate = new ArrayList<Float>(size);
	}

	public ExchangeRateList(ArrayList<Float> rates) {
		super();
		fxRate = rates;
	}

	/**
	 * @return the fxRate
	 */
	public ArrayList<Float> getFxRates() {
		return fxRate;
	}

	/**
	 * @param fxRate the fxRate to set
	 */
	public void setFxRates(ArrayList<Float> fxRates) {
		this.fxRate = fxRates;
	}

	public float getFxRate(int iteration) {
		return fxRate.get(iteration);
	}

	public void setFxRate(int iteration, float fxRate) {
		this.fxRate.set(iteration, fxRate);
	}

	public void addFxRate(float fxRate) {
		this.fxRate.add(fxRate);
	}
}
