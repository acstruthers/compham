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

	public static int sample(double[] pdf, Random random) {
		double r = random.nextDouble();
		for (int i = 0; i < pdf.length; i++) {
			if (r < pdf[i])
				return i;
			r -= pdf[i];
		}
		return pdf.length - 1; // should not happen
	}
}
