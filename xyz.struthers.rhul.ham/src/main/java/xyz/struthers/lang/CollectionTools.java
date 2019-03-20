/**
 * 
 */
package xyz.struthers.lang;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Adam Struthers
 *
 */
public class CollectionTools {

	/**
	 * 
	 */
	public CollectionTools() {
		super();
	}

	static public final double sumDouble(Map<String, Double> data) {
		return data.values().stream().mapToDouble(Double::doubleValue).sum();
	}

	static public final double sumDouble(Collection<Double> data) {
		return data.stream().mapToDouble(Double::doubleValue).sum();
	}

	static public final float sumFloat(Map<String, Float> data) {
		return data.values().stream().reduce(0f, Float::sum);
	}

	static public final float sumFloat(Collection<Float> data) {
		return data.stream().reduce(0f, Float::sum);
	}

	static public final int sumInt(Map<String, Integer> data) {
		return data.values().stream().mapToInt(Integer::intValue).sum();
	}

	static public final int sumInt(Collection<Integer> data) {
		return data.stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Calculate each key as % of total.
	 * 
	 * @param data
	 * @return
	 */
	static public final Map<String, Float> calculatePercentageFromInteger(Map<String, Integer> data) {
		float total = Float.valueOf(data.values().stream().mapToInt(Integer::intValue).sum());
		Set<String> keySet = data.keySet();
		Map<String, Float> percent = new HashMap<String, Float>(keySet.size());
		for (String key : keySet) {
			percent.put(key, Float.valueOf(data.get(key)) / total);
		}
		return percent;
	}

	/**
	 * Calculate each key as % of total.
	 * 
	 * @param data
	 * @return
	 */
	static public final Map<String, Float> calculatePercentageFromDouble(Map<String, Float> data) {
		float total = (float) data.values().stream().mapToDouble(Float::doubleValue).sum();
		Set<String> keySet = data.keySet();
		Map<String, Float> percent = new HashMap<String, Float>(keySet.size());
		for (String key : keySet) {
			percent.put(key, Float.valueOf(data.get(key)) / total);
		}
		return percent;
	}

}
