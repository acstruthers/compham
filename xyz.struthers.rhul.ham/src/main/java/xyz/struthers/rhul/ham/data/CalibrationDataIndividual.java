/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.rhul.ham.config.PropertiesXml;
import xyz.struthers.rhul.ham.config.PropertiesXmlFactory;

/**
 * Loads CSV data downloaded using Table Builder from the ABS, RBA, APRA and
 * ATO. This class contains the data that is only needed when calibrating
 * individuals.
 * 
 * @author Adam Struthers
 * @since 2019-02-24
 */
@Component
@Scope(value = "singleton")
public class CalibrationDataIndividual {

	private static final boolean DEBUG = true;

	// map implementation optimisation
	public static final float MAP_LOAD_FACTOR = 0.75f;

	public static final int MAP_INIT_SIZE_LGA = (int) Math.ceil(573 / MAP_LOAD_FACTOR); // 572 (UR) including state
																						// totals (563 Enum)
	public static final int MAP_INIT_SIZE_SEXP = (int) Math.ceil(2 / MAP_LOAD_FACTOR); // 2 (no totals in data)
	public static final int MAP_INIT_SIZE_POA = (int) Math.ceil(2600 / MAP_LOAD_FACTOR); // 2567 plus room for totals
	public static final int MAP_INIT_SIZE_AGE5P = (int) Math.ceil(22 / MAP_LOAD_FACTOR); // 21 plus totals
	public static final int MAP_INIT_SIZE_INDP = (int) Math.ceil(23 / MAP_LOAD_FACTOR); // 22 plus totals
	public static final int MAP_INIT_SIZE_INCP = (int) Math.ceil(18 / MAP_LOAD_FACTOR); // 17 plus totals
	// public static final int MAP_INIT_SIZE_CDCF = (int) Math.ceil(15 /
	// MAP_LOAD_FACTOR); // 15 (no totals in data)

	// series names
	public static final String ATO_INDIVIDUAL_T2A = "ATO_IndividualTable2A";
	public static final String ATO_INDIVIDUAL_T3A = "ATO_IndividualTable3A";
	public static final String ATO_INDIVIDUAL_T6B = "ATO_IndividualTable6B";
	public static final String ATO_INDIVIDUAL_T9 = "ATO_IndividualTable9";

	public static final String CENSUS_SEXP_POA_AGE5P_INDP_INCP = "Census SEXP by POA (UR) by AGE5P, INDP and INCP";

	// properties
	PropertiesXml properties;

	// beans
	private AreaMapping area;
	private CalibrationData sharedData;

	// data
	private boolean dataLoaded;
	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC; // ANZSIC industry code mapping

	/**
	 * ATO Individuals Table 2A<br>
	 * Contains P&L and people count by sex, 5-year age range, and state.<br>
	 * Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
	 */
	private Map<String, Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>> atoIndividualTable2a;
	/**
	 * ATO Individuals Table 3A<br>
	 * Contains P&L and people count by sex, 5-year age range, and income range.<br>
	 * Keys: Series Title, Income Range, Age, Gender, Taxable Status
	 */
	private Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> atoIndividualTable3a;
	/**
	 * ATO Individuals Table 6B<br>
	 * Contains P&L and people count by post code.<br>
	 * Keys: Series Title, Post Code
	 */
	private Map<String, TObjectFloatHashMap<String>> atoIndividualTable6b;
	/**
	 * ATO Individuals Table 9 (Industry Division summary)<br>
	 * Contains count and taxable income, summarised by industry division.<br>
	 * Keys: Series Title, Industry Division Code
	 */
	private Map<String, Map<String, Float>> atoIndividualTable9DivisionSummary;
	/**
	 * ABS Census Table Builder data:<br>
	 * SEXP by POA (UR) by AGE5P, INDP and INCP<br>
	 * Individual income by industry and demographic.
	 * 
	 * Keys: Age5, Industry Division, Personal Income, POA, Sex<br>
	 * Values: Number of persons
	 */
	// private Map<String, Map<String, Map<String, Map<String, Map<String,
	// Float>>>>> censusSEXP_POA_AGE5P_INDP_INCP;
	private Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> censusSEXP_POA_AGE5P_INDP_INCP;
	private boolean initialisedCensusSEXP_POA_AGE5P_INDP_INCP;

	/**
	 * 
	 */
	public CalibrationDataIndividual() {
		super();
		this.init();
	}

	public int getAdjustedPeopleByLga(String lgaCode) {
		return this.getAdjustedPeopleByLga(lgaCode);
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Performs a deep delete of the objects that won't be needed after Individuals
	 * are calibrated.
	 */
	@PreDestroy
	public void close() {
		this.area = null;
		this.sharedData = null;
		this.abs1292_0_55_002ANZSIC = null;

		if (this.title != null) {
			for (String key : this.title.keySet()) {
				this.title.get(key).clear();
				this.title.put(key, null);
			}
			this.title = null;
		}
		if (this.unitType != null) {
			for (String key : this.unitType.keySet()) {
				this.unitType.get(key).clear();
				this.unitType.put(key, null);
			}
			this.unitType = null;
		}
		// ATO 2A Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment
		// Method
		if (this.atoIndividualTable2a != null) {
			for (String series : this.atoIndividualTable2a.keySet()) {
				for (String state : this.atoIndividualTable2a.get(series).keySet()) {
					for (String age : this.atoIndividualTable2a.get(series).get(state).keySet()) {
						for (String sex : this.atoIndividualTable2a.get(series).get(state).get(age).keySet()) {
							for (String taxableStatus : this.atoIndividualTable2a.get(series).get(state).get(age)
									.get(sex).keySet()) {
								for (String lodgment : this.atoIndividualTable2a.get(series).get(state).get(age)
										.get(sex).get(taxableStatus).keySet()) {
									this.atoIndividualTable2a.get(series).get(state).get(age).get(sex)
											.get(taxableStatus).put(lodgment, 0f); // null
								} // end lodgment loop
								this.atoIndividualTable2a.get(series).get(state).get(age).get(sex).get(taxableStatus)
										.clear();
								this.atoIndividualTable2a.get(series).get(state).get(age).get(sex).put(taxableStatus,
										null);
							} // end taxable status loop
							this.atoIndividualTable2a.get(series).get(state).get(age).get(sex).clear();
							this.atoIndividualTable2a.get(series).get(state).get(age).put(sex, null);
						} // end sex loop
						this.atoIndividualTable2a.get(series).get(state).get(age).clear();
						this.atoIndividualTable2a.get(series).get(state).put(age, null);
					} // end age loop
					this.atoIndividualTable2a.get(series).get(state).clear();
					this.atoIndividualTable2a.get(series).put(state, null);
				} // end state loop
				this.atoIndividualTable2a.get(series).clear();
				this.atoIndividualTable2a.put(series, null);
			} // end series loop
			this.atoIndividualTable2a.clear();
			this.atoIndividualTable2a = null;
		}

		// ATO 3A Keys: Series Title, Income Range, Age, Gender, Taxable Status
		if (this.atoIndividualTable3a != null) {
			for (String series : this.atoIndividualTable3a.keySet()) {
				for (String income : this.atoIndividualTable3a.get(series).keySet()) {
					for (String age : this.atoIndividualTable3a.get(series).get(income).keySet()) {
						for (String sex : this.atoIndividualTable3a.get(series).get(income).get(age).keySet()) {
							for (String taxableStatus : this.atoIndividualTable3a.get(series).get(income).get(age)
									.get(sex).keySet()) {
								this.atoIndividualTable3a.get(series).get(income).get(age).get(sex).put(taxableStatus,
										0f); // null
							} // end taxable status loop
							this.atoIndividualTable3a.get(series).get(income).get(age).get(sex).clear();
							this.atoIndividualTable3a.get(series).get(income).get(age).put(sex, null);
						} // end sex loop
						this.atoIndividualTable3a.get(series).get(income).get(age).clear();
						this.atoIndividualTable3a.get(series).get(income).put(age, null);
					} // end age loop
					this.atoIndividualTable3a.get(series).get(income).clear();
					this.atoIndividualTable3a.get(series).put(income, null);
				} // end state loop
				this.atoIndividualTable3a.get(series).clear();
				this.atoIndividualTable3a.put(series, null);
			} // end series loop
			this.atoIndividualTable3a.clear();
			this.atoIndividualTable3a = null;
		}

		// ATO 6B Keys: Series Title, Post Code
		if (this.atoIndividualTable6b != null) {
			for (String series : this.atoIndividualTable6b.keySet()) {
				for (String poa : this.atoIndividualTable6b.get(series).keySet()) {
					this.atoIndividualTable6b.get(series).put(poa, 0f); // null
				} // end poa loop
				this.atoIndividualTable6b.get(series).clear();
				this.atoIndividualTable6b.put(series, null);
			}
			this.atoIndividualTable6b.clear();
			this.atoIndividualTable6b = null;
		}

		// ATO 9 Keys: Series Title, Industry Division Code
		if (this.atoIndividualTable9DivisionSummary != null) {
			for (String series : this.atoIndividualTable9DivisionSummary.keySet()) {
				for (String div : this.atoIndividualTable9DivisionSummary.get(series).keySet()) {
					this.atoIndividualTable9DivisionSummary.get(series).put(div, 0f); /// null
				} // end poa loop
				this.atoIndividualTable9DivisionSummary.get(series).clear();
				this.atoIndividualTable9DivisionSummary.put(series, null);
			}
			this.atoIndividualTable9DivisionSummary.clear();
			this.atoIndividualTable9DivisionSummary = null;
		}

		// Census SEXP Keys: Age5, Industry Division, Personal Income, POA, Sex
		if (this.censusSEXP_POA_AGE5P_INDP_INCP != null) {
			for (String age : this.censusSEXP_POA_AGE5P_INDP_INCP.keySet()) {
				for (String div : this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).keySet()) {
					for (String income : this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).keySet()) {
						for (String poa : this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).keySet()) {
							for (String sex : this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).get(poa)
									.keySet()) {
								this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).get(poa).put(sex, 0f); // null
							} // end taxable status loop
							this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).get(poa).clear();
							this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).put(poa, null);
						} // end sex loop
						this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).get(income).clear();
						this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).put(income, null);
					} // end age loop
					this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).get(div).clear();
					this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).put(div, null);
				} // end state loop
				this.censusSEXP_POA_AGE5P_INDP_INCP.get(age).clear();
				this.censusSEXP_POA_AGE5P_INDP_INCP.put(age, null);
			} // end series loop
			this.censusSEXP_POA_AGE5P_INDP_INCP.clear();
			this.censusSEXP_POA_AGE5P_INDP_INCP = null;
		}
		this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP = false;

		this.init(); // set all the pointers to null
	}

	private void loadData() {
		this.title = new HashMap<String, List<String>>();
		this.unitType = new HashMap<String, List<String>>();
		this.abs1292_0_55_002ANZSIC = this.sharedData.getAbs1292_0_55_002ANZSIC();

		long memoryBefore = 0L; // for debugging memory consumption

		if (DEBUG) {
			System.gc();
			memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		}

		// ABS Census SEXP by POA (UR) by AGE5P, INDP and INCP
		System.out.print(new Date(System.currentTimeMillis())
				+ ": Loading ABS Census SEXP by POA (UR) by AGE5P, INDP and INCP data");
		this.censusSEXP_POA_AGE5P_INDP_INCP = new HashMap<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>(
				MAP_INIT_SIZE_AGE5P);
		int fromColumnSEXP_POA_AGE5P_INDP_INCP = 1;
		int toColumnSEXP_POA_AGE5P_INDP_INCP = 8229;
		System.out.print(": NSW");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - NSW.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP = true;
		System.out.print(", VIC");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - VIC.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", QLD");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - QLD.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", SA");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - SA.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", WA");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - WA.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", TAS");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - TAS.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", NT");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - NT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.print(", ACT");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - ACT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");
		System.out.println(", OT.");
		this.loadAbsCensusTableCsv3Columns1Wafer(
				properties.getFilename("ABS/CensusTableBuilder")
						+ "SEXP by POA (UR) by AGE5P, INDP and INCP/SEXP by POA (UR) by AGE5P, INDP and INCP - OT.csv",
				this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP, fromColumnSEXP_POA_AGE5P_INDP_INCP,
				toColumnSEXP_POA_AGE5P_INDP_INCP, this.censusSEXP_POA_AGE5P_INDP_INCP, "POA");

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(">>> Memory used by ABS Census SEXP by POA (UR) by AGE5P, INDP and INCP: "
					+ formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// Load ATO Individuals Table data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 2A data");
		// int[] atoIndividualTable2aColumns = { 5, 6, 7, 18, 19, 20, 21, 24, 25, 30,
		// 31, 32, 33, 34, 35, 36, 37, 38, 39,
		// 40, 41, 42, 43, 46, 47, 62, 63, 66, 67, 96, 97, 98, 99, 102, 103, 104, 105,
		// 140, 141, 142, 143, 144,
		// 145 };
		int[] atoIndividualTable2aColumns = { 5, 6, 7 };
		int ato2aMapCapacity = (int) Math.ceil(atoIndividualTable2aColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable2a = new HashMap<String, Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>>(
				ato2aMapCapacity);
		this.loadAtoIndividualsTable2a(properties.getFilename("ATO") + "Individual/IndividualsTable2A.csv",
				ATO_INDIVIDUAL_T2A, atoIndividualTable2aColumns, this.title, this.atoIndividualTable2a);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used by ATO Individuals Table 2A: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 3A data");

		int[] atoIndividualTable3aColumns = { 5, 6, 7, 20, 21, 24, 25, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41,
				42, 43, 46, 47, 58, 59, 62, 63, 66, 67, 96, 97, 98, 99, 102, 103, 104, 105, 140, 141, 142, 143, 144,
				145 };
		int ato3aMapCapacity = (int) Math.ceil(atoIndividualTable3aColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable3a = new HashMap<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>(
				ato3aMapCapacity);
		this.loadAtoIndividualsTable3a(properties.getFilename("ATO") + "Individual/IndividualsTable3A.csv",
				ATO_INDIVIDUAL_T3A, atoIndividualTable3aColumns, this.title, this.atoIndividualTable3a);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used by ATO Individuals Table 3A: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 6B data");
		// int[] atoIndividualTable6bColumns = { 2, 3, 4, 15, 16, 17, 18, 21, 22, 28,
		// 29, 30, 31, 32, 33, 34, 35, 36, 37,
		// 38, 39, 40, 41, 44, 45, 59, 60, 63, 64, 93, 94, 95, 96, 99, 100, 101, 102 };
		int[] atoIndividualTable6bColumns = { 2, 3, 4 };
		int ato6bMapCapacity = (int) Math.ceil(atoIndividualTable6bColumns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable6b = new HashMap<String, TObjectFloatHashMap<String>>(ato6bMapCapacity);
		this.loadAtoIndividualsTable6(properties.getFilename("ATO") + "Individual/IndividualsTable6B.csv",
				ATO_INDIVIDUAL_T6B, atoIndividualTable6bColumns, this.title, this.atoIndividualTable6b);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used by ATO Individuals Table 6B: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		/*
		 * System.out.println(new Date(System.currentTimeMillis()) +
		 * ": Loading ATO Individuals Table 6C data"); this.atoIndividualTable6c = new
		 * HashMap<String, Map<String, String>>(); int[] atoIndividualTable6cColumns = {
		 * 10, 11, 12, 13, 14, 32, 33, 34, 35, 36 }; this.loadAtoIndividualsTable6(
		 * "/data/ATO/Individual/IndividualsTable6C_transformed.csv",
		 * ATO_INDIVIDUAL_T6C, atoIndividualTable6cColumns, this.title,
		 * this.atoIndividualTable6c);
		 */

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Individuals Table 9 data");
		// int[] atoIndividualTable9Columns = { 2, 3, 4, 15, 16, 17, 18, 21, 22, 27, 28,
		// 29, 30, 31, 32, 33, 34, 35, 36,
		// 37, 38, 39, 40, 43, 44, 93, 94, 95, 96, 99, 100, 101, 102 };
		int[] atoIndividualTable9Columns = { 2, 3, 4 };
		int ato9MapCapacity = (int) Math.ceil(atoIndividualTable9Columns.length / MAP_LOAD_FACTOR);
		this.atoIndividualTable9DivisionSummary = new HashMap<String, Map<String, Float>>(ato9MapCapacity);
		this.loadAtoIndividualsTable9DivisionSummary(properties.getFilename("ATO") + "Individual/IndividualsTable9.csv",
				ATO_INDIVIDUAL_T9, atoIndividualTable9Columns, this.title, this.atoIndividualTable9DivisionSummary);
		// this.atoIndividualTable9 = new HashMap<String, Map<String,
		// String>>(ato9MapCapacity);
		// this.loadAtoIndividualsTable9("/data/ATO/Individual/IndividualsTable9.csv",
		// ATO_INDIVIDUAL_T9,
		// atoIndividualTable9Columns, this.title, this.atoIndividualTable9);

		if (DEBUG) {
			System.gc();
			long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			System.out.println(
					">>> Memory used by ATO Individuals Table 9: " + formatter.format(megabytesConsumed) + "MB");
			memoryBefore = memoryAfter;
		}

		// set flag so we only load the data once
		System.out.println(new Date(System.currentTimeMillis()) + ": Individual data loaded");
		this.dataLoaded = true;
	}

	/**
	 * Loads ABS Census Table Builder tables with two dimensions (row & column).
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the column titles.<br>
	 * 2. Row 4 column 1 contains the series title.<br>
	 * 3. Data starts on row 12.<br>
	 * 4. The first column contains the LGA names (not codes).
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	/*
	 * private void loadAbsCensusTableCsv2D(String fileResourceLocation, String
	 * tableName, int[] columnsToImport, boolean isInitialised, Map<String,
	 * List<String>> titles, Map<String, Map<String, String>> data) {
	 * 
	 * CSVReader reader = null; try { InputStream is =
	 * this.getClass().getResourceAsStream(fileResourceLocation); reader = new
	 * CSVReader(new InputStreamReader(is)); boolean header = true; boolean footer =
	 * false; int currentRow = 1; int titleRow = 10; int lastHeaderRow = 11;
	 * String[] seriesId = new String[columnsToImport.length];
	 * 
	 * String[] line = null; while ((line = reader.readNext()) != null) { if
	 * (header) { if (currentRow == titleRow) { for (int i = 0; i <
	 * columnsToImport.length; i++) { seriesId[i] = line[columnsToImport[i]]; } if
	 * (!isInitialised) { titles.put(tableName, new
	 * ArrayList<String>(columnsToImport.length)); for (int i = 0; i <
	 * columnsToImport.length; i++) { // store title
	 * titles.get(tableName).add(line[columnsToImport[i]]);
	 * 
	 * // store series ID as key with blank collections to populate with data below
	 * data.put(line[columnsToImport[i]], new HashMap<String, String>()); } } } else
	 * if (currentRow == lastHeaderRow) { header = false; } } else if (!footer) { if
	 * (line.length > 0 && line[0].equals("Total")) { footer = true; } else { for
	 * (int i = 0; i < columnsToImport.length; i++) { // parse the body of the data
	 * String lgaCode = this.area.getLgaCodeFromName(line[0]);
	 * data.get(seriesId[i]).put(lgaCode, line[columnsToImport[i]]); // WISHLIST:
	 * modify this so it can use POA, state, etc. not just LGA } } } }
	 * reader.close(); reader = null; } catch (FileNotFoundException e) { // open
	 * file e.printStackTrace(); } catch (IOException e) { // read next
	 * e.printStackTrace(); } }
	 */

	/**
	 * Loads ABS Census Table Builder tables with three dimensions (row, column &
	 * wafer).
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the first wafer title.<br>
	 * 2. The row after each wafer title is the column titles.<br>
	 * 3. Row 4 column 1 contains the series title.<br>
	 * 4. Data starts on row 12.<br>
	 * 5. The first column contains the LGA names (not codes).<br>
	 * 6. The first row in the footer begins with "Data Source".
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: column, row, wafer. (div, LGA, income)
	 */
	/*
	 * private void loadAbsCensusTableCsv1Column1Wafer(String fileResourceLocation,
	 * boolean isInitialised, int fromColumnIndex, int toColumnIndex, Map<String,
	 * Map<String, Map<String, String>>> data) {
	 * 
	 * CSVReader reader = null; try { InputStream is =
	 * this.getClass().getResourceAsStream(fileResourceLocation); reader = new
	 * CSVReader(new InputStreamReader(is)); boolean header = true; boolean footer =
	 * false; int currentRow = 1; int lastHeaderRow = 9; // the row before the first
	 * wafer's title row boolean prevRowIsBlank = true; // there's a blank row
	 * before wafer names boolean prevRowIsWaferName = false; String waferName =
	 * null; int waferNumber = 0; String[] seriesId = new String[toColumnIndex -
	 * fromColumnIndex];
	 * 
	 * String[] line = null; while ((line = reader.readNext()) != null) { if
	 * (header) { if (currentRow++ == lastHeaderRow) { header = false; } } else if
	 * (!footer) { if (line[0].length() > 11 && line[0].substring(0,
	 * 11).equals("Data Source")) { footer = true; } else { if (prevRowIsBlank &&
	 * !line[0].isBlank()) { // set wafer name waferName = line[0].trim();
	 * prevRowIsWaferName = true; waferNumber++; prevRowIsBlank = false; } else { if
	 * (prevRowIsWaferName) { if (waferNumber == 1) { // set series ID for (int i =
	 * 0; i < toColumnIndex - fromColumnIndex; i++) { seriesId[i] = line[i +
	 * fromColumnIndex]; } if (!isInitialised) { for (int i = 0; i < toColumnIndex -
	 * fromColumnIndex; i++) { // store series ID as key with blank collections to
	 * populate with data below data.put(line[i + fromColumnIndex], new
	 * HashMap<String, Map<String, String>>(MAP_INIT_SIZE_LGA)); } } }
	 * prevRowIsWaferName = false; } else if (line.length > 1 && !line[1].isBlank())
	 * { // parse the body of the data // WISHLIST: modify this so it can use POA,
	 * state, etc. not just LGA String lgaCode =
	 * this.area.getLgaCodeFromName(line[0]); if (lgaCode != null) { // null check
	 * excludes invalid LGAs for (int i = 0; i < toColumnIndex - fromColumnIndex;
	 * i++) { if (waferNumber == 1) { data.get(seriesId[i]).put(lgaCode, new
	 * HashMap<String, String>(MAP_INIT_SIZE_CDCF)); }
	 * data.get(seriesId[i]).get(lgaCode).put(waferName, line[i + fromColumnIndex]);
	 * } } } else if (line[0].isBlank()) { prevRowIsBlank = true; } } } } }
	 * reader.close(); reader = null; } catch (FileNotFoundException e) { // open
	 * file e.printStackTrace(); } catch (IOException e) { // read next
	 * e.printStackTrace(); } }
	 */

	/**
	 * Loads ABS Census Table Builder tables with one row series, three column
	 * series, and one wafer series.
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the first wafer title.<br>
	 * 2. The rows after each wafer title are the column titles.<br>
	 * 3. Row 4 column 1 contains the series title.<br>
	 * 4. Data starts on row 15.<br>
	 * 5. The first column contains the LGA names (not codes).<br>
	 * 6. The first row in the footer begins with "Data Source".
	 * 
	 * @param fileResourceLocation - the URI of the file to import.
	 * @param isInitialised        - true if any file has already been imported for
	 *                             this data series, false otherwise.
	 * @param fromColumnIndex      - import data from this column index (inclusive).
	 * @param toColumnIndex        - import data to this column index (exclusive).
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: 3 columns, row, wafer. (AGE5P, INDP,
	 *                             INCP, LGA, SEXP)
	 */
	private void loadAbsCensusTableCsv3Columns1Wafer(String fileResourceLocation, boolean isInitialised,
			int fromColumnIndex, int toColumnIndex,
			Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> data, String lgaOrPoa) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int lastHeaderRow = 9; // the row before the first wafer's title row
			boolean prevRowIsBlank = true; // there's a blank row before wafer names
			String waferName = null;
			int waferNumber = 0;
			int columnSeriesNumber = Integer.MAX_VALUE;
			final int columnSeriesMax = 3; // because the dataset contains 3 column series
			String[][] columnTitles = new String[columnSeriesMax][toColumnIndex - fromColumnIndex];

			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow++ == lastHeaderRow) {
						header = false;
					}
				} else if (!footer) {
					if (line[0].length() > 11 && line[0].substring(0, 11).equals("Data Source")) {
						footer = true;
					} else {
						if (prevRowIsBlank && !line[0].isBlank()) {
							// set wafer name
							waferName = line[0].trim();
							waferName = waferName.substring(0, 1); // wafer is sex, so just take the first letter (M, F)
							columnSeriesNumber = 0;
							waferNumber++;
							prevRowIsBlank = false;
						} else {
							if (columnSeriesNumber < columnSeriesMax) {
								// set series ID
								String thisTitle = null;
								for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
									thisTitle = line[i + fromColumnIndex].isEmpty() ? thisTitle
											: line[i + fromColumnIndex];
									columnTitles[columnSeriesNumber][i] = thisTitle;
								}
								columnSeriesNumber++;
							} else if (columnSeriesNumber == columnSeriesMax && !isInitialised) {
								// add blank maps to data, so they can be populated below
								if (waferNumber == 1) {
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										if (!columnTitles[0][i].isBlank() && !data.containsKey(columnTitles[0][i])) {
											// add column series 1 key
											data.put(columnTitles[0][i],
													new HashMap<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>(
															MAP_INIT_SIZE_INDP));
										}
										String divCode = this.abs1292_0_55_002ANZSIC.get("Division to Division Code")
												.get(columnTitles[1][i].toUpperCase());
										if (!columnTitles[1][i].isBlank()
												&& !data.get(columnTitles[0][i]).containsKey(divCode)) {
											// add column series 2 key
											// convert Industry Division Description to Code
											data.get(columnTitles[0][i]).put(divCode,
													new HashMap<String, Map<String, TObjectFloatHashMap<String>>>(
															MAP_INIT_SIZE_INCP));
										}
										if (!columnTitles[2][i].isBlank() && !data.get(columnTitles[0][i]).get(divCode)
												.containsKey(columnTitles[2][i])) {
											// add column series 3 key
											data.get(columnTitles[0][i]).get(divCode).put(columnTitles[2][i],
													new HashMap<String, TObjectFloatHashMap<String>>(
															MAP_INIT_SIZE_LGA));
										}
									}
								}
								columnSeriesNumber++; // make sure this is only executed once
							} else if (line.length > 1 && !line[1].isBlank()) {
								// parse the body of the data
								String areaCode = null;
								if (lgaOrPoa.equalsIgnoreCase("LGA")) {
									areaCode = this.area.getLgaCodeFromName(line[0]);
								} else {
									// assume it's POA in the format NNNN, SSS
									if (!line[0].equalsIgnoreCase("Total")) {
										areaCode = line[0].substring(0, line[0].indexOf(","));
									}
								}
								if (areaCode != null) {
									// null check excludes invalid LGAs
									for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
										String divCode = this.abs1292_0_55_002ANZSIC.get("Division to Division Code")
												.get(columnTitles[1][i].toUpperCase());
										if (waferNumber == 1) {
											data.get(columnTitles[0][i]).get(divCode).get(columnTitles[2][i])
													.put(areaCode, new TObjectFloatHashMap<String>(MAP_INIT_SIZE_SEXP));
										}
										float value = 0f;
										try {
											value = Float.valueOf(line[i + fromColumnIndex].trim().replace(",", ""));
										} catch (NumberFormatException e) {
											// do nothing and leave it as zero.
										}
										data.get(columnTitles[0][i]).get(divCode).get(columnTitles[2][i]).get(areaCode)
												.put(waferName, value);
									}
								}
							} else if (line[0].isBlank()) {
								prevRowIsBlank = true;
							}
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

	/*
	 * private void loadAbsCensusTableCsv3Columns1WaferJDK(String
	 * fileResourceLocation, boolean isInitialised, int fromColumnIndex, int
	 * toColumnIndex, Map<String, Map<String, Map<String, Map<String, Map<String,
	 * Float>>>>> data, String lgaOrPoa) {
	 * 
	 * CSVReader reader = null; try { InputStream is =
	 * this.getClass().getResourceAsStream(fileResourceLocation); reader = new
	 * CSVReader(new InputStreamReader(is)); boolean header = true; boolean footer =
	 * false; int currentRow = 1; int lastHeaderRow = 9; // the row before the first
	 * wafer's title row boolean prevRowIsBlank = true; // there's a blank row
	 * before wafer names String waferName = null; int waferNumber = 0; int
	 * columnSeriesNumber = Integer.MAX_VALUE; final int columnSeriesMax = 3; //
	 * because the dataset contains 3 column series String[][] columnTitles = new
	 * String[columnSeriesMax][toColumnIndex - fromColumnIndex];
	 * 
	 * String[] line = null; while ((line = reader.readNext()) != null) { if
	 * (header) { if (currentRow++ == lastHeaderRow) { header = false; } } else if
	 * (!footer) { if (line[0].length() > 11 && line[0].substring(0,
	 * 11).equals("Data Source")) { footer = true; } else { if (prevRowIsBlank &&
	 * !line[0].isBlank()) { // set wafer name waferName = line[0].trim(); waferName
	 * = waferName.substring(0, 1); // wafer is sex, so just take the first letter
	 * (M, F) columnSeriesNumber = 0; waferNumber++; prevRowIsBlank = false; } else
	 * { if (columnSeriesNumber < columnSeriesMax) { // set series ID String
	 * thisTitle = null; for (int i = 0; i < toColumnIndex - fromColumnIndex; i++) {
	 * thisTitle = line[i + fromColumnIndex].isEmpty() ? thisTitle : line[i +
	 * fromColumnIndex]; columnTitles[columnSeriesNumber][i] = thisTitle; }
	 * columnSeriesNumber++; } else if (columnSeriesNumber == columnSeriesMax &&
	 * !isInitialised) { // add blank maps to data, so they can be populated below
	 * if (waferNumber == 1) { for (int i = 0; i < toColumnIndex - fromColumnIndex;
	 * i++) { if (!columnTitles[0][i].isBlank() &&
	 * !data.containsKey(columnTitles[0][i])) { // add column series 1 key
	 * data.put(columnTitles[0][i], new HashMap<String, Map<String, Map<String,
	 * Map<String, Float>>>>( MAP_INIT_SIZE_INDP)); } String divCode =
	 * this.abs1292_0_55_002ANZSIC.get("Division to Division Code")
	 * .get(columnTitles[1][i].toUpperCase()); if (!columnTitles[1][i].isBlank() &&
	 * !data.get(columnTitles[0][i]).containsKey(divCode)) { // add column series 2
	 * key // convert Industry Division Description to Code
	 * data.get(columnTitles[0][i]).put(divCode, new HashMap<String, Map<String,
	 * Map<String, Float>>>( MAP_INIT_SIZE_INCP)); } if
	 * (!columnTitles[2][i].isBlank() && !data.get(columnTitles[0][i]).get(divCode)
	 * .containsKey(columnTitles[2][i])) { // add column series 3 key
	 * data.get(columnTitles[0][i]).get(divCode).put(columnTitles[2][i], new
	 * HashMap<String, Map<String, Float>>(MAP_INIT_SIZE_LGA)); } } }
	 * columnSeriesNumber++; // make sure this is only executed once } else if
	 * (line.length > 1 && !line[1].isBlank()) { // parse the body of the data
	 * String areaCode = null; if (lgaOrPoa.equalsIgnoreCase("LGA")) { areaCode =
	 * this.area.getLgaCodeFromName(line[0]); } else { // assume it's POA in the
	 * format NNNN, SSS if (!line[0].equalsIgnoreCase("Total")) { areaCode =
	 * line[0].substring(0, line[0].indexOf(",")); } } if (areaCode != null) { //
	 * null check excludes invalid LGAs for (int i = 0; i < toColumnIndex -
	 * fromColumnIndex; i++) { String divCode =
	 * this.abs1292_0_55_002ANZSIC.get("Division to Division Code")
	 * .get(columnTitles[1][i].toUpperCase()); if (waferNumber == 1) {
	 * data.get(columnTitles[0][i]).get(divCode).get(columnTitles[2][i])
	 * .put(areaCode, new HashMap<String, Float>(MAP_INIT_SIZE_SEXP)); } float value
	 * = 0f; try { value = Float.valueOf(line[i +
	 * fromColumnIndex].trim().replace(",", "")); } catch (NumberFormatException e)
	 * { // do nothing and leave it as zero. }
	 * data.get(columnTitles[0][i]).get(divCode).get(columnTitles[2][i]).get(
	 * areaCode) .put(waferName, value); } } } else if (line[0].isBlank()) {
	 * prevRowIsBlank = true; } } } } } reader.close(); reader = null; } catch
	 * (FileNotFoundException e) { // open file e.printStackTrace(); } catch
	 * (IOException e) { // read next e.printStackTrace(); } }
	 */

	/**
	 * Loads ABS Census Table Builder tables with one row series, two column series,
	 * and two wafer series.
	 * 
	 * File pre-conditions:<br>
	 * 1. Row 10 contains the first wafer title.<br>
	 * 2. The rows after each wafer title are the column titles.<br>
	 * 3. Row 4 column 1 contains the series title.<br>
	 * 4. Data starts on row 15.<br>
	 * 5. The first column contains the LGA names (not codes).<br>
	 * 6. The first row in the footer begins with "Data Source".
	 * 
	 * @param fileResourceLocation - the URI of the file to import.
	 * @param isInitialised        - true if any file has already been imported for
	 *                             this data series, false otherwise.
	 * @param fromColumnIndex      - import data from this column index (inclusive).
	 * @param toColumnIndex        - import data to this column index (exclusive).
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: 2 columns, row, 2 wafers. (HIND,
	 *                             RNTRD/MRERD, LGA, HCFMD, TEND)
	 */
	/*
	 * private void loadAbsCensusTableCsv2Columns2Wafers(String
	 * fileResourceLocation, boolean isInitialised, int fromColumnIndex, int
	 * toColumnIndex, Map<String, Map<String, Map<String, Map<String, Map<String,
	 * String>>>>> data) {
	 * 
	 * CSVReader reader = null; try { InputStream is =
	 * this.getClass().getResourceAsStream(fileResourceLocation); reader = new
	 * CSVReader(new InputStreamReader(is)); boolean header = true; boolean footer =
	 * false; int currentRow = 1; int lastHeaderRow = 9; // the row before the first
	 * wafer's title row boolean prevRowIsBlank = true; // there's a blank row
	 * before wafer names final int numWafers = 2; String[] waferName = new
	 * String[numWafers]; int waferNumber = 0; int columnSeriesNumber =
	 * Integer.MAX_VALUE; final int columnSeriesMax = 2; // because the dataset
	 * contains 2 column series String[][] columnTitles = new
	 * String[columnSeriesMax][toColumnIndex - fromColumnIndex];
	 * 
	 * String[] line = null; while ((line = reader.readNext()) != null) { if
	 * (header) { if (currentRow++ == lastHeaderRow) { header = false; } } else if
	 * (!footer) { if (line[0].length() > 11 && line[0].substring(0,
	 * 11).equals("Data Source")) { footer = true; } else { if (prevRowIsBlank &&
	 * !line[0].isBlank()) { // set wafer name String[] tmp = line[0].split(","); //
	 * wafer names are separated by commas for (int i = 0; i < numWafers; i++) {
	 * waferName[i] = tmp[i].trim(); } columnSeriesNumber = 0; waferNumber++;
	 * prevRowIsBlank = false; } else { if (columnSeriesNumber < columnSeriesMax) {
	 * // set series ID String thisTitle = null; for (int i = 0; i < toColumnIndex -
	 * fromColumnIndex; i++) { thisTitle = line[i + fromColumnIndex].isEmpty() ?
	 * thisTitle : line[i + fromColumnIndex]; columnTitles[columnSeriesNumber][i] =
	 * thisTitle; } columnSeriesNumber++; } else if (columnSeriesNumber ==
	 * columnSeriesMax && !isInitialised) { // add blank maps to data, so they can
	 * be populated below if (waferNumber == 1) { for (int i = 0; i < toColumnIndex
	 * - fromColumnIndex; i++) { if (!columnTitles[0][i].isBlank() &&
	 * !data.containsKey(columnTitles[0][i])) { // add column series 1 key
	 * data.put(columnTitles[0][i], new HashMap<String, Map<String, Map<String,
	 * Map<String, String>>>>( Math.max(MAP_INIT_SIZE_RNTRD, MAP_INIT_SIZE_MRERD)));
	 * } if (!columnTitles[1][i].isBlank() &&
	 * !data.get(columnTitles[0][i]).containsKey(columnTitles[1][i])) { // add
	 * column series 2 key data.get(columnTitles[0][i]).put(columnTitles[1][i], new
	 * HashMap<String, Map<String, Map<String, String>>>( MAP_INIT_SIZE_LGA)); } } }
	 * } else if (line.length > 1 && !line[1].isBlank()) { // parse the body of the
	 * data // WISHLIST: modify this so it can use POA, state, etc. not just LGA
	 * String lgaCode = this.area.getLgaCodeFromName(line[0]); if (lgaCode != null)
	 * { // null check excludes invalid LGAs for (int i = 0; i < toColumnIndex -
	 * fromColumnIndex; i++) { if (waferNumber == 1) {
	 * data.get(columnTitles[0][i]).get(columnTitles[1][i]).put(lgaCode, new
	 * HashMap<String, Map<String, String>>(MAP_INIT_SIZE_HCFMD)); } if
	 * (!data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
	 * .containsKey(waferName[0])) { // add map if this is the first time the wafer
	 * 1 has been read
	 * data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
	 * .put(waferName[0], new HashMap<String, String>(MAP_INIT_SIZE_TEND)); }
	 * data.get(columnTitles[0][i]).get(columnTitles[1][i]).get(lgaCode)
	 * .get(waferName[0]).put(waferName[1], line[i + fromColumnIndex]); } } } else
	 * if (line[0].isBlank()) { prevRowIsBlank = true; } } } } } reader.close();
	 * reader = null; } catch (FileNotFoundException e) { // open file
	 * e.printStackTrace(); } catch (IOException e) { // read next
	 * e.printStackTrace(); } }
	 */

	/**
	 * Load data from ATO Individuals Table 2A
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: Series Title, State, Age, Gender, Taxable
	 *                             Status, Lodgment Method
	 */
	private void loadAtoIndividualsTable2a(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles,
			Map<String, Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Lodgment method")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]].trim();
							thesecolumnNames.add(line[columnsToImport[i]].trim());

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]].trim(),
									new HashMap<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Keys: Series Title, State, Age, Gender, Taxable Status, Lodgment Method
						String thisState = line[3].trim().length() > 3 ? "Other" : line[3].trim();
						String thisAge = line[4].trim();
						String thisSex = line[1].substring(0, 1).toUpperCase();
						String thisTaxableStatus = line[2].substring(0, 1).equals("N") ? "N" : "Y";
						String thisLodgmentMethod = line[0].substring(0, 1);

						for (int i = 0; i < columnsToImport.length; i++) {
							// create nested maps for new data categories
							if (!data.get(seriesId[i]).containsKey(thisState)) {
								data.get(seriesId[i]).put(thisState,
										new HashMap<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>());
							}
							if (!data.get(seriesId[i]).get(thisState).containsKey(thisAge)) {
								data.get(seriesId[i]).get(thisState).put(thisAge,
										new HashMap<String, Map<String, TObjectFloatHashMap<String>>>());
							}
							if (!data.get(seriesId[i]).get(thisState).get(thisAge).containsKey(thisSex)) {
								data.get(seriesId[i]).get(thisState).get(thisAge).put(thisSex,
										new HashMap<String, TObjectFloatHashMap<String>>());
							}
							if (!data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex)
									.containsKey(thisTaxableStatus)) {
								data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex).put(thisTaxableStatus,
										new TObjectFloatHashMap<String>());
							}

							// parse the body of the data
							float value = 0f;
							try {
								value = Float.valueOf(line[columnsToImport[i]].trim().replace(",", ""));
							} catch (NumberFormatException e) {
								// do nothing and leave it as zero.
							}
							data.get(seriesId[i]).get(thisState).get(thisAge).get(thisSex).get(thisTaxableStatus)
									.put(thisLodgmentMethod, value);
						}
					} else {
						footer = true;
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

	/**
	 * Load data from ATO Individuals Table 3A
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: Series Title, Income Range, Age, Gender,
	 *                             Taxable Status
	 */
	private void loadAtoIndividualsTable3a(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles,
			Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Gender")) {
						// title row
						List<String> theseColumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]].trim();
							theseColumnNames.add(line[columnsToImport[i]].trim());

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]].trim(),
									new HashMap<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>());
						}
						titles.put(tableName, theseColumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Keys: Series Title, Income Range, Age, Gender, Taxable Status
						String thisIncomeRange = line[3].trim();
						String thisAge = line[2].trim();
						String thisSex = line[0].trim().substring(0, 1).toUpperCase();
						String thisTaxableStatus = line[1].trim().substring(0, 1).equals("N") ? "N" : "Y";

						for (int i = 0; i < columnsToImport.length; i++) {
							// create nested maps for new data categories
							if (!data.get(seriesId[i]).containsKey(thisIncomeRange)) {
								data.get(seriesId[i]).put(thisIncomeRange,
										new HashMap<String, Map<String, TObjectFloatHashMap<String>>>());
							}
							if (!data.get(seriesId[i]).get(thisIncomeRange).containsKey(thisAge)) {
								data.get(seriesId[i]).get(thisIncomeRange).put(thisAge,
										new HashMap<String, TObjectFloatHashMap<String>>());
							}
							if (!data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).containsKey(thisSex)) {
								data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).put(thisSex,
										new TObjectFloatHashMap<String>());
							}

							// parse the body of the data
							float value = 0f;
							try {
								value = Float.valueOf(line[columnsToImport[i]].trim().replace(",", ""));
							} catch (NumberFormatException e) {
								// do nothing and leave it as zero.
							}
							data.get(seriesId[i]).get(thisIncomeRange).get(thisAge).get(thisSex).put(thisTaxableStatus,
									value);
						}
					} else {
						footer = true;
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

	/**
	 * Load data from ATO Individuals Table 6B, 6C
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAtoIndividualsTable6(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, TObjectFloatHashMap<String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("State/ Territory1")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]].trim();
							thesecolumnNames.add(line[columnsToImport[i]].trim());

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]].trim(), new TObjectFloatHashMap<String>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[0].isBlank()) {
						// Check if line[1] is numeric, and skip this row if it's not because we
						// can't map a "state other" category to an LGA
						if (NumberUtils.isCreatable(line[1])) {
							for (int i = 0; i < columnsToImport.length; i++) {
								// parse the body of the data
								float value = 0f;
								try {
									value = Float.valueOf(line[columnsToImport[i]].trim().replace(",", ""));
								} catch (NumberFormatException e) {
									// do nothing and leave it as zero.
								}
								data.get(seriesId[i]).put(line[1].trim(), value);
							}
						}
					} else {
						footer = true;
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

	/**
	 * Load data from ATO Individuals Table 9
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	/*
	 * private void loadAtoIndividualsTable9(String fileResourceLocation, String
	 * tableName, int[] columnsToImport, Map<String, List<String>> titles,
	 * Map<String, Map<String, String>> data) {
	 * 
	 * CSVReader reader = null; try { InputStream is =
	 * this.getClass().getResourceAsStream(fileResourceLocation); reader = new
	 * CSVReader(new InputStreamReader(is)); boolean header = true; boolean footer =
	 * false; String[] seriesId = new String[columnsToImport.length];
	 * 
	 * String[] line = null; while ((line = reader.readNext()) != null && !footer) {
	 * if (header) { if (line[0].equals("Broad Industry1")) { // title row
	 * List<String> thesecolumnNames = new
	 * ArrayList<String>(columnsToImport.length); for (int i = 0; i <
	 * columnsToImport.length; i++) { // store title seriesId[i] =
	 * line[columnsToImport[i]].trim();
	 * thesecolumnNames.add(line[columnsToImport[i]].trim());
	 * 
	 * // store series ID as key with blank collections to populate with data below
	 * data.put(line[columnsToImport[i]].trim(), new HashMap<String, String>()); }
	 * titles.put(tableName, thesecolumnNames); header = false; } } else { if
	 * (!line[1].equals("Other individuals")) { String fineIndustryCode =
	 * line[1].trim().substring(0, 5); for (int i = 0; i < columnsToImport.length;
	 * i++) { // parse the body of the data
	 * data.get(seriesId[i]).put(fineIndustryCode, line[columnsToImport[i]].trim());
	 * } } else { footer = true; } } } reader.close(); reader = null; } catch
	 * (FileNotFoundException e) { // open file e.printStackTrace(); } catch
	 * (IOException e) { // read next e.printStackTrace(); } }
	 */

	/**
	 * Load data from ATO Individuals Table 9
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param tableName            - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAtoIndividualsTable9DivisionSummary(String fileResourceLocation, String tableName,
			int[] columnsToImport, Map<String, List<String>> titles, Map<String, Map<String, Float>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Broad Industry1")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						int divisionMapCapacity = (int) Math.ceil(19 / MAP_LOAD_FACTOR + 1); // 19 divisions imported
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]].trim();
							thesecolumnNames.add(line[columnsToImport[i]].trim());

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]].trim(), new HashMap<String, Float>(divisionMapCapacity));
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[1].equals("Other individuals")) {
						String divisionCode = line[0].trim().substring(0, 1).toUpperCase();
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							float oldVal = 0f;
							if (data.get(seriesId[i]).get(divisionCode) != null) {
								oldVal = data.get(seriesId[i]).get(divisionCode);
							}
							float value = 0f;
							try {
								value = Float.valueOf(line[columnsToImport[i]].trim().replace(",", ""));
							} catch (NumberFormatException e) {
								// do nothing and leave it as zero.
							}
							data.get(seriesId[i]).put(divisionCode, oldVal + value);
						}
					} else {
						footer = true;
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

	@PostConstruct
	private void init() {
		this.properties = PropertiesXmlFactory.getProperties();

		this.dataLoaded = false;

		this.title = null;
		this.unitType = null;

		this.atoIndividualTable2a = null;
		this.atoIndividualTable3a = null;
		this.atoIndividualTable6b = null;
		this.atoIndividualTable9DivisionSummary = null;

		this.censusSEXP_POA_AGE5P_INDP_INCP = null;
		this.initialisedCensusSEXP_POA_AGE5P_INDP_INCP = false;
	}

	/**
	 * @return the atoIndividualTable2a
	 */
	public Map<String, Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>>> getAtoIndividualTable2a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable2a;
	}

	/**
	 * @return the atoIndividualTable3a
	 */
	public Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> getAtoIndividualTable3a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable3a;
	}

	/**
	 * @return the atoIndividualTable6b
	 */
	public Map<String, TObjectFloatHashMap<String>> getAtoIndividualTable6b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable6b;
	}

	/**
	 * @return the atoIndividualTable9DivisionSummary
	 */
	public Map<String, Map<String, Float>> getAtoIndividualTable9DivisionSummary() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoIndividualTable9DivisionSummary;
	}

	/**
	 * @return the censusSEXP_POA_AGE5P_INDP_INCP
	 */
	public Map<String, Map<String, Map<String, Map<String, TObjectFloatHashMap<String>>>>> getCensusSEXP_POA_AGE5P_INDP_INCP() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusSEXP_POA_AGE5P_INDP_INCP;
	}

	/**
	 * @param area the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

	/**
	 * @param sharedData the sharedData to set
	 */
	@Autowired
	public void setSharedData(CalibrationData sharedData) {
		this.sharedData = sharedData;
	}

}
