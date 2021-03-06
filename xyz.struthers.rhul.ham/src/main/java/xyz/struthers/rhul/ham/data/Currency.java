/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Random;

import gnu.trove.list.array.TFloatArrayList;

/**
 * @author Adam Struthers
 * @since 27-Jan-2019
 */
public class Currency implements Serializable {

	private static final long serialVersionUID = 1L;

	String iso4217code;
	String name;
	float avg1yr;
	float stdev1yr;
	float stdev5yr;
	TFloatArrayList exchangeRate;

	/**
	 * 
	 */
	public Currency() {
		super();
		this.exchangeRate = new TFloatArrayList();
	}

	public Currency(String isoCode, String currencyName, float fxRate, float average1yr, float standardDeviation1yr,
			float standardDeviation5yr) {
		super();
		this.iso4217code = isoCode;
		this.name = currencyName;
		this.avg1yr = average1yr;
		this.stdev1yr = standardDeviation1yr;
		this.stdev5yr = standardDeviation5yr;
		this.exchangeRate = new TFloatArrayList();
		this.exchangeRate.add(fxRate);
	}

	/**
	 * Gets the summary column headings, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the column headings
	 */
	public String toCsvSummaryStringHeaders(String separator) {
		StringBuilder sb = new StringBuilder();

		sb.append("ISO4217Code" + separator);
		sb.append("Name" + separator);
		sb.append("Mean1yr" + separator);
		sb.append("StdDev1yr" + separator);
		sb.append("StdDev5yr" + separator);
		sb.append("CurrentRate" + separator);
		sb.append("Iteration" + separator);

		return sb.toString();
	}

	/**
	 * Gets the summary data, to write to CSV file.
	 * 
	 * @param separator
	 * @return a CSV list of the data
	 */
	public String toCsvSummaryString(String separator, int iteration) {
		StringBuilder sb = new StringBuilder();

		DecimalFormat decimal = new DecimalFormat("###0.0000000000");
		DecimalFormat wholeNumber = new DecimalFormat("###0");
		// DecimalFormat percent = new DecimalFormat("###0.0000");

		sb.append(this.iso4217code + separator);
		sb.append(this.name + separator);
		sb.append(decimal.format(this.avg1yr) + separator);
		sb.append(decimal.format(this.stdev1yr) + separator);
		sb.append(decimal.format(this.stdev5yr) + separator);
		sb.append(decimal.format(this.exchangeRate.get(iteration)) + separator);
		sb.append(wholeNumber.format(iteration));

		return sb.toString();
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
			this.exchangeRate.add(exchangeRate);
		} else {
			result = 1;
			// this.exchangeRate.add(result);
		}
		return result;
	}

	/**
	 * Sets the exchange rate for this iteration to be the same as the previous
	 * exchange rate.
	 * 
	 * @param iteration
	 * @return the exchange rate
	 */
	public float setExchangeRateSame(int iteration) {
		float fxRate = 0f;
		if (this.exchangeRate.size() > iteration) {
			float prevFxRate = (iteration == 0 ? this.avg1yr : this.exchangeRate.get(iteration - 1));
			this.exchangeRate.set(iteration, prevFxRate);
		} else if (this.exchangeRate.size() == iteration) {
			float prevFxRate = (iteration == 0 ? this.avg1yr : this.exchangeRate.get(iteration - 1));
			this.exchangeRate.add(prevFxRate);
		} else {
			fxRate = 1f;
			// this.exchangeRate.add(fxRate);
		}
		return fxRate;
	}

	/**
	 * Generates the exchange rate for this iteration randomly, using the 1-year
	 * standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom1yr(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev1yr, 0);
	}

	/**
	 * Generates an upward movement in the exchange rate for this iteration
	 * randomly, using the 1-year standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom1yrUp(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev1yr, 1);
	}

	/**
	 * Generates a downward movement in the exchange rate for this iteration
	 * randomly, using the 1-year standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom1yrDown(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev1yr, -1);
	}

	/**
	 * Generates the exchange rate for this iteration randomly, using the 5-year
	 * standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom5yr(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev5yr, 0);
	}

	/**
	 * Generates an upward movement in the exchange rate for this iteration
	 * randomly, using the 5-year standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom5yrUp(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev5yr, 1);
	}

	/**
	 * Generates a downward movement in the exchange rate for this iteration
	 * randomly, using the 5-year standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @return - the exchange rate
	 */
	public float setExchangeRateRandom5yrDown(int iteration, Random random) {
		return this.setExchangeRateRandom(iteration, random, this.stdev5yr, -1);
	}

	/**
	 * Generates the exchange rate for this iteration randomly, using the specified
	 * standard deviation.
	 * 
	 * @param iteration
	 * @param random    - the pseudo-random number generator to use
	 * @param stdDev    - the standard deviation to use
	 * @param forceSign - force the change to be positive (forceSign > 0), negative
	 *                  (forceSign < 0), or leave it random (forceSign == 0).
	 * @return - the exchange rate
	 */
	private float setExchangeRateRandom(int iteration, Random random, float stdDev, int forceSign) {
		if (this.exchangeRate == null) {
			this.exchangeRate = new TFloatArrayList();
		}
		float prevFxRate = (iteration == 0 ? this.avg1yr : this.exchangeRate.get(iteration - 1));
		float fxRate = 1f;
		if (forceSign > 0) {
			fxRate = prevFxRate + (float) Math.abs(random.nextGaussian()) * stdDev;
		} else if (forceSign < 0) {
			fxRate = prevFxRate + (float) -Math.abs(random.nextGaussian()) * stdDev;
		} else {
			fxRate = prevFxRate + (float) random.nextGaussian() * stdDev;
		}
		if (this.exchangeRate.size() > iteration) {
			this.exchangeRate.set(iteration, fxRate);
		} else {
			for (int i = this.exchangeRate.size(); i < iteration; i++) {
				this.exchangeRate.add(fxRate);
				prevFxRate = fxRate;
				if (forceSign > 0) {
					fxRate = prevFxRate + (float) Math.abs(random.nextGaussian()) * stdDev;
				} else if (forceSign < 0) {
					fxRate = prevFxRate + (float) -Math.abs(random.nextGaussian()) * stdDev;
				} else {
					fxRate = prevFxRate + (float) random.nextGaussian() * stdDev;
				}
			}
		}
		return fxRate;
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
			newFxRate = (float) (oldFxRate + numStdDevs * this.stdev5yr);
		} else {
			// use 1-year standard deviation
			newFxRate = (float) (oldFxRate + numStdDevs * this.stdev1yr);
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

	public TFloatArrayList getExchangeRates() {
		return this.exchangeRate;
	}

}
