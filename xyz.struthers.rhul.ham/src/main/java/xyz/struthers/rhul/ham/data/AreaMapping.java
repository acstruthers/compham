/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

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

	// column headings in CSV files
	private static final String ABS_GCCSA_CODE = "GCCSA_CODE_2016";
	private static final String LGA_LGA_CODE = "LGA_CODE_2018";
	private static final String POA_POA_CODE = "POA_CODE_2016";
	private static final String COUNT_PERSON = "Person";
	private static final String COUNT_DWELLING = "Dwelling";

	// data variables
	private boolean dataMapped;
	private Map<String, Map<String, String>> absData;
	private Map<String, Map<String, String>> lgaData;
	private Map<String, Map<String, String>> poaData;
	private Map<String, Map<String, String>> countData;
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
	 * 
	 * @return a map of the unadjusted number of people in each LGA, per the census.
	 */
	public Map<String, Integer> getCensusPeopleByLga() {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}

		Set<String> lgaSet = this.lgaMeshblocks.keySet();
		HashMap<String, Integer> result = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			int lgaPeople = 0;
			Set<String> lgaMb = this.lgaMeshblocks.get(lga);
			for (String mb : lgaMb) {
				lgaPeople += Integer.valueOf(this.countData.get(AreaMapping.COUNT_PERSON).get(mb));
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
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}

		int lgaPeople = 0;
		Set<String> lgaMb = this.lgaMeshblocks.get(lgaCode);
		for (String mb : lgaMb) {
			lgaPeople += Integer.valueOf(this.countData.get(AreaMapping.COUNT_PERSON).get(mb));
		}
		return lgaPeople;
	}

	/**
	 * 
	 * @return a map of the unadjusted number of dwellings in each LGA, per the
	 *         census.
	 */
	public Map<String, Integer> getCensusDwellingsByLga() {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}

		Set<String> lgaSet = this.lgaMeshblocks.keySet();
		HashMap<String, Integer> result = new HashMap<String, Integer>(lgaSet.size());
		for (String lga : lgaSet) {
			int lgaDwellings = 0;
			Set<String> lgaMb = this.lgaMeshblocks.get(lga);
			for (String mb : lgaMb) {
				lgaDwellings += Integer.valueOf(this.countData.get(AreaMapping.COUNT_DWELLING).get(mb));
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
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}

		int lgaDwellings = 0;
		Set<String> lgaMb = this.lgaMeshblocks.get(lgaCode);
		for (String mb : lgaMb) {
			lgaDwellings += Integer.valueOf(this.countData.get(AreaMapping.COUNT_DWELLING).get(mb));
		}
		return lgaDwellings;
	}

	/**
	 * 
	 * @param poaCode
	 *            - Postal Area (POA) code
	 * @return Local Government Area (LGA) code
	 */
	public String getLgaCodeFromPoa(String poaCode) {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}
		return this.mapPoaToLga.get(poaCode);
	}

	/**
	 * 
	 * @param lgaCode
	 *            - Local Government Area (LGA) code
	 * @return Postal Area (POA) code
	 */
	public String getPoaCodeFromLga(String lgaCode) {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}
		return this.mapLgaToPoa.get(lgaCode);
	}
	
	/**
	 * 
	 * @param lgaCode
	 *            - Local Government Area (LGA) code
	 * @return Greater Capital City Statistical Area (GCCSA) code
	 */
	public String getGccsaCodeFromLga(String lgaCode) {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}
		return this.mapLgaToGccsa.get(lgaCode);
	}

	public String getLgaNameFromCode(String lgaCode) {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}
		return this.mapLgaCodeToName.get(lgaCode);
	}

	public String getLgaCodeFromName(String lgaName) {
		if (!this.dataMapped) {
			this.mapMeshblocks();
		}
		return this.mapLgaNameToCode.get(lgaName);
	}

	// TODO: String getStateFromLga(String lgaCode)

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

		this.dataMapped = true;
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
				int newCount = Integer.valueOf(this.countData.get(AreaMapping.COUNT_PERSON).get(mb));
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
		this.lgaMeshblocks = new HashMap<String, Set<String>>();
		this.poaMeshblocks = new HashMap<String, Set<String>>();

		// load ABS meshblock data (MB, SA1, SA2, SA3, SA4, GCCSA, State)
		final boolean[] absLoadColumn = { false, false, false, false, false, false, false, false, false, false, false,
				true, true, true, true, false };
		this.absData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_ACT.csv",
				this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_NSW.csv",
				this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_NT.csv", this.absData,
				absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_OT.csv", this.absData,
				absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_QLD.csv",
				this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_SA.csv", this.absData,
				absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_TAS.csv",
				this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_VIC.csv",
				this.absData, absLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.001_AbsMeshblock/MB_2016_WA.csv", this.absData,
				absLoadColumn);

		// load LGA data
		this.mapLgaNameToCode = new HashMap<String, String>();
		this.mapLgaCodeToName = new HashMap<String, String>();

		final boolean[] lgaLoadColumn = { false, true, true, false, false, false };
		this.lgaData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_ACT.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_NSW.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_NT.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_OT.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_QLD.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_SA.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_TAS.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_VIC.csv",
				this.lgaData, lgaLoadColumn);
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/LGA_2018_WA.csv",
				this.lgaData, lgaLoadColumn);

		// load POA data
		final boolean[] poaLoadColumn = { false, true, false, false };
		this.poaData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/1270.0.55.003_NonAbsMeshblock/POA_2016_AUST.csv",
				this.poaData, poaLoadColumn);

		// load mesh block counts
		final boolean[] countLoadColumn = { false, false, false, true, true, false };
		this.countData = new HashMap<String, Map<String, String>>();
		this.readMeshblockCsvData("/data/ABS/2074.0_MeshblockCounts/2016 Census Mesh Block Counts.csv",
				this.countData, countLoadColumn);

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
	 * @param fileResourceLocation
	 *            - the URI to the CSV file
	 * @param title
	 *            - the column titles in the CSV file
	 * @param data
	 *            - the data rows in the CSV file
	 * @param loadColumn
	 *            - boolean values to include (true) or exclude (false) each column
	 *            in the CSV file when reading them. It always excludes the first
	 *            column regardless of the value in loadColumn[0].
	 */
	private void readMeshblockCsvData(String fileResourceLocation, Map<String, Map<String, String>> data, boolean[] loadColumn) {

		final int LGA_NAME_COL = 4;

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
						if (title[i].equals(AreaMapping.LGA_LGA_CODE)) {
							// add faster mapping for LGA: a set of the meshblocks that it comprises of
							if (!this.lgaMeshblocks.containsKey(line[i])) {
								this.lgaMeshblocks.put(line[i], new HashSet<String>());
							}
							this.lgaMeshblocks.get(line[i]).add(line[0]);

							// map LGA code to name
							if (!this.mapLgaCodeToName.containsKey(line[i])) {
								this.mapLgaCodeToName.put(line[i], line[LGA_NAME_COL]);
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

	/**
	 * Initialises class variables.
	 */
	@PostConstruct
	private void init() {
		this.dataMapped = false;
		this.absData = null;
		this.lgaData = null;
		this.poaData = null;
		this.countData = null;
		this.mapLgaToGccsa = null;
		this.mapPoaToLga = null;
		this.mapLgaToPoa = null;
		this.mapLgaNameToCode = null;
		this.mapLgaCodeToName = null;
		this.lgaMeshblocks = null;
		this.poaMeshblocks = null;
	}
}
