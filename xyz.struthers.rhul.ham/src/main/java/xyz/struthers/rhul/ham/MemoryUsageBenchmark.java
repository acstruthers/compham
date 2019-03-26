/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.opencsv.CSVReader;

/**
 * @author Adam
 *
 */
public class MemoryUsageBenchmark {

	public MemoryUsageBenchmark() {
	}

	/**
	 * Idea to test to reduce memory footprint Map the key to an index, then
	 * store everything else in an array. Create the arrays on the second reading of
	 * the file, when the data size is known exactly. Could even drop the original
	 * map and create another one with the minimum required initial capacity.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		double megabytesBefore = memoryBefore / 1024d / 1024d;
		System.out.println("MEMORY USAGE BEFORE: " + formatter.format(megabytesBefore) + "MB");

		Map<String, Map<String, String>> abs2074_0; // People and Dwellings count by Meshblock

		// System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS
		// 2074.0 census meshblock count data");
		abs2074_0 = new HashMap<String, Map<String, String>>();
		int[] abs2074_0_Columns = { 3, 4 };
		loadAbsDataCsv_2074_0("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv", abs2074_0_Columns,
				abs2074_0);

		System.gc();
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		double megabytesAfter = memoryAfter / 1024d / 1024d;
		// System.out.println("MEMORY USAGE AFTER: " + formatter.format(megabytesAfter)
		// + "MB");
		System.out.println("MEMORY CONSUMED BY MAPS: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		// #############################################################

		megabytesBefore = megabytesAfter;
		// System.out.println("MEMORY USAGE BEFORE: " +
		// formatter.format(megabytesBefore) + "MB");

		// System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS
		// 2074.0 census meshblock count data");
		Map<String, Integer> dataIndices = new HashMap<String, Integer>();
		ArrayList<ArrayList<String>> dataMatrix = new ArrayList<ArrayList<String>>(abs2074_0_Columns.length);
		ArrayList<ArrayList<String>> keys = new ArrayList<ArrayList<String>>(abs2074_0_Columns.length);
		loadAbsDataCsv_2074_0_UsingArrays("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv",
				abs2074_0_Columns, dataIndices, dataMatrix, keys);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024d / 1024d;
		System.out.println(
				"MEMORY CONSUMED BY STRING MATRICES: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		// #############################################################

		megabytesBefore = megabytesAfter;

		Map<String, Integer> dataIndicesDouble = new HashMap<String, Integer>();
		ArrayList<ArrayList<Double>> dataMatrixDouble = new ArrayList<ArrayList<Double>>(abs2074_0_Columns.length);
		ArrayList<ArrayList<String>> keysDouble = new ArrayList<ArrayList<String>>(abs2074_0_Columns.length);
		loadAbsDataCsv_2074_0_UsingDoubleArrays("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv",
				abs2074_0_Columns, dataIndicesDouble, dataMatrixDouble, keysDouble);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024d / 1024d;
		System.out.println(
				"MEMORY CONSUMED BY DOUBLE MATRICES: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		// #############################################################

		megabytesBefore = megabytesAfter;

		Map<String, Integer> dataIndicesInteger = new HashMap<String, Integer>();
		ArrayList<ArrayList<Integer>> dataMatrixInteger = new ArrayList<ArrayList<Integer>>(abs2074_0_Columns.length);
		ArrayList<ArrayList<String>> keysInteger = new ArrayList<ArrayList<String>>(abs2074_0_Columns.length);
		loadAbsDataCsv_2074_0_UsingIntegerArrays("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv",
				abs2074_0_Columns, dataIndicesInteger, dataMatrixInteger, keysInteger);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024d / 1024d;
		System.out.println(
				"MEMORY CONSUMED BY INTEGER MATRICES: " + formatter.format(megabytesAfter - megabytesBefore) + "MB");

		System.out.println("MEMORY USAGE AFTER: " + formatter.format(megabytesAfter) + "MB");
	}

	private static void loadAbsDataCsv_2074_0(String fileResourceLocation, int[] columnsToImport,
			Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					// store series ID
					for (int i = 0; i < columnsToImport.length; i++) {
						seriesId[i] = line[columnsToImport[i]];
						data.put(line[columnsToImport[i]], new HashMap<String, String>());
					}
					header = false;
				} else {
					if (!line[0].isBlank()) { // data exists, so import this row
						data.put(line[0], new HashMap<String, String>());
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(seriesId[i]).put(line[0], line[columnsToImport[i]]);
						}
					}
				}
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}
	}

	private static void loadAbsDataCsv_2074_0_UsingArrays(String fileResourceLocation, int[] columnsToImport,
			Map<String, Integer> dataIndices, ArrayList<ArrayList<String>> dataMatrix,
			ArrayList<ArrayList<String>> keys) {

		CSVReader reader = null;
		try {
			// determine the array sizes in the first pass
			InputStream is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			String[] seriesId = new String[columnsToImport.length];
			int dataRowCount = 0;
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					// store series ID
					for (int i = 0; i < columnsToImport.length; i++) {
						seriesId[i] = line[columnsToImport[i]];
						dataIndices.put(line[columnsToImport[i]], i);
					}
					header = false;
				} else {
					if (!line[0].isBlank()) { // data exists, so count this row
						dataRowCount++;
					}
				}
			}
			reader.close();

			// read the data into the arrays on the second pass
			is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			header = true;
			line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					// do nothing because the header was processed on the first reading
					keys.add(new ArrayList<String>(columnsToImport.length));
					for (int i = 0; i < columnsToImport.length; i++) {
						dataMatrix.add(new ArrayList<String>(dataRowCount));
						keys.get(0).add(line[columnsToImport[i]]);
					}
					dataMatrix.trimToSize();
					keys.get(0).trimToSize();
					header = false;
					dataRowCount = 0; // reset so I can use this as an index number
				} else {
					if (!line[0].isBlank()) { // data exists, so import this row
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							dataMatrix.get(i).add(line[columnsToImport[i]]);
						}
						dataRowCount++;
					}
				}
			}
			for (int i = 0; i < columnsToImport.length; i++) {
				dataMatrix.get(i).trimToSize();
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}
	}

	private static void loadAbsDataCsv_2074_0_UsingDoubleArrays(String fileResourceLocation, int[] columnsToImport,
			Map<String, Integer> dataIndices, ArrayList<ArrayList<Double>> dataMatrix,
			ArrayList<ArrayList<String>> keys) {

		CSVReader reader = null;
		try {
			// determine the array sizes in the first pass
			InputStream is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			int dataRowCount = 0;
			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					// store series ID
					for (int i = 0; i < columnsToImport.length; i++) {
						seriesId[i] = line[columnsToImport[i]];
						dataIndices.put(line[columnsToImport[i]], i);
					}
					header = false;
				} else {
					if (!line[0].isBlank()) { // data exists, so count this row
						dataRowCount++;
					} else {
						footer = true;
					}
				}
			}
			reader.close();
			reader = null;
			
			// read the data into the arrays on the second pass
			is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			header = true;
			footer = false;
			line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					// do nothing because the header was processed on the first reading
					keys.add(new ArrayList<String>(columnsToImport.length));
					for (int i = 0; i < columnsToImport.length; i++) {
						dataMatrix.add(new ArrayList<Double>(dataRowCount));
						keys.get(0).add(line[columnsToImport[i]]);
					}
					dataMatrix.trimToSize();
					keys.get(0).trimToSize();
					header = false;
					dataRowCount = 0; // reset so I can use this as an index number
				} else {
					if (!line[0].isBlank()) { // data exists, so import this row
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							//System.out.println("line: " + line);
							//System.out.println("line.length: " + line.length);
							//for (int j=0; j<line.length; j++) {
							//	System.out.println("line["+j+"]: " + line[j]);
							//}
							//System.out.println("line[columnsToImport[i]]: " + line[columnsToImport[i]]);
							double val = 0d;
							try {
								val = Double.valueOf(line[columnsToImport[i]]);
							} catch (NumberFormatException e) {
								val = 0d;
							}
							dataMatrix.get(i).add(val);
						}
						dataRowCount++;
					} else {
						footer = true;
					}
				}
			}
			for (int i = 0; i < columnsToImport.length; i++) {
				dataMatrix.get(i).trimToSize();
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}
	}
	
	private static void loadAbsDataCsv_2074_0_UsingIntegerArrays(String fileResourceLocation, int[] columnsToImport,
			Map<String, Integer> dataIndices, ArrayList<ArrayList<Integer>> dataMatrix,
			ArrayList<ArrayList<String>> keys) {

		CSVReader reader = null;
		try {
			// determine the array sizes in the first pass
			InputStream is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			int dataRowCount = 0;
			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					// store series ID
					for (int i = 0; i < columnsToImport.length; i++) {
						seriesId[i] = line[columnsToImport[i]];
						dataIndices.put(line[columnsToImport[i]], i);
					}
					header = false;
				} else {
					if (!line[0].isBlank()) { // data exists, so count this row
						dataRowCount++;
					} else {
						footer = true;
					}
				}
			}
			reader.close();
			reader = null;
			
			// read the data into the arrays on the second pass
			is = MemoryUsageBenchmark.class.getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			header = true;
			footer = false;
			line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					// do nothing because the header was processed on the first reading
					keys.add(new ArrayList<String>(columnsToImport.length));
					for (int i = 0; i < columnsToImport.length; i++) {
						dataMatrix.add(new ArrayList<Integer>(dataRowCount));
						keys.get(0).add(line[columnsToImport[i]]);
					}
					dataMatrix.trimToSize();
					keys.get(0).trimToSize();
					header = false;
					dataRowCount = 0; // reset so I can use this as an index number
				} else {
					if (!line[0].isBlank()) { // data exists, so import this row
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							int val = 0;
							try {
								val = Integer.valueOf(line[columnsToImport[i]]);
							} catch (NumberFormatException e) {
								val = 0;
							}
							dataMatrix.get(i).add(val);
						}
						dataRowCount++;
					} else {
						footer = true;
					}
				}
			}
			for (int i = 0; i < columnsToImport.length; i++) {
				dataMatrix.get(i).trimToSize();
			}
			reader.close();
			reader = null;
		} catch (FileNotFoundException e) {
			// open file
			e.printStackTrace();
		} catch (IOException e) {
			// read next
			e.printStackTrace();
		}
	}
}
