/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

import xyz.struthers.rhul.ham.MemoryUsageBenchmark;
import xyz.struthers.rhul.ham.config.Properties;

/**
 * Loads ASGS boundary data from the ABS, downloaded in CSV format. Uses
 * meshblocks as the common field to map between Postal Area (POA), Local
 * Government Area (LGA) and Greater Capital City Statistical Area (GCCSA)
 * regions.
 * 
 * @author Adam Struthers
 * @since 2018-11-29
 */
@Component
@Scope(value = "singleton")
public class AreaMapping {

	// series names
	public static final String ABS_3222_0 = "ABS_3222.0"; // population projections

	// zero-based column numbers
	private static final int LGA_NAME_COL = 2;
	private static final int LGA_STATE_COL = 4;

	// column headings in CSV files
	private static final String ABS_GCCSA_CODE = "GCCSA_CODE_2016";
	private static final String LGA_LGA_CODE = "LGA_CODE_2018";
	private static final String POA_POA_CODE = "POA_CODE_2016";
	private static final int INDEX_COUNT_PERSON = 0;
	private static final int INDEX_COUNT_DWELLING = 1;

	// data variables
	private boolean dataLoaded;
	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;

	private Map<String, Map<String, String>> absData;
	private Map<String, Map<String, String>> lgaData;
	private Map<String, Map<String, String>> poaData;

	private Map<Date, Integer> totalPopulation;
	private Map<Date, Map<String, Integer>> adjustedPeopleByLga;
	private Map<String, Integer> abs2074_0indexMap;
	private ArrayList<ArrayList<Integer>> abs2074_0dataMatrix;
	private ArrayList<ArrayList<String>> abs2074_0seriesTitles;
	private Map<String, Map<Date, String>> abs3222_0; // AU by gender and age (population projections)

	// private Map<String, Double> postCodeLatitude;
	// private Map<String, Double> postCodeLongitude;
	// private Map<String, Double> lgaLatitude;
	// private Map<String, Double> lgaLongitude;
	private Map<String, String> mapLgaToGccsa;
	private Map<String, String> mapPoaToLga;
	private Map<String, String> mapLgaToPoa; // there are multiple post codes per LGA, but any one is enough for us to
												// determine which LGAs are close to each other
	private Map<String, String> mapLgaNameToCode;
	private Map<String, String> mapLgaCodeToName;
	private Map<String, String> mapLgaCodeToState;
	private Map<String, Set<String>> lgaMeshblocks; // key: LGA code, value: Meshblock codes
	private Map<String, Set<String>> poaMeshblocks; // key: POA code, value: Meshblock codes

	/**
	 * Default constructor. Initialises class variables.
	 */
	public AreaMapping() {
		super();
		this.init();
	}

	/**
	 * Gets the total Australian population as at a given date
	 * 
	 * @param date - Dates in the data file are MMM-yyyy, so date argument should be
	 *             the first day of each Month.
	 * @return total Australian population
	 */
	public int getTotalPopulation(Date date) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		if (this.totalPopulation == null) {
			this.totalPopulation = new HashMap<Date, Integer>(5);
		}
		Integer totalPop = this.totalPopulation.get(date);
		if (totalPop == null) {
			totalPop = 0;
			Set<String> seriesIds = this.abs3222_0.keySet();
			for (String series : seriesIds) {
				totalPop += Integer.valueOf(this.abs3222_0.get(series).get(date));
			}
			this.totalPopulation.put(date, totalPop);
		}
		Properties.setTotalPopulationAU(totalPop);
		return totalPop.intValue();
	}

	public Map<String, Integer> getAdjustedPeopleByLga(Date date) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		if (this.adjustedPeopleByLga == null) {
			this.adjustedPeopleByLga = new HashMap<Date, Map<String, Integer>>(5);
		}
		Map<String, Integer> result = this.adjustedPeopleByLga.get(date);
		if (result == null) {
			Map<String, Integer> censusPeopleByLga = this.getCensusPeopleByLga();
			Set<String> lgaSet = censusPeopleByLga.keySet();
			int totalCensusPopulation = 0;
			for (String lga : lgaSet) {
				totalCensusPopulation += censusPeopleByLga.get(lga);
			}
			result = new HashMap<String, Integer>(lgaSet.size());
			double factor = Double.valueOf(this.getTotalPopulation(date)) / Double.valueOf(totalCensusPopulation);
			for (String lga : lgaSet) {
				result.put(lga, (int) Math.round(Double.valueOf(censusPeopleByLga.get(lga)) * factor));
			}
		}
		return result;
	}

	public int getAdjustedPeopleByLga(String lgaCode, Date date) {
		return this.getAdjustedPeopleByLga(date).get(lgaCode);
	}

	/**
	 * 
	 * @return a map of the unadjusted number of people in each LGA, per the census.
	 */
	public Map<String, Integer> getCensusPeopleByLga() {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}

		Set<String> lgaSet = this.lgaMeshblocks.keySet();
		HashMap<String, Integer> result = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			int lgaPeople = 0;
			Set<String> lgaMb = this.lgaMeshblocks.get(lga);
			for (String mb : lgaMb) {
				lgaPeople += this.abs2074_0dataMatrix.get(INDEX_COUNT_PERSON).get(this.abs2074_0indexMap.get(mb));
			}
			result.put(lga, lgaPeople);
		}
		return result;
	}

	/**
	 * Gets the unadjusted number of people in a particular LGA, per the census.
	 * 
	 * @param lgaCode
	 * @return the number of people in the LGA
	 */
	public Integer getCensusPeopleByLga(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}

		int lgaPeople = 0;
		Set<String> lgaMb = this.lgaMeshblocks.get(lgaCode);
		for (String mb : lgaMb) {
			lgaPeople += this.abs2074_0dataMatrix.get(INDEX_COUNT_PERSON).get(this.abs2074_0indexMap.get(mb));
		}
		return lgaPeople;
	}

	/**
	 * 
	 * @return a map of the unadjusted number of dwellings in each LGA, per the
	 *         census.
	 */
	public Map<String, Integer> getCensusDwellingsByLga() {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}

		Set<String> lgaSet = this.lgaMeshblocks.keySet();
		HashMap<String, Integer> result = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			int lgaDwellings = 0;
			Set<String> lgaMb = this.lgaMeshblocks.get(lga);
			for (String mb : lgaMb) {
				lgaDwellings += this.abs2074_0dataMatrix.get(INDEX_COUNT_DWELLING).get(this.abs2074_0indexMap.get(mb));
			}
			result.put(lga, lgaDwellings);
		}
		return result;
	}

	/**
	 * Gets the unadjusted number of dwellings in a particular LGA, per the census.
	 * 
	 * @param lgaCode
	 * @return the number of dwellings in the LGA
	 */
	public Integer getCensusDwellingsByLga(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}

		int lgaDwellings = 0;
		Set<String> lgaMb = this.lgaMeshblocks.get(lgaCode);
		for (String mb : lgaMb) {
			lgaDwellings += this.abs2074_0dataMatrix.get(INDEX_COUNT_DWELLING).get(this.abs2074_0indexMap.get(mb));
		}
		return lgaDwellings;
	}

	/**
	 * 
	 * @param meshblockCode - Meshblock code
	 * @return Local Government Area (LGA) code
	 */
	public String getLgaCodeFromMeshblock(String meshblockCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.absData.get(LGA_LGA_CODE).get(meshblockCode);
	}

	/**
	 * 
	 * @param poaCode - Postal Area (POA) code
	 * @return Local Government Area (LGA) code
	 */
	public String getLgaCodeFromPoa(String poaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapPoaToLga.get(poaCode);
	}

	/**
	 * 
	 * @param lgaCode - Local Government Area (LGA) code
	 * @return Postal Area (POA) code
	 */
	public String getPoaCodeFromLga(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapLgaToPoa.get(lgaCode);
	}

	/**
	 * 
	 * @param lgaCode - Local Government Area (LGA) code
	 * @return Greater Capital City Statistical Area (GCCSA) code
	 */
	public String getGccsaCodeFromLga(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapLgaToGccsa.get(lgaCode);
	}

	public String getLgaNameFromCode(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapLgaCodeToName.get(lgaCode);
	}

	public String getLgaCodeFromName(String lgaName) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapLgaNameToCode.get(lgaName);
	}

	public String getLgaStateFromCode(String lgaCode) {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return this.mapLgaCodeToState.get(lgaCode);
	}

	// TODO: boolean isInGccsa(String lgaCode)

	/**
	 * Generates meshblock mapping between LGA, POA and GCCSA.
	 * 
	 */
	private void mapMeshblocks() {
		this.loadMeshblocks();

		// map LGA to GCCSA
		Set<String> lgaSet = new HashSet<String>(this.lgaData.get(AreaMapping.LGA_LGA_CODE).values());
		this.mapLgaToGccsa = new HashMap<String, String>(lgaSet.size());
		this.mapSmallToBigArea(this.lgaData, AreaMapping.LGA_LGA_CODE, this.absData, AreaMapping.ABS_GCCSA_CODE,
				this.mapLgaToGccsa);

		// map POA to LGA
		// TODO: extend this to map LGA to POA too (any POA code within the LGA is fine)
		Set<String> poaSet = new HashSet<String>(this.poaData.get(AreaMapping.POA_POA_CODE).values());
		this.mapPoaToLga = new HashMap<String, String>(poaSet.size());
		this.mapSmallToBigArea(this.poaData, AreaMapping.POA_POA_CODE, this.lgaData, AreaMapping.LGA_LGA_CODE,
				this.mapPoaToLga);

		this.dataLoaded = true;
	}

	/**
	 * Maps from a small statistical area to a large one. If there are more than one
	 * large area represented by the mesh blocks in the small area, the one with the
	 * largest population is chosen.
	 * 
	 * @param fromData
	 * @param fromTitle
	 * @param toData
	 * @param toTitle
	 * @param newMap
	 */
	private void mapSmallToBigArea(Map<String, Map<String, String>> fromData, String fromTitle,
			Map<String, Map<String, String>> toData, String toTitle, Map<String, String> newMap) {
		// map LGA to GCCSA
		Set<String> fromKeySet = new HashSet<String>(fromData.get(fromTitle).values());
		for (String fromKey : fromKeySet) {
			/*
			 * // https://stackoverflow.com/questions/10462819/get-keys-from-hashmap-in-java
			 * List<String> fromMeshblocks = fromData.get(fromTitle).entrySet().stream()
			 * .filter(e -> e.getValue().equals(fromKey)).map(Map.Entry::getKey)
			 * .collect(Collectors.toList());
			 */
			List<String> fromMeshblocks = null;
			if (fromTitle.equals(AreaMapping.LGA_LGA_CODE)) {
				fromMeshblocks = new ArrayList<String>(this.lgaMeshblocks.get(fromKey));
			}
			if (fromTitle.equals(AreaMapping.POA_POA_CODE)) {
				fromMeshblocks = new ArrayList<String>(this.poaMeshblocks.get(fromKey));
			}

			// assign the From Code to the To Code with the greatest population count
			Map<String, Integer> toPeopleCount = new HashMap<String, Integer>(); // maps LGA:count
			for (String mb : fromMeshblocks) {
				String toCode = toData.get(toTitle).get(mb);
				int toCodeCount = toPeopleCount.containsKey(toCode) ? toPeopleCount.get(toCode) : 0;
				int newCount = this.abs2074_0dataMatrix.get(INDEX_COUNT_PERSON).get(this.abs2074_0indexMap.get(mb));
				toPeopleCount.put(toCode, toCodeCount + newCount); // update count in map
			}

			// find To Code with the largest population, and map the From Code to it
			String largestPopulationCode = null;
			int largestPopulation = 0;
			for (String toCode : toPeopleCount.keySet()) {
				if (toPeopleCount.get(toCode) > largestPopulation) {
					largestPopulation = toPeopleCount.get(toCode);
					largestPopulationCode = toCode;
				}
			}

			// store mapping of From Code to To Code
			newMap.put(fromKey, largestPopulationCode);
		}

	}

	/**
	 * Loads all Meshblock CSV files into memory.
	 */
	private void loadMeshblocks() {
		this.title = new HashMap<String, List<String>>();
		this.unitType = new HashMap<String, List<String>>();
		this.lgaMeshblocks = new HashMap<String, Set<String>>();
		this.poaMeshblocks = new HashMap<String, Set<String>>();

		// load ABS meshblock data (MB, SA1, SA2, SA3, SA4, GCCSA, State)
		final boolean[] absLoadColumn = { false, false, false, false, false, false, false, false, false, false, false,
				true, true, true, true, false };
		this.absData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_NSW.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_NT.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_OT.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_QLD.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_SA.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_TAS.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_VIC.csv", this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_WA.csv", this.absData, absLoadColumn);

		// load LGA data
		this.mapLgaNameToCode = new HashMap<String, String>();
		this.mapLgaCodeToName = new HashMap<String, String>();
		this.mapLgaCodeToState = new HashMap<String, String>();

		final boolean[] lgaLoadColumn = { false, true, true, false, false, false };
		this.lgaData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_NSW.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_VIC.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_QLD.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_SA.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_WA.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_TAS.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_NT.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_ACT.csv", this.lgaData,
				lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_OT.csv", this.lgaData,
				lgaLoadColumn);

		// load POA data
		final boolean[] poaLoadColumn = { false, true, false, false };
		this.poaData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/POA_2016_AUST.csv", this.poaData,
				poaLoadColumn);

		// load mesh block counts
		final int[] abs2074_0_Columns = { 3, 4 };
		this.abs2074_0indexMap = new HashMap<String, Integer>();
		this.abs2074_0dataMatrix = new ArrayList<ArrayList<Integer>>(abs2074_0_Columns.length);
		this.abs2074_0seriesTitles = new ArrayList<ArrayList<String>>(abs2074_0_Columns.length);
		this.loadAbsDataCsv_2074_0("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv",
				abs2074_0_Columns, this.abs2074_0indexMap, this.abs2074_0dataMatrix, this.abs2074_0seriesTitles);

		// load ABS 3222.0 data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 3222.0 Income data");
		this.abs3222_0 = new HashMap<String, Map<Date, String>>();
		int[] abs3220_0Columns = { 203, 204, 205, 206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219,
				220, 221, 222, 223, 224, 225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240,
				241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254, 255, 256, 257, 258, 259, 260, 261,
				262, 263, 264, 265, 266, 267, 268, 269, 270, 271, 272, 273, 274, 275, 276, 277, 278, 279, 280, 281, 282,
				283, 284, 285, 286, 287, 288, 289, 290, 291, 292, 293, 294, 295, 296, 297, 298, 299, 300, 301, 302,
				303 }; // loads count of Persons 0 - 100
		this.loadAbsDataCsv_Catalogue(
				"/data/ABS/3222.0_PopnProjections/Table B9. Population projections - Series B.csv", ABS_3222_0,
				abs3220_0Columns, this.title, this.unitType, this.abs3222_0);

		// load postcode latitude and longitude
		/*
		 * this.postCodeLatitude = new HashMap<String, Double>(); this.postCodeLongitude
		 * = new HashMap<String, Double>(); this.readPostCodeLatLongDataCsv(
		 * Properties.RESOURCE_DIRECTORY +
		 * "\\data\\Corra\\Australian_Post_Codes_Lat_Lon.csv");
		 */
	}

	/**
	 * Reads in a single Meshblock CSV file.
	 * 
	 * @param fileResourceLocation - the URI to the CSV file
	 * @param title                - the column titles in the CSV file
	 * @param data                 - the data rows in the CSV file
	 * @param loadColumn           - boolean values to include (true) or exclude
	 *                             (false) each column in the CSV file when reading
	 *                             them. It always excludes the first column
	 *                             regardless of the value in loadColumn[0].
	 */
	private void readMeshblockCsvData(String fileResourceLocation, Map<String, Map<String, String>> data,
			boolean[] loadColumn) {

		CSVReader reader = null;
		try {
			// open file
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));

			// read column headings from row 1
			String[] title = reader.readNext();
			for (int i = 1; i < title.length; i++) {
				// exclude first column because we don't need to store meshblocks separately
				// because they're the key in each column's data mapping
				if (loadColumn[i] && !data.containsKey(title[i])) { // only add this if it hasn't been added before - we
																	// don't want to overwrite the previous files' data
					data.put(title[i], new HashMap<String, String>()); // store column heading as key
				}
			}

			// read data rows, starting at row 2
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (!line[0].isEmpty()) {
					for (int i = 1; i < line.length; i++) {
						// parse the body of the data
						if (loadColumn[i]) {
							data.get(title[i]).put(line[0], line[i]); // assumes line[0] is the Meshblock Code
						}
						if (title[i].equals(LGA_LGA_CODE)) {
							// add faster mapping for LGA: a set of the meshblocks that it comprises of
							if (!this.lgaMeshblocks.containsKey(line[i])) {
								this.lgaMeshblocks.put(line[i], new HashSet<String>());
							}
							this.lgaMeshblocks.get(line[i]).add(line[0]);

							// map LGA code to name
							if (!this.mapLgaCodeToName.containsKey(line[i])) {
								this.mapLgaCodeToName.put(line[i], line[LGA_NAME_COL]);
							}

							// map LGA code to state
							if (!this.mapLgaCodeToState.containsKey(line[i])) {
								String thisState = null;
								switch (line[LGA_STATE_COL].toUpperCase()) {
								case "NEW SOUTH WALES":
									thisState = "NSW";
									break;
								case "VICTORIA":
									thisState = "VIC";
									break;
								case "QUEENSLAND":
									thisState = "QLD";
									break;
								case "SOUTH AUSTRALIA":
									thisState = "SA";
									break;
								case "WESTERN AUSTRALIA":
									thisState = "WA";
									break;
								case "TASMANIA":
									thisState = "TAS";
									break;
								case "NORTHERN TERRITORY":
									thisState = "NT";
									break;
								case "AUSTRALIAN CAPITAL TERRITORY":
									thisState = "ACT";
									break;
								default:
									thisState = "Other";
								}
								this.mapLgaCodeToState.put(line[i], thisState);
							}

							// map LGA name to code
							if (!this.mapLgaNameToCode.containsKey(line[LGA_NAME_COL])) {
								this.mapLgaNameToCode.put(line[LGA_NAME_COL], line[i]);
							}
						}
						if (title[i].equals(AreaMapping.POA_POA_CODE)) {
							// add faster mapping for POA: a set of the meshblocks that it comprises of
							if (!this.poaMeshblocks.containsKey(line[i])) {
								this.poaMeshblocks.put(line[i], new HashSet<String>());
							}
							this.poaMeshblocks.get(line[i]).add(line[0]);
						}
					}
				} else {
					break; // data has finished, so break out of loop before copyright notice, etc.
				}
			}
			// close reader
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

	/*
	 * Loads a CSV file that contains a list of post codes and their latitudes and
	 * longitudes.
	 * 
	 * 
	 * @param fileURI - the URI of the file to import
	 */
	/*
	 * private void readPostCodeLatLongDataCsv(String fileURI) { CSVReader reader =
	 * null; try { reader = new CSVReader(new FileReader(fileURI)); String[] line =
	 * null; line = reader.readNext(); // discard header row while ((line =
	 * reader.readNext()) != null) { // parse the body of the data
	 * this.postCodeLatitude.put(line[0], Double.valueOf(line[5]));
	 * this.postCodeLongitude.put(line[0], Double.valueOf(line[6])); } } catch
	 * (FileNotFoundException e) { // open file e.printStackTrace(); } catch
	 * (IOException e) { // read next e.printStackTrace(); } }
	 */

	private void loadAbsDataCsv_2074_0(String fileResourceLocation, int[] columnsToImport,
			Map<String, Integer> indexMap, ArrayList<ArrayList<Integer>> dataMatrix,
			ArrayList<ArrayList<String>> seriesTitles) {

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
					seriesTitles.add(new ArrayList<String>(columnsToImport.length));
					for (int i = 0; i < columnsToImport.length; i++) {
						dataMatrix.add(new ArrayList<Integer>(dataRowCount));
						seriesTitles.get(0).add(line[columnsToImport[i]]);
					}
					dataMatrix.trimToSize();
					seriesTitles.get(0).trimToSize();
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
						indexMap.put(line[0], dataRowCount++); // Key: meshblock, Value: index
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

	/**
	 * Loads ABS pre-prepared catalogue data.
	 * 
	 * File pre-conditions:<br>
	 * 1. The first row contains the column titles.<br>
	 * 2. The unit type row has "Unit" in the first column.<br>
	 * 3. The last header row has "Series ID" in the first column.<br>
	 * 4. The first column contains the dates in the format MMM-yyyy.
	 * 
	 * Catalogues this works for include: ABS3222.0
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAbsDataCsv_Catalogue(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units, Map<String, Map<Date, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean titleRow = true;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			DateFormat dateFormat = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH);
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (titleRow) {
						// store title
						titles.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(catalogueName).add(line[columnsToImport[i]]);
						}
						titleRow = false;
					} else if (line[0].equals("Unit")) {
						// store unit types
						units.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(catalogueName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new HashMap<Date, String>());
						}
						header = false;
					}
				} else {
					for (int i = 0; i < columnsToImport.length; i++) {
						// parse the body of the data
						data.get(seriesId[i]).put(dateFormat.parse(line[0]), line[columnsToImport[i]]);
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
		} catch (ParseException e) {
			// parsing date from string
			e.printStackTrace();
		}
	}

	/**
	 * Initialises class variables.
	 */
	@PostConstruct
	private void init() {
		this.dataLoaded = false;
		this.totalPopulation = null;
		this.adjustedPeopleByLga = null;

		this.absData = null;
		this.lgaData = null;
		this.poaData = null;

		this.abs2074_0indexMap = null;
		this.abs2074_0dataMatrix = null;
		this.abs2074_0seriesTitles = null;
		this.abs3222_0 = null;

		this.mapLgaToGccsa = null;
		this.mapPoaToLga = null;
		this.mapLgaToPoa = null;
		this.mapLgaNameToCode = null;
		this.mapLgaCodeToName = null;
		this.mapLgaCodeToState = null;
		this.lgaMeshblocks = null;
		this.poaMeshblocks = null;
	}

	/**
	 * @return the abs3222_0
	 */
	public Map<String, Map<Date, String>> getAbs3222_0() {
		if (!this.dataLoaded) {
			this.mapMeshblocks();
		}
		return abs3222_0;
	}
}
