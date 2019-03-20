/**
 * 
 */
package xyz.struthers.lang;

import java.util.Random;

/**
 * Custom mathematical functions that aren't available in the standard
 * Java.lang.Math library.
 * 
 * @author Adam Struthers
 * @since 2019-01-26
 */
public class CustomMath {

	private static final double EPSILON_LOAN = 0.0001f; // to compare interest rates in loan calculations

	/**
	 * 
	 */
	public CustomMath() {
		super();
	}

	/**
	 * Calculates the distance in km between two lat/long points using the haversine
	 * formula
	 * 
	 * SOURCE:
	 * https://stackoverflow.com/questions/7426710/how-to-find-the-distance-between-two-zipcodes-using-java-code
	 * 
	 * @author Glen Edmonds (Melbourne, VIC)
	 * 
	 * @param lat1 - Latitude of point 1.
	 * @param lng1 - Longitude of point 1.
	 * @param lat2 - Latitude of point 2.
	 * @param lng2 - Longitude of point 2.
	 * @return the great-circle distance in km between point 1 and point 2.
	 */
	public static final double haversine(double lat1, double lng1, double lat2, double lng2) {
		int r = 6371; // average radius of the earth in km
		double dLat = Math.toRadians(lat2 - lat1);
		double dLon = Math.toRadians(lng2 - lng1);
		double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
		double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = r * c;
		return d;
	}

	public static int sample(final double[] pdf, Random random) {
		double r = random.nextDouble();
		for (int i = 0; i < pdf.length; i++) {
			if (r < pdf[i])
				return i;
			r -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}

	public static int sample(final float[] pdf, Random random) {
		float r = random.nextFloat();
		for (int i = 0; i < pdf.length; i++) {
			if (r < pdf[i])
				return i;
			r -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}

	public static int sample(final double[] pdf, double random) {
		for (int i = 0; i < pdf.length; i++) {
			if (random < pdf[i])
				return i;
			random -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}

	public static int sample(final float[] pdf, float random) {
		for (int i = 0; i < pdf.length; i++) {
			if (random < pdf[i])
				return i;
			random -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}

	/**
	 * Calculates the original purchase price of a house/unit based on the mortgage
	 * repayments.
	 *
	 * SOURCE: https://en.wikipedia.org/wiki/Mortgage_calculator
	 * 
	 * @param repaymentAmount - the monthly repayment amount
	 * @param interestRate    - the annual interest rate divided by 12
	 * @param termMonths      - the term of the loan, expressed in months
	 * @return the original purchase price (in original nominal dollars)
	 */
	public float getPropertyPurchasePrice(float repaymentAmount, float interestRate, int termMonths) {
		float purchasePrice = 0f;
		if (Math.abs(interestRate - 0f) < EPSILON_LOAN) { // rate is 0%
			purchasePrice = repaymentAmount * (float) termMonths;
		} else {
			purchasePrice = repaymentAmount * (1f - (float) Math.pow(1d + interestRate, (double) -termMonths))
					/ interestRate;
		}
		return purchasePrice;
	}

	/**
	 * Calculates the current loan balance, assuming a constant interest rate and
	 * contractual repayments only (i.e. no additional repayments).
	 * 
	 * SOURCE: https://en.wikipedia.org/wiki/Mortgage_calculator
	 * 
	 * @param repaymentAmount
	 * @param interestRate
	 * @param purchasePrice
	 * @return the current loan balance
	 */
	public float getCurrentLoanBalance(float repaymentAmount, float interestRate, float purchasePrice,
			int currentMonth) {
		float currentBalance = 0f;
		if (Math.abs(interestRate - 0f) < EPSILON_LOAN) { // rate is 0%
			currentBalance = purchasePrice - repaymentAmount * (float) currentMonth;
		} else {
			currentBalance = purchasePrice * (float) Math.pow(1d + interestRate, (double) currentMonth)
					- ((float) Math.pow(1d + interestRate, (double) currentMonth) - 1f) * repaymentAmount
							/ interestRate;
		}
		return currentBalance;
	}

	/**
	 * Calculates the loan repayments, assuming a constant interest rate. This can
	 * be used to calculate the new loan repayments if/when interest rates change
	 * during the life of a loan.
	 * 
	 * @param interestRate
	 * @param purchasePrice
	 * @param termMonths
	 * @return the monthly loan repayments due
	 */
	public float getRepaymentAmount(Float interestRate, float originalLoanBalance, int termMonths) {
		float repaymentAmount = 0f;
		if (Math.abs(interestRate - 0f) < EPSILON_LOAN) { // rate is 0%
			repaymentAmount = originalLoanBalance / (float) termMonths;
		} else {
			repaymentAmount = (interestRate * originalLoanBalance
					* (float) Math.pow(1d + interestRate, (double) termMonths))
					/ ((float) Math.pow(1d + interestRate, (double) termMonths) - 1f);
		}
		return repaymentAmount;
	}

}
