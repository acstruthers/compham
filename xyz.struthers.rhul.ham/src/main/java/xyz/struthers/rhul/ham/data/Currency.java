/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.Random;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public class Currency {

	String iso4217code;
	String name;
	float avg1yr;
	float stdev1yr;
	float stdev5yr;
	ArrayList<Float> exchangeRate;

	/**
	 * 
	 */
	public Currency() {
		super();
		this.exchangeRate = new ArrayList<Float>();
	}

	public Currency(String isoCode, String currencyName, float fxRate, float average1yr, float standardDeviation1yr,
			float standardDeviation5yr) {
		super();
		this.iso4217code = isoCode;
		this.name = currencyName;
		this.avg1yr = average1yr;
		this.stdev1yr = standardDeviation1yr;
		this.stdev5yr = standardDeviation5yr;
		this.exchangeRate = new ArrayList<Float>();
		this.exchangeRate.add(fxRate);
	}

	/**
	 * @return the iso4217code
	 */
	public String getIso4217code() {
		return iso4217code;
	}

	/**
	 * @param iso4217code the iso4217code to set
	 */
	public void setIso4217code(String iso4217code) {
		this.iso4217code = iso4217code;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the exchangeRate
	 */
	public float getExchangeRate(int iteration) {
		return exchangeRate.get(iteration);
	}

	/**
	 * @param exchangeRate the exchangeRate to set
	 */
	public int setExchangeRate(int iteration, float exchangeRate) {
		int result = 0;
		if (this.exchangeRate.size() > iteration) {
			this.exchangeRate.set(iteration, exchangeRate);
		} else if (this.exchangeRate.size() == iteration) {
			this.exchangeRate.set(iteration, exchangeRate);
		} else {
			result = 1;
		}
		return result;
	}

	/**
	 * Generates the next FX rate, using a Gaussian ("normal") distribution.
	 * 
	 * @param random       - the random number genreator to use
	 * @param use5yrStdDev - true to use 5-year standard deviation, false to use
	 *                     1-year standard deviation.
	 * @return the next "random" FX rate
	 */
	public float generateNextGaussianFxRate(Random random, boolean use5yrStdDev) {
		int lastIdx = this.exchangeRate.size() - 1;
		float oldFxRate = this.exchangeRate.get(lastIdx);
		double numStdDevs = random.nextGaussian();
		float newFxRate = 0f;
		if (use5yrStdDev) {
			// use 5-year standard deviation
			oldFxRate = (float) (oldFxRate + numStdDevs * this.stdev5yr);
		} else {
			// use 1-year standard deviation
			oldFxRate = (float) (oldFxRate + numStdDevs * this.stdev1yr);
		}
		return newFxRate;
	}

	/**
	 * @return the avg1yr
	 */
	public float getAvg1yr() {
		return avg1yr;
	}

	/**
	 * @param avg1yr the avg1yr to set
	 */
	public void setAvg1yr(float avg1yr) {
		this.avg1yr = avg1yr;
	}

	/**
	 * @return the stdev1yr
	 */
	public float getStdev1yr() {
		return stdev1yr;
	}

	/**
	 * @param stdev1yr the stdev1yr to set
	 */
	public void setStdev1yr(float stdev1yr) {
		this.stdev1yr = stdev1yr;
	}

	/**
	 * @return the stdev5yr
	 */
	public float getStdev5yr() {
		return stdev5yr;
	}

	/**
	 * @param stdev5yr the stdev5yr to set
	 */
	public void setStdev5yr(float stdev5yr) {
		this.stdev5yr = stdev5yr;
	}

}
