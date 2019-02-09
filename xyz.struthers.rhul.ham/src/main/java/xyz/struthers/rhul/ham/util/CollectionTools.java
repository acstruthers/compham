/**
 * 
 */
package xyz.struthers.rhul.ham.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Adam
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
	static public final Map<String, Double> calculatePercentageFromInteger(Map<String, Integer> data) {
		double total = Double.valueOf(data.values().stream().mapToInt(Integer::intValue).sum());
		Set<String> keySet = data.keySet();
		Map<String, Double> percent = new HashMap<String, Double>(keySet.size());
		for (String key : keySet) {
			percent.put(key, Double.valueOf(data.get(key)) / total);
		}
		return percent;
	}

	/**
	 * Calculate each key as % of total.
	 * 
	 * @param data
	 * @return
	 */
	static public final Map<String, Double> calculatePercentageFromDouble(Map<String, Double> data) {
		double total = data.values().stream().mapToDouble(Double::doubleValue).sum();
		Set<String> keySet = data.keySet();
		Map<String, Double> percent = new HashMap<String, Double>(keySet.size());
		for (String key : keySet) {
			percent.put(key, Double.valueOf(data.get(key)) / total);
		}
		return percent;
	}

}
