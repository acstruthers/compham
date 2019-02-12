/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.opencsv.CSVReader;

import xyz.struthers.rhul.ham.config.Properties;

/**
 * Loads CSV data downloaded using Table Builder from the ABS, RBA, APRA and
 * ATO.
 * 
 * @author Adam Struthers
 * @since 2018-11-20
 */
@Component
@Scope(value = "singleton")
public class CalibrationData {

	// multipliers
	public static final Double BILLION = 1000000000d;
	public static final Double MILLION = 1000000d;
	public static final Double THOUSAND = 1000d;
	public static final Double PERCENT = 0.01d;

	// series names
	public static final String RBA_E1 = "RBA_E1"; // household & business Balance Sheet
	public static final String RBA_E2 = "RBA_E2"; // household Balance Sheet ratios
	public static final String ABS1292_0_55_002_ANZSIC = "ABS_1292.0.55.002_ANZSIC";
	public static final String ABS1410_0_ECONOMY = "ABS_1410.0_Economy";
	public static final String ABS1410_0_FAMILY = "ABS_1410.0_Family";
	public static final String ABS1410_0_INCOME = "ABS_1410.0_Income";
	public static final String ABS_3222_0 = "ABS_3222.0"; // population projections
	public static final String ABS_5368_0_T14A = "ABS_5368.0_Table14a"; // exports by country
	public static final String ABS_5368_0_T14B = "ABS_5368.0_Table14b"; // imports by country
	public static final String ABS_5368_0_T36A = "ABS_5368.0_Table36a"; // exports NSW
	public static final String ABS_5368_0_T36B = "ABS_5368.0_Table36b"; // exports VIC
	public static final String ABS_5368_0_T36C = "ABS_5368.0_Table36c"; // exports QLD
	public static final String ABS_5368_0_T36D = "ABS_5368.0_Table36d"; // exports SA
	public static final String ABS_5368_0_T36E = "ABS_5368.0_Table36e"; // exports WA
	public static final String ABS_5368_0_T36F = "ABS_5368.0_Table36f"; // exports TAS
	public static final String ABS_5368_0_T36G = "ABS_5368.0_Table36g"; // exports NT
	public static final String ABS_5368_0_T36H = "ABS_5368.0_Table36h"; // exports ACT
	public static final String ABS_5368_0_T37A = "ABS_5368.0_Table37a"; // imports NSW
	public static final String ABS_5368_0_T37B = "ABS_5368.0_Table37b"; // imports VIC
	public static final String ABS_5368_0_T37C = "ABS_5368.0_Table37c"; // imports QLD
	public static final String ABS_5368_0_T37D = "ABS_5368.0_Table37d"; // imports SA
	public static final String ABS_5368_0_T37E = "ABS_5368.0_Table37e"; // imports WA
	public static final String ABS_5368_0_T37F = "ABS_5368.0_Table37f"; // imports TAS
	public static final String ABS_5368_0_T37G = "ABS_5368.0_Table37g"; // imports NT
	public static final String ABS_5368_0_T37H = "ABS_5368.0_Table37h"; // imports ACT
	public static final String ABS_5368_0_EXPORTERS = "ABS_5368.0.55.006_Exporters";
	public static final String ABS_5676_0_T7 = "ABS_5676.0_Table7";
	public static final String ABS_5676_0_T19 = "ABS_5676.0_Table19";
	public static final String ABS_5676_0_T21 = "ABS_5676.0_Table21";
	public static final String ABS_5676_0_T22 = "ABS_5676.0_Table22";
	public static final String ABS6524_055_002_EMPLOYEE_T5 = "ABS_6524.055.002_Employee_Table 5";
	public static final String ABS6524_055_002_INVEST_T5 = "ABS_6524.055.002_Investment_Table 5";
	public static final String ABS6524_055_002_INCOME_T5 = "ABS_6524.055.002_Income_Table 5";
	public static final String ABS8155_0_T2 = "ABS_8155.0_Table2";
	public static final String ABS8155_0_T4 = "ABS_8155.0_Table4";
	public static final String ABS8155_0_T5 = "ABS_8155.0_Table5";
	public static final String ABS8155_0_T6 = "ABS_8155.0_Table6";
	public static final String ABS8165_0_STATE_EMPLOYMENT = "ABS_8165.0_StateEmployment";
	public static final String ABS8165_0_STATE_TURNOVER = "ABS_8165.0_StateTurnover";
	public static final String ABS8165_0_LGA_EMPLOYMENT = "ABS_8165.0_LGAEmployment";
	public static final String ABS8165_0_LGA_TURNOVER = "ABS_8165.0_LGATurnover";
	public static final String ABS8165_0_T4 = "ABS_8165.0_Table4";
	public static final String ABS8165_0_T13 = "ABS_8165.0_Table13";
	public static final String ABS8165_0_T17 = "ABS_8165.0_Table17";
	public static final String ABS8167_0_T3 = "ABS_8167.0_Table3";
	public static final String ABS8167_0_T6 = "ABS_8167.0_Table3";
	public static final String LGA_BY_INCP = "Census_LGA_INCP";
	public static final String LGA_BY_MRERD = "Census_LGA_MRERD";
	public static final String LGA_BY_RNTRD = "Census_LGA_RNTRD";
	public static final String ADI_DATA = "ADI_Data";
	public static final String CCY_DATA = "Currency_Data";
	public static final String COUNTRY_DATA = "Country_Data";
	public static final String RBA_BS = "RBA_BalSht";
	public static final String RBA_PL = "RBA_ProfitLoss";
	public static final String GOVT_BS = "Government_BalSht";
	public static final String GOVT_PL = "Government_ProfitLoss";

	// beans
	private AreaMapping area;

	// data
	private boolean dataLoaded;
	private Map<Date, Integer> totalPopulation;
	private Map<Date, Map<String, Integer>> adjustedPeopleByLga;
	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;
	private Map<String, Map<Date, String>> rbaE1; // AU Bal Sht totals
	private Map<String, Map<Date, String>> rbaE2; // AU Bal Sht ratios
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC; // ANZSIC industry code mapping
	private Map<String, Map<String, Map<String, String>>> abs1410_0Economy; // Data by LGA: Economy (keys: year, LGA,
																			// series)
	private Map<String, Map<String, Map<String, String>>> abs1410_0Family; // Data by LGA: Family (keys: year, LGA,
																			// series)
	private Map<String, Map<String, Map<String, String>>> abs1410_0Income; // Data by LGA: Income
	private Map<String, Map<Date, String>> abs3222_0; // AU by gender and age
	private Map<String, Map<Date, String>> abs5368_0Table14a; // exports by country
	private Map<String, Map<Date, String>> abs5368_0Table14b; // imports by country
	private Map<String, Map<Date, String>> abs5368_0Table36a; // exports NSW
	private Map<String, Map<Date, String>> abs5368_0Table36b; // exports VIC
	private Map<String, Map<Date, String>> abs5368_0Table36c; // exports QLD
	private Map<String, Map<Date, String>> abs5368_0Table36d; // exports SA
	private Map<String, Map<Date, String>> abs5368_0Table36e; // exports WA
	private Map<String, Map<Date, String>> abs5368_0Table36f; // exports TAS
	private Map<String, Map<Date, String>> abs5368_0Table36g; // exports NT
	private Map<String, Map<Date, String>> abs5368_0Table36h; // exports ACT
	private Map<String, Map<Date, String>> abs5368_0Table37a; // imports NSW
	private Map<String, Map<Date, String>> abs5368_0Table37b; // imports VIC
	private Map<String, Map<Date, String>> abs5368_0Table37c; // imports QLD
	private Map<String, Map<Date, String>> abs5368_0Table37d; // imports SA
	private Map<String, Map<Date, String>> abs5368_0Table37e; // imports WA
	private Map<String, Map<Date, String>> abs5368_0Table37f; // imports TAS
	private Map<String, Map<Date, String>> abs5368_0Table37g; // imports NT
	private Map<String, Map<Date, String>> abs5368_0Table37h; // imports ACT
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs5368_0Exporters; // formatted export data
																							// (keys: industry, state,
																							// country, value range)
	private Map<String, Map<Date, String>> abs5676_0Table7; // Business Indicators: Sales by State
	private Map<String, Map<Date, String>> abs5676_0Table19; // Business Indicators: Wages by State
	private Map<String, Map<Date, String>> abs5676_0Table21; // Business Indicators: Sales vs Wages Ratio
	private Map<String, Map<Date, String>> abs5676_0Table22; // Business Indicators: Profits vs Sales Ratio
	private Map<String, Map<String, Map<String, String>>> abs6524_055_002EmployeeTable5; // Income by LGA: Employee
																							// (Table 5) (keys: year,
																							// column title without
																							// units, LGA)
	private Map<String, Map<String, Map<String, String>>> abs6524_055_002InvestmentTable5; // Income by LGA: Investments
																							// (Table 5) (keys: year,
																							// column title without
																							// units, LGA)
	private Map<String, Map<String, Map<String, String>>> abs6524_055_002IncomeTable5; // Income by LGA: Income (Table
																						// 5) (keys: year, column title
																						// without units, LGA)
	private Map<String, Map<String, Map<String, String>>> abs8155_0Table2; // labour costs by division (keys: year,
																			// column title, industry)
	private Map<String, Map<String, Map<String, String>>> abs8155_0Table4; // industry performance by division (keys:
																			// year, column title, industry)
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table5; // business size by division
																						// (keys: year, column title,
																						// size, industry)
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8155_0Table6; // states by division (keys:
																						// year, column title, state,
																						// industry)
	/*
	 * Count by state, industry & employment range. Keys are: employment range,
	 * state, industry class code.
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateEmployment;
	/*
	 * Count by state, industry & turnover range. Keys are: turnover range, state,
	 * industry class code.
	 */
	private Map<String, Map<String, Map<String, String>>> abs8165_0StateTurnover;
	/*
	 * Count by LGA, industry & employment range. Keys are: employment range, state,
	 * LGA code, industry division code.
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaEmployment;
	/*
	 * Count by LGA, industry & turnover range. Keys are: turnover range, state, LGA
	 * code, industry division code.
	 */
	private Map<String, Map<String, Map<String, Map<String, String>>>> abs8165_0LgaTurnover;
	private Map<String, Map<String, String>> abs8165_0Table4; // businesses by main state
	private Map<String, Map<String, String>> abs8165_0Table13; // businesses by employment size range
	private Map<String, Map<String, String>> abs8165_0Table17; // businesses by annual turnover range
	private Map<String, Map<String, String>> abs8167_0Table3; // main source of income
	private Map<String, Map<String, String>> abs8167_0Table6; // main supplier
	private Map<String, Map<String, String>> censusLgaByINCP; // LGA by INCP: Household income (title, LGA code, value)
	private Map<String, Map<String, String>> censusLgaByMRERD; // LGA by MRERD: Mortgage repayments (title, LGA code,
																// value)
	private Map<String, Map<String, String>> censusLgaByRNTRD; // LGA by RNTRD: Rent payments (title, LGA code, value)
	private boolean initialisedCensusLgaByINCP;
	private boolean initialisedCensusLgaByMRERD;
	private boolean initialisedCensusLgaByRNTRD;
	private Map<String, Map<String, String>> adiData; // banks, building societies & credit unions
	private Map<String, Map<String, String>> currencyData; // (ISO code, field name, value)
	private Map<String, Map<String, String>> countryData; // (Country name, field name, value)
	private Map<String, Double> rbaBalSht;
	private Map<String, Double> rbaProfitLoss;
	private Map<String, Double> govtBalSht;
	private Map<String, Double> govtProfitLoss;

	/**
	 * 
	 */
	public CalibrationData() {
		super();
		this.init();
	}

	/**
	 * 
	 */
	public CalibrationData(AreaMapping areaMapping) {
		super();
		this.init();
		this.area = areaMapping;
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
			this.loadData();
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
			this.loadData();
		}
		if (this.adjustedPeopleByLga == null) {
			this.adjustedPeopleByLga = new HashMap<Date, Map<String, Integer>>(5);
		}
		Map<String, Integer> result = this.adjustedPeopleByLga.get(date);
		if (result == null) {
			Map<String, Integer> censusPeopleByLga = area.getCensusPeopleByLga();
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
	 * Deletes all the field variables, freeing up memory.
	 */
	@PreDestroy
	public void close() {
		// TODO: implement me
	}

	private void loadData() {
		this.title = new HashMap<String, List<String>>();
		this.unitType = new HashMap<String, List<String>>();

		// load ABS 1292.0.55.002 ANZSIC mapping table
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1292.0.55.002 ANZSIC mapping table");
		this.abs1292_0_55_002ANZSIC = new HashMap<String, Map<String, String>>(4 + 4 + 3 + 2 + 1);
		/*
		 * this.loadAbsDataCsv_1292_0_55_002(
		 * 
		 * + "/data/ABS/1292.0.55.002_ANZSIC/1292.0.55.002_ANZSIC codes formatted.csv",
		 * ABS1292_0_55_002_ANZSIC, this.title, this.abs1292_0_55_002ANZSIC);
		 */
		this.loadAbsDataCsv_1292_0_55_002("/data/ABS/1292.0.55.002_ANZSIC/1292.0.55.002_ANZSIC codes formatted.csv",
				ABS1292_0_55_002_ANZSIC, this.title, this.abs1292_0_55_002ANZSIC);

		// load RBA data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA E1 data");
		this.rbaE1 = new HashMap<String, Map<Date, String>>();
		int[] rbaE1Columns = { 1, 3, 4, 8, 9, 10, 11, 14, 15, 16, 17, 18, 20, 23 };
		this.loadRbaDataCsv("/data/RBA/E_HouseholdBusiness/e1-data.csv", RBA_E1, rbaE1Columns, this.title,
				this.unitType, this.rbaE1);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA E2 data");
		this.rbaE2 = new HashMap<String, Map<Date, String>>();
		int[] rbaE2Columns = { 3, 6, 8, 9, 10 };
		this.loadRbaDataCsv("/data/RBA/E_HouseholdBusiness/e2-data.csv", RBA_E2, rbaE2Columns, this.title,
				this.unitType, this.rbaE2);

		// load ABS 1410.0 data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Economy data");
		this.abs1410_0Economy = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0EconomyColumns = { 49, 50, 51, 52, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99,
				100, 101, 102, 103, 104 };
		String[] abs1410_0EconomyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Economy and Industry, LGA, 2011 to 2017.csv",
				ABS1410_0_ECONOMY, abs1410_0EconomyColumns, abs1410_0EconomyYears, this.title, this.unitType,
				this.abs1410_0Economy);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Family data");
		this.abs1410_0Family = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0FamilyColumns = { 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 41, 42, 49, 57, 58, 59, 60,
				61, 62, 63 };
		String[] abs1410_0FamilyYears = { "2016" };
		this.loadAbsDataCsv_1410_0("/data/ABS/1410.0_DataByRegion/Family and Community, LGA, 2011 to 2017.csv",
				ABS1410_0_FAMILY, abs1410_0FamilyColumns, abs1410_0FamilyYears, this.title, this.unitType,
				this.abs1410_0Family);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1410.0 Income data");
		this.abs1410_0Income = new HashMap<String, Map<String, Map<String, String>>>(7); // 7 years in the data file
		int[] abs1410_0IncomeColumns = { 15, 19, 20, 21, 25, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60 };
		String[] abs1410_0IncomeYears = { "2016" };
		this.loadAbsDataCsv_1410_0(
				"/data/ABS/1410.0_DataByRegion/Income (including Government Allowances), LGA, 2011 to 2017.csv",
				ABS1410_0_INCOME, abs1410_0IncomeColumns, abs1410_0IncomeYears, this.title, this.unitType,
				this.abs1410_0Income);

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

		// load ABS 5368.0 International Trade data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 5368.0 International Trade data");
		System.out.print("   Table 14a");
		this.abs5368_0Table14a = new HashMap<String, Map<Date, String>>();
		int[] abs5368_0Columns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
				24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
				50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75,
				76, 77, 78, 79, 80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100,
				101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
				122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142,
				143, 144, 145, 146, 147, 148, 149, 150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163,
				164, 165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179, 180, 181, 182, 183, 184,
				185, 186, 187, 188, 189, 190, 191, 192, 193, 194, 195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205,
				206, 207, 208, 209, 210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224, 225, 226,
				227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239, 240, 241, 242, 243, 244, 245, 246, 247,
				248, 249, 250, 251, 252 };
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368014a - exports by country.csv", ABS_5368_0_T14A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table14a);

		System.out.print(", 14b");
		this.abs5368_0Table14b = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368014b - imports by country.csv", ABS_5368_0_T14B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table14b);

		System.out.print(", 36a");
		this.abs5368_0Table36a = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036a - merch exports NSW.csv", ABS_5368_0_T36A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36a);

		System.out.print(", 36b");
		this.abs5368_0Table36b = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036b - merch exports VIC.csv", ABS_5368_0_T36B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36b);

		System.out.print(", 36c");
		this.abs5368_0Table36c = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036c - merch exports QLD.csv", ABS_5368_0_T36C,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36c);

		System.out.print(", 36d");
		this.abs5368_0Table36d = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036d - merch exports SA.csv", ABS_5368_0_T36D,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36d);

		System.out.print(", 36e");
		this.abs5368_0Table36e = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036e - merch exports WA.csv", ABS_5368_0_T36E,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36e);

		System.out.print(", 36f");
		this.abs5368_0Table36f = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036f - merch exports TAS.csv", ABS_5368_0_T36F,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36f);

		System.out.print(", 36g");
		this.abs5368_0Table36g = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036g - merch exports NT.csv", ABS_5368_0_T36G,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36g);

		System.out.print(", 36h");
		this.abs5368_0Table36h = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036h - merch exports ACT.csv", ABS_5368_0_T36H,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36h);

		System.out.print(", 37a");
		this.abs5368_0Table37a = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037a - merch imports NSW.csv", ABS_5368_0_T37A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37a);

		System.out.print(", 37b");
		this.abs5368_0Table37b = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037b - merch imports VIC.csv", ABS_5368_0_T37B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37b);

		System.out.print(", 37c");
		this.abs5368_0Table37c = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037c - merch imports QLD.csv", ABS_5368_0_T37C,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37c);

		System.out.print(", 37d");
		this.abs5368_0Table37d = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037d - merch imports SA.csv", ABS_5368_0_T37D,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37d);

		System.out.print(", 37e");
		this.abs5368_0Table37e = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037e - merch imports WA.csv", ABS_5368_0_T37E,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37e);

		System.out.print(", 37f");
		this.abs5368_0Table37f = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037f - merch imports TAS.csv", ABS_5368_0_T37F,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37f);

		System.out.print(", 37g");
		this.abs5368_0Table37g = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037g - merch imports NT.csv", ABS_5368_0_T37G,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37g);

		System.out.println(", 37h");
		this.abs5368_0Table37h = new HashMap<String, Map<Date, String>>();
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037h - merch imports ACT.csv", ABS_5368_0_T37H,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37h);

		// load ABS 53686.0 exporters data
		System.out.println(
				new Date(System.currentTimeMillis()) + ": Loading ABS 5368.0.55.006 Exporters (formatted data)");
		this.abs5368_0Exporters = new HashMap<String, Map<String, Map<String, Map<String, String>>>>(16);
		int[] abs5368_0ExportersColumns = { 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20 };
		this.loadAbsDataCsv_5368_0Exporters("/data/ABS/5368.0.55.006_Exporters/5368.0_exporter data.csv",
				ABS_5368_0_EXPORTERS, abs5368_0ExportersColumns, this.title, this.unitType, this.abs5368_0Exporters);

		// load ABS 5676.0 data
		System.out.println(new Date(System.currentTimeMillis())
				+ ": Loading ABS 5676.0 Business Indicators: Table 7, Sales by State");
		this.abs5676_0Table7 = new HashMap<String, Map<Date, String>>();
		int[] abs5676_0Table7Columns = { 9, 10, 11, 12, 13, 14, 15, 16 }; // loads seasonally adjusted sales
		this.loadAbsDataCsv_Catalogue("/data/ABS/5676.0_BusinessIndicators/Table7_SalesByState.csv", ABS_5676_0_T7,
				abs5676_0Table7Columns, this.title, this.unitType, this.abs5676_0Table7);

		System.out.println(new Date(System.currentTimeMillis())
				+ ": Loading ABS 5676.0 Business Indicators: Table 19, Wages by State");
		this.abs5676_0Table19 = new HashMap<String, Map<Date, String>>();
		int[] abs5676_0Table19Columns = { 10, 11, 12, 13, 14, 15, 16, 17, 18 }; // loads seasonally adjusted wages
		this.loadAbsDataCsv_Catalogue("/data/ABS/5676.0_BusinessIndicators/Table19_WagesByState.csv", ABS_5676_0_T19,
				abs5676_0Table19Columns, this.title, this.unitType, this.abs5676_0Table19);

		System.out.println(new Date(System.currentTimeMillis())
				+ ": Loading ABS 5676.0 Business Indicators: Table 21, Sales vs Wages Ratio");
		this.abs5676_0Table21 = new HashMap<String, Map<Date, String>>();
		int[] abs5676_0Table21Columns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }; // loads sales to wages
																								// ratio by industry
		this.loadAbsDataCsv_Catalogue("/data/ABS/5676.0_BusinessIndicators/Table21_SalesVsWagesRatio.csv",
				ABS_5676_0_T21, abs5676_0Table21Columns, this.title, this.unitType, this.abs5676_0Table21);

		System.out.println(new Date(System.currentTimeMillis())
				+ ": Loading ABS 5676.0 Business Indicators: Table 22, Profits vs Sales Ratio");
		this.abs5676_0Table22 = new HashMap<String, Map<Date, String>>();
		int[] abs5676_0Table22Columns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }; // loads profit to sales
																								// ratio by industry
		this.loadAbsDataCsv_Catalogue(

				"/data/ABS/5676.0_BusinessIndicators/Table22_ProfitsVsSalesRatio.csv", ABS_5676_0_T22,
				abs5676_0Table22Columns, this.title, this.unitType, this.abs5676_0Table22);

		// load ABS 6524 employee
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 6524.055.002 Employee data");
		this.abs6524_055_002EmployeeTable5 = new HashMap<String, Map<String, Map<String, String>>>(6); // 6 years
		int[] abs6524_055_002EmployeeTable5Columns = { 19 };
		this.loadAbsDataCsv_6524_0("/data/ABS/6524.0.55.002_IncomeByLGA/Employee income_Table5.csv",
				ABS6524_055_002_EMPLOYEE_T5, abs6524_055_002EmployeeTable5Columns, this.title, this.unitType,
				this.abs6524_055_002EmployeeTable5);

		// ABS 6524 investment
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 6524.055.002 Investment data");
		this.abs6524_055_002InvestmentTable5 = new HashMap<String, Map<String, Map<String, String>>>(6); // 6 years
		int[] abs6524_055_002InvestmentTable5Columns = { 7, 13, 19, 25, 31, 37 };
		this.loadAbsDataCsv_6524_0("/data/ABS/6524.0.55.002_IncomeByLGA/Investment income_Table5.csv",
				ABS6524_055_002_INVEST_T5, abs6524_055_002InvestmentTable5Columns, this.title, this.unitType,
				this.abs6524_055_002InvestmentTable5);

		// ABS 6524 income
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 6524.055.002 Income data");
		this.abs6524_055_002IncomeTable5 = new HashMap<String, Map<String, Map<String, String>>>(6); // 6 years
		int[] abs6524_055_002IncomeTable5Columns = { 7, 13, 19, 25, 31 };
		this.loadAbsDataCsv_6524_0("/data/ABS/6524.0.55.002_IncomeByLGA/Total income_Table5.csv",
				ABS6524_055_002_INCOME_T5, abs6524_055_002IncomeTable5Columns, this.title, this.unitType,
				this.abs6524_055_002IncomeTable5);

		// ABS 8155.0
		System.out
				.println(new Date(System.currentTimeMillis()) + ": Loading ABS 8155.0 Australian industry by division");
		System.out.print("   Table 2");
		this.abs8155_0Table2 = new HashMap<String, Map<String, Map<String, String>>>();
		int[] abs8155_0Table2Columns = { 1, 2, 3, 5, 6 };
		String[] abs8155_0Table2Years = { "2016–17" };
		int abs8155_0Table2TitleRow = 4;
		int abs8155_0Table2UnitsRow = 5;
		this.loadAbsDataCsv_8155_0T2T4("/data/ABS/8155.0_IndustryByDivision/Table2_LabourCosts.csv", ABS8155_0_T2,
				abs8155_0Table2Columns, abs8155_0Table2Years, abs8155_0Table2TitleRow, abs8155_0Table2UnitsRow,
				this.title, this.unitType, this.abs8155_0Table2);

		System.out.print(", Table 4");
		this.abs8155_0Table4 = new HashMap<String, Map<String, Map<String, String>>>();
		int[] abs8155_0Table4Columns = { 1, 2, 3, 4, 5, 7, 8, 9, 10, 11 };
		String[] abs8155_0Table4Years = { "2016–17" };
		int abs8155_0Table4TitleRow = 6;
		int abs8155_0Table4UnitsRow = 7;
		this.loadAbsDataCsv_8155_0T2T4(

				"/data/ABS/8155.0_IndustryByDivision/Table4_IndustryPerformance.csv", ABS8155_0_T4,
				abs8155_0Table4Columns, abs8155_0Table4Years, abs8155_0Table4TitleRow, abs8155_0Table4UnitsRow,
				this.title, this.unitType, this.abs8155_0Table4);

		System.out.print(", Table 5");
		this.abs8155_0Table5 = new HashMap<String, Map<String, Map<String, Map<String, String>>>>();
		int[] abs8155_0Table5Columns = { 3, 6, 9, 12, 15, 18, 21 };
		int abs8155_0Table5TitleRow = 4;
		int abs8155_0Table5UnitsRow = 6;
		this.loadAbsDataCsv_8155_0T5T6("/data/ABS/8155.0_IndustryByDivision/Table5_BusinessSize.csv", ABS8155_0_T5,
				abs8155_0Table5Columns, abs8155_0Table5TitleRow, abs8155_0Table5UnitsRow, this.title, this.unitType,
				this.abs8155_0Table5);

		System.out.print(", Table 6.");
		this.abs8155_0Table6 = new HashMap<String, Map<String, Map<String, Map<String, String>>>>();
		int[] abs8155_0Table6Columns = { 3, 6, 9 };
		int abs8155_0Table6TitleRow = 4;
		int abs8155_0Table6UnitsRow = 6;
		this.loadAbsDataCsv_8155_0T5T6("/data/ABS/8155.0_IndustryByDivision/Table6_States.csv", ABS8155_0_T6,
				abs8155_0Table6Columns, abs8155_0Table6TitleRow, abs8155_0Table6UnitsRow, this.title, this.unitType,
				this.abs8155_0Table6);

		// ABS 8165.0 Count of Businesses
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 8165.0 count of businesses");
		System.out.print("   State Employment");
		int[] abs8165_0StateEmploymentColumns = { 23, 24, 25, 26 };
		int abs8165_0StateEmploymentTitleRow = 5;
		int abs8165_0StateEmploymentUnitsRow = 6;
		this.abs8165_0StateEmployment = new HashMap<String, Map<String, Map<String, String>>>(
				abs8165_0StateEmploymentColumns.length);
		this.loadAbsDataCsv_8165_0State(

				"/data/ABS/8165.0_CountOfBusinesses/8165.0_by State, Industry Code & Employment Size.csv",
				ABS8165_0_STATE_EMPLOYMENT, abs8165_0StateEmploymentColumns, abs8165_0StateEmploymentTitleRow,
				abs8165_0StateEmploymentUnitsRow, this.title, this.unitType, this.abs8165_0StateEmployment);

		System.out.print(", State Turnover");
		int[] abs8165_0StateTurnoverColumns = { 31, 32, 33, 34, 35, 36 };
		int abs8165_0StateTurnoverTitleRow = 5;
		int abs8165_0StateTurnoverUnitsRow = 6;
		this.abs8165_0StateTurnover = new HashMap<String, Map<String, Map<String, String>>>(
				abs8165_0StateTurnoverColumns.length);
		this.loadAbsDataCsv_8165_0State(

				"/data/ABS/8165.0_CountOfBusinesses/8165.0_by State, Industry Code & Turnover.csv",
				ABS8165_0_STATE_TURNOVER, abs8165_0StateTurnoverColumns, abs8165_0StateTurnoverTitleRow,
				abs8165_0StateTurnoverUnitsRow, this.title, this.unitType, this.abs8165_0StateTurnover);

		System.out.print(", LGA Employment");
		int[] abs8165_0LgaEmploymentColumns = { 5, 6, 7, 8 };
		this.abs8165_0LgaEmployment = new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
				abs8165_0LgaEmploymentColumns.length);
		this.loadAbsDataCsv_8165_0Lga(

				"/data/ABS/8165.0_CountOfBusinesses/8165.0_by State, LGA, Industry & Employment Size.csv",
				ABS8165_0_LGA_EMPLOYMENT, abs8165_0LgaEmploymentColumns, this.title, this.unitType,
				this.abs8165_0LgaEmployment);

		System.out.print(", LGA Turnover");
		int[] abs8165_0LgaTurnoverColumns = { 5, 6, 7, 8, 9, 10 };
		this.abs8165_0LgaTurnover = new HashMap<String, Map<String, Map<String, Map<String, String>>>>(
				abs8165_0LgaTurnoverColumns.length);
		this.loadAbsDataCsv_8165_0Lga(

				"/data/ABS/8165.0_CountOfBusinesses/8165.0_by State, LGA, Industry & Turnover.csv",
				ABS8165_0_LGA_TURNOVER, abs8165_0LgaTurnoverColumns, this.title, this.unitType,
				this.abs8165_0LgaTurnover);

		System.out.print(", Table 4");
		this.abs8165_0Table4 = new HashMap<String, Map<String, String>>(1);
		int[] abs8165_0Table4Columns = { 4 };
		int[] abs8165_0Table4Rows = { 40, 41, 42, 43, 44, 45, 46, 47, 48 };
		int abs8165_0Table4TitleRow = 4;
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8165.0_CountOfBusinesses/8165.0_Table4_State.csv", ABS8165_0_T4,
				abs8165_0Table4Columns, abs8165_0Table4Rows, abs8165_0Table4TitleRow, this.title, this.abs8165_0Table4);

		System.out.print(", Table 13");
		this.abs8165_0Table13 = new HashMap<String, Map<String, String>>(1);
		int[] abs8165_0Table13Columns = { 5 };
		int[] abs8165_0Table13Rows = { 34, 36, 37, 38, 39 };
		int abs8165_0Table13TitleRow = 4;
		this.loadAbsDataRowsColumnsCsv(

				"/data/ABS/8165.0_CountOfBusinesses/8165.0_Table13_EmploymentSize.csv", ABS8165_0_T13,
				abs8165_0Table13Columns, abs8165_0Table13Rows, abs8165_0Table13TitleRow, this.title,
				this.abs8165_0Table13);

		System.out.print(", Table 17");
		this.abs8165_0Table17 = new HashMap<String, Map<String, String>>(1);
		int[] abs8165_0Table17Columns = { 5 };
		int[] abs8165_0Table17Rows = { 31, 32, 33, 34, 35, 36 };
		int abs8165_0Table17TitleRow = 4;
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8165.0_CountOfBusinesses/8165.0_Table17_Turnover.csv", ABS8165_0_T17,
				abs8165_0Table17Columns, abs8165_0Table17Rows, abs8165_0Table17TitleRow, this.title,
				this.abs8165_0Table17);

		System.out.print(", state & employment");

		System.out.print(", state & turnover");

		System.out.print(", LGA & employment");

		System.out.println(", LGA & turnover.");

		// ABS 8167.0 Business Markets and Competition
		System.out.println(
				new Date(System.currentTimeMillis()) + ": Loading ABS 8167.0 business markets and competition");
		this.abs8167_0Table3 = new HashMap<String, Map<String, String>>();
		int[] abs8167_0Table3Columns = { 1, 2, 3, 4, 5 };
		int[] abs8167_0Table3Rows = { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 };
		int abs8167_0titleRow = 6;
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8167.0_BusMktAndComp/Table3.csv", ABS8167_0_T3,
				abs8167_0Table3Columns, abs8167_0Table3Rows, abs8167_0titleRow, this.title, this.abs8167_0Table3);

		this.abs8167_0Table6 = new HashMap<String, Map<String, String>>();
		int[] abs8167_0Table6Columns = { 1, 2, 3, 4, 5 };
		int[] abs8167_0Table6Rows = { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 };
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8167.0_BusMktAndComp/Table6.csv", ABS8167_0_T6,
				abs8167_0Table6Columns, abs8167_0Table6Rows, abs8167_0titleRow, this.title, this.abs8167_0Table6);

		// ABS Census: LGA by INCP
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS Census LGA by INCP data");
		this.censusLgaByINCP = new HashMap<String, Map<String, String>>();
		int[] censusLgaByINCPColumns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18 };
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - ACT.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - NSW.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - NT.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - OT.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - QLD.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - SA.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - TAS.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - VIC.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by INCP/LGA (UR) by INCP - WA.csv", CalibrationData.LGA_BY_INCP,
				censusLgaByINCPColumns, this.initialisedCensusLgaByINCP, this.title, this.censusLgaByINCP);

		// ABS Census: LGA by MRERD
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS Census LGA by MRERD data");
		this.censusLgaByMRERD = new HashMap<String, Map<String, String>>();
		int[] censusLgaByMRERDColumns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21,
				22 };
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - ACT.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - NSW.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - NT.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - OT.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - QLD.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - SA.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - TAS.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - VIC.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by MRERD/LGA by MRERD - WA.csv", CalibrationData.LGA_BY_MRERD,
				censusLgaByMRERDColumns, this.initialisedCensusLgaByMRERD, this.title, this.censusLgaByMRERD);

		// ABS Census: LGA by RNTRD
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS Census LGA by RNTRD data");
		this.censusLgaByRNTRD = new HashMap<String, Map<String, String>>();
		int[] censusLgaByRNTRDColumns = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
				23, 24, 25, 26 };
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - ACT.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - NSW.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - NT.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - OT.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - QLD.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - SA.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - TAS.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - VIC.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);
		this.loadAbsCensusTableCsv2D(

				"/data/ABS/CensusTableBuilder2016/LGA by RNTRD/LGA by RNTRD - WA.csv", CalibrationData.LGA_BY_RNTRD,
				censusLgaByRNTRDColumns, this.initialisedCensusLgaByRNTRD, this.title, this.censusLgaByRNTRD);

		// load pre-formatted ADI data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ADI data");
		this.adiData = new HashMap<String, Map<String, String>>();
		int[] adiColumns = { 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
				28, 29, 30, 31, 32, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 56, 57, 58, 59,
				60, 63, 66, 67, 68, 79 };
		String[] adiCategories = { "Major Bank", "Other Domestic Bank", "Foreign Bank", "Mutual ADI" };
		this.loadAdiDataCsv("/data/ADI/ADI_data.csv", CalibrationData.ADI_DATA, adiColumns, adiCategories, this.title,
				this.unitType, this.adiData);

		// load pre-formatted currency data (approx 5.51 kB)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading currency data");
		this.currencyData = new HashMap<String, Map<String, String>>();
		int[] currencyColumns = { 1, 61, 62, 63, 64 };
		this.loadCurrencyDataCsv("/data/FxRates/FX Rates_Monthly.csv", CalibrationData.CCY_DATA, currencyColumns,
				this.title, this.currencyData);

		// load pre-formatted country data (approx 4.83 kB)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading country data");
		this.countryData = new HashMap<String, Map<String, String>>();
		int[] countryColumns = { 1, 3 };
		this.loadCountryDataCsv("/data/FxRates/CountriesCurrencies_ABS.csv", CalibrationData.COUNTRY_DATA,
				countryColumns, this.title, this.countryData);

		// load RBA Balance Sheet (337 bytes) and Profit & Loss Statement (335 bytes)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA financial statements");
		this.rbaBalSht = new HashMap<String, Double>();
		int[] rbaBalShtRows = { 3, 4, 5, 6, 7, 11, 12, 13, 14, 18, 19, 25, 26, 27, 28, 29, 30, 31 };
		int rbaBalShtColumn = 2;
		double rbaMultiplier = 1d;
		this.loadFinancialStatementCsv("/data/RBA/RBA_BalSht.csv", CalibrationData.RBA_BS, rbaBalShtColumn,
				rbaBalShtRows, this.title, this.rbaBalSht, rbaMultiplier);

		this.rbaProfitLoss = new HashMap<String, Double>();
		int[] rbaProfitLossRows = { 3, 4, 5, 6, 7, 8, 9, 13, 14, 15, 18, 19 };
		int rbaProfitLossColumn = 2;
		this.loadFinancialStatementCsv("/data/RBA/RBA_PnL.csv", CalibrationData.RBA_PL, rbaProfitLossColumn,
				rbaProfitLossRows, this.title, this.rbaProfitLoss, rbaMultiplier);

		// load Australian Government Financial Statistics
		System.out
				.println(new Date(System.currentTimeMillis()) + ": Loading Australian Government financial statements");
		this.govtBalSht = new HashMap<String, Double>();
		int[] govtBalShtRows = { 8, 10, 12, 13, 15, 16, 22, 23, 25, 29 };
		int govtBalShtColumn = 10;
		double govtMultiplier = 1000000d;
		this.loadFinancialStatementCsv(

				"/data/ABS/5512.0_GovtFinStats/55120DO057_201617 - Table 3 - Bal Sht.csv", CalibrationData.GOVT_PL,
				govtBalShtColumn, govtBalShtRows, this.title, this.govtBalSht, govtMultiplier);

		this.govtProfitLoss = new HashMap<String, Double>();
		int[] govtProfitLossRows = { 7, 9, 10, 12, 17, 18, 22, 23, 37, 48 };
		int govtProfitLossColumn = 10;
		this.loadFinancialStatementCsv(

				"/data/ABS/5512.0_GovtFinStats/55120DO057_201617 - Table 1 - P&L.csv", CalibrationData.GOVT_PL,
				govtProfitLossColumn, govtProfitLossRows, this.title, this.govtProfitLoss, govtMultiplier);

		// set flag so we only load the data once
		System.out.println(new Date(System.currentTimeMillis()) + ": Data loaded");
		this.dataLoaded = true;
	}

	/**
	 * Household and Business Balance Sheets – E1
	 * 
	 * REFERENCE: RBA (2018) 'Household and Business Balance Sheets – E1',
	 * Statistical Tables: Household and Business Finances: June 2018, Sydney, NSW:
	 * Reserve Bank of Australia.
	 * 
	 * Loads ABS pre-prepared catalogue data.
	 * 
	 * File pre-conditions:<br>
	 * 1. The column titles row has "Title" in the first column.<br>
	 * 2. The unit type row has "Units" in the first column.<br>
	 * 3. The last header row has "Series ID" in the first column.<br>
	 * 4. The first column contains the dates in the format MMM-yyyy.
	 * 
	 * Data sources this works for include: E1 Household & Business Balance Sheets
	 * E2 Household Finances - Selected Ratios
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to store this series' data in the
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
	private void loadRbaDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units, Map<String, Map<Date, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-yyyy");
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Title")) {
						// store title
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 1; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Units")) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 1; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 1; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new HashMap<Date, String>());
						}
						header = false;
					}
				} else {
					if (line[0].isEmpty()) {
						footer = true;
					} else {
						for (int i = 1; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(seriesId[i]).put(dateFormat.parse(line[0]), line[columnsToImport[i]]);
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
		} catch (ParseException e) {
			// parsing date from string
			e.printStackTrace();
		}
	}

	/**
	 * Imports an ANZSIC code table so we can map between levels in the hierarchy.
	 * 
	 * @param               - the URI of the file to import
	 * @param catalogueName - the name used to store this series' data in the maps
	 * @param titles        - this is a list of the mapping pairs (e.g. "Group Code
	 *                      to Subdivision Code")
	 * @param data          - key 1 is mapping per the titles (e.g. "Class Code to
	 *                      Division") while key 2 is the code or description (e.g.
	 *                      "Division Code").
	 */
	private void loadAbsDataCsv_1292_0_55_002(String fileResourceLocation, String catalogueName,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 1;
			final int titleRow = 4;
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title
						List<String> mappingTitles = new ArrayList<String>(4 + 4 + 3 + 2 + 1);
						mappingTitles.add("Division Code to Division");
						mappingTitles.add("Subdivision Code to Subdivision");
						mappingTitles.add("Group Code to Group");
						mappingTitles.add("Class Code to Class");
						mappingTitles.add("Division to Division Code");
						mappingTitles.add("Subdivision to Subdivision Code");
						mappingTitles.add("Group to Group Code");
						mappingTitles.add("Class to Class Code");
						mappingTitles.add("Class Code to Group Code");
						mappingTitles.add("Class Code to Subdivision Code");
						mappingTitles.add("Class Code to Division Code");
						mappingTitles.add("Group Code to Subdivision Code");
						mappingTitles.add("Group Code to Division Code");
						mappingTitles.add("Subdivision Code to Division Code");
						titles.put(catalogueName, mappingTitles);

						// store mapping titles as key with blank collections to populate with data
						// below
						for (int i = 0; i < mappingTitles.size(); i++) {
							data.put(mappingTitles.get(i), new HashMap<String, String>());
						}
						header = false;
					}
					currentRow++;
				} else {
					// parse the body of the data
					data.get("Division Code to Division").put(line[0], line[1]);
					data.get("Subdivision Code to Subdivision").put(line[2], line[3]);
					data.get("Group Code to Group").put(line[4], line[5]);
					data.get("Class Code to Class").put(line[6], line[7]);
					data.get("Division to Division Code").put(line[1], line[0]);
					data.get("Subdivision to Subdivision Code").put(line[3], line[2]);
					data.get("Group to Group Code").put(line[5], line[4]);
					data.get("Class to Class Code").put(line[7], line[6]);
					data.get("Class Code to Group Code").put(line[6], line[4]);
					data.get("Class Code to Subdivision Code").put(line[6], line[2]);
					data.get("Class Code to Division Code").put(line[6], line[0]);
					data.get("Group Code to Subdivision Code").put(line[4], line[2]);
					data.get("Group Code to Division Code").put(line[4], line[0]);
					data.get("Subdivision Code to Division Code").put(line[2], line[0]);
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
	 * private void loadAbsDataCsv_1292_0_55_002(String fileURI, String
	 * catalogueName, Map<String, List<String>> titles, Map<String, Map<String,
	 * String>> data) {
	 * 
	 * CSVReader reader = null; try { reader = new CSVReader(new
	 * FileReader(fileURI)); boolean header = true; int currentRow = 1; final int
	 * titleRow = 4; String[] line = null; while ((line = reader.readNext()) !=
	 * null) { if (header) { if (currentRow == titleRow) { // store title
	 * List<String> mappingTitles = new ArrayList<String>(4 + 4 + 3 + 2 + 1);
	 * mappingTitles.add("Division Code to Division");
	 * mappingTitles.add("Subdivision Code to Subdivision");
	 * mappingTitles.add("Group Code to Group");
	 * mappingTitles.add("Class Code to Class");
	 * mappingTitles.add("Division to Division Code");
	 * mappingTitles.add("Subdivision to Subdivision Code");
	 * mappingTitles.add("Group to Group Code");
	 * mappingTitles.add("Class to Class Code");
	 * mappingTitles.add("Class Code to Group Code");
	 * mappingTitles.add("Class Code to Subdivision Code");
	 * mappingTitles.add("Class Code to Division Code");
	 * mappingTitles.add("Group Code to Subdivision Code");
	 * mappingTitles.add("Group Code to Division Code");
	 * mappingTitles.add("Subdivision Code to Division Code");
	 * titles.put(catalogueName, mappingTitles);
	 * 
	 * // store mapping titles as key with blank collections to populate with data
	 * // below for (int i = 0; i < mappingTitles.size(); i++) {
	 * data.put(mappingTitles.get(i), new HashMap<String, String>()); } header =
	 * false; } currentRow++; } else { // parse the body of the data
	 * data.get("Division Code to Division").put(line[0], line[1]);
	 * data.get("Subdivision Code to Subdivision").put(line[2], line[3]);
	 * data.get("Group Code to Group").put(line[4], line[5]);
	 * data.get("Class Code to Class").put(line[6], line[7]);
	 * data.get("Division to Division Code").put(line[1], line[0]);
	 * data.get("Subdivision to Subdivision Code").put(line[3], line[2]);
	 * data.get("Group to Group Code").put(line[5], line[4]);
	 * data.get("Class to Class Code").put(line[7], line[6]);
	 * data.get("Class Code to Group Code").put(line[6], line[4]);
	 * data.get("Class Code to Subdivision Code").put(line[6], line[2]);
	 * data.get("Class Code to Division Code").put(line[6], line[0]);
	 * data.get("Group Code to Subdivision Code").put(line[4], line[2]);
	 * data.get("Group Code to Division Code").put(line[4], line[0]);
	 * data.get("Subdivision Code to Division Code").put(line[2], line[0]); } }
	 * reader.close(); } catch (FileNotFoundException e) { // open file
	 * e.printStackTrace(); } catch (IOException e) { // read next
	 * e.printStackTrace(); } }
	 */

	/**
	 * Loads ABS 1410.0 catalogue data.
	 * 
	 * TODO: check if the column titles are too long to be keys in a Map<String, T>
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param yearsToImport        - a list of years to import data for (e.g.
	 *                             "2016")
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Year, Series ID, LGA.
	 */
	private void loadAbsDataCsv_1410_0(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			String[] yearsToImport, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, String>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 1;
			final int titleRow = 7;
			final int yearCol = 2;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title
						titles.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(catalogueName).add(line[columnsToImport[i]]);
							seriesId[i] = new String(line[columnsToImport[i]]);
						}

						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < yearsToImport.length; i++) {
							data.put(yearsToImport[i], new HashMap<String, Map<String, String>>());
							for (int j = 0; j < columnsToImport.length; j++) {
								data.get(yearsToImport[i]).put(line[columnsToImport[j]], new HashMap<String, String>());
							}
						}
					} else if (line[0].equals("CODE")) {
						// store unit types
						units.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(catalogueName).add(line[columnsToImport[i]]);
						}
						header = false;
					}
					currentRow++;
				} else {
					// check if we should import this year
					boolean importThisRow = false;
					for (int i = 0; i < yearsToImport.length; i++) {
						if (line[yearCol].equals(yearsToImport[i])) {
							importThisRow = true;
							break;
						}
					}
					if (importThisRow) {
						// for (int j = 1; j < columnsToImport.length; j++) {
						for (int j = 0; j < columnsToImport.length; j++) {
							// parse the body of the data
							data.get(line[yearCol]).get(seriesId[j]).put(line[0], line[columnsToImport[j]]);
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
			SimpleDateFormat dateFormat = new SimpleDateFormat("MMM-yyyy");
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
	 * Formatted export data (keys: industry, country, state, value range)
	 * 
	 * The first key is the column headings, then the rest of hte keys are in the
	 * same order that the data is sorted in. This makes knowing when to create a
	 * new child map easier because the key will be different to the previous line's
	 * key.
	 * 
	 * @param fileResourceLocation
	 * @param catalogueName
	 * @param columnsToImport
	 * @param titles
	 * @param units
	 * @param data
	 */
	private void loadAbsDataCsv_5368_0Exporters(String fileResourceLocation, String catalogueName,
			int[] columnsToImport, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, Map<String, String>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 0;
			int titleRow = 4; // zero-based
			int unitsRow = 5;
			int lastHeaderRow = 6;
			String prevCountry = "";
			String prevState = "";
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						titles.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							titles.get(catalogueName).add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];

							// create top-level maps
							data.put(seriesId[i], new HashMap<String, Map<String, Map<String, String>>>());
						}
					} else if (currentRow == unitsRow) {
						units.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							// store unit types
							units.get(catalogueName).add(line[columnsToImport[i]]);
						}
					} else if (currentRow == lastHeaderRow) {
						header = false;
					}
					currentRow++;
				} else {
					for (int i = 0; i < columnsToImport.length; i++) {
						// check if we need to create a new Country map
						if (!line[0].equals(prevCountry)) {
							data.get(seriesId[i]).put(line[0], new HashMap<String, Map<String, String>>());
						}
						// check if we need to create a new State map
						if (!line[0].equals(prevState)) {
							data.get(seriesId[i]).get(line[0]).put(line[2], new HashMap<String, String>());
						}

						// parse the body of the data
						data.get(seriesId[i]).get(line[0]).get(line[2]).put(line[1], line[columnsToImport[i]]);
					}
					prevCountry = line[0];
					prevState = line[2];
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
	 * Loads ABS 6524.0.55.002 catalogue data.
	 * 
	 * TODO: check if the column titles are too long to be keys in a Map<String, T>
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             date and is imported only as the key for the
	 *                             other columns' data.
	 * @param yearsToImport        - a list of years to import data for (e.g.
	 *                             "2016")
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Year, Series ID, LGA.
	 */
	private void loadAbsDataCsv_6524_0(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, String>>> data) {

		/*
		 * use previous column heading from row 6 if this cell is empty.<br> use years
		 * from row 7 as the first key, column heading from row 6 as the 2nd key, LGA as
		 * 3rd key/
		 */

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 1;
			int titleRow = 6;
			String[] seriesId = new String[columnsToImport.length];
			String[] year = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						titles.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						units.put(catalogueName, new ArrayList<String>(columnsToImport.length));
						String tmpTitle = null;
						String tmpUnits = null;
						for (int i = 0; i < line.length; i++) {
							// parse header cells to create title & units
							if (!line[i].isEmpty()) {
								String tmp = line[i];
								tmpTitle = tmp.substring(0, tmp.indexOf("(") - 1);
								tmpUnits = tmp.substring(tmp.indexOf("(") + 1);
								tmpUnits = tmpUnits.substring(0, tmpUnits.indexOf(")"));
							}

							// determine if this is a column/year we should import
							int j = 0;
							titleLoop: while (j < columnsToImport.length) {
								if (i == columnsToImport[j]) {
									// import column because it's chosen
									// store title (excluding the unit type in brackets)
									titles.get(catalogueName).add(tmpTitle);

									// store unit types (the unit type in brackets, parsed from the title)
									units.get(catalogueName).add(tmpUnits);

									seriesId[j] = tmpTitle;
									break titleLoop;
								}
								j++;
							}
						}
					} else if (line[0].equals("LGA")) {
						for (int i = 0; i < line.length; i++) {
							// determine if this is a column/year we should import
							int j = 0;
							yearLoop: while (j < columnsToImport.length) {
								if (i == columnsToImport[j]) {
									// import column because it's chosen
									if (!data.containsKey(line[i])) {
										// this is the first column for this year, so add it to the map
										// store series ID as key with blank collections to populate with data below
										data.put(line[i], new HashMap<String, Map<String, String>>());
										for (int k = 0; k < columnsToImport.length; k++) {
											data.get(line[i]).put(seriesId[k], new HashMap<String, String>());
										}
									}
									// store year
									year[j] = line[columnsToImport[j]];
									break yearLoop;
								}
								j++;
							}
						}
						header = false;
					}
					currentRow++;
				} else {
					for (int i = 0; i < columnsToImport.length; i++) {
						// parse the body of the data
						data.get(year[i]).get(seriesId[i]).put(line[0], line[columnsToImport[i]]);
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
	 * Loads ABS 8155.0 Table 2 and Table 4 data.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param yearsToImport        - a list of years to import data for (e.g.
	 *                             "2016")
	 * @param titleRow             - the zero-based row number of the series titles.
	 * @param unitsRow             - the zero-based rown number of the unit types.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Year, Series Title, Industry.
	 */
	private void loadAbsDataCsv_8155_0T2T4(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			String[] yearsToImport, int titleRow, int unitsRow, Map<String, List<String>> titles,
			Map<String, List<String>> units, Map<String, Map<String, Map<String, String>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 0;
			final int yearCol = 0;
			String[] seriesId = new String[columnsToImport.length];
			String industry = "";
			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (currentRow == titleRow) {
						// store title
						List<String> titlesInFile = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							titlesInFile.add(line[columnsToImport[i]]);
							seriesId[i] = new String(line[columnsToImport[i]]);
						}
						titles.put(catalogueName, titlesInFile);

						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < yearsToImport.length; i++) {
							Map<String, Map<String, String>> columnMaps = new HashMap<String, Map<String, String>>(
									columnsToImport.length);
							for (int j = 0; j < columnsToImport.length; j++) {
								columnMaps.put(line[columnsToImport[j]], new HashMap<String, String>());
							}
							data.put(yearsToImport[i], columnMaps);
						}
					} else if (currentRow == unitsRow) {
						// store unit types
						List<String> unitTypes = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							unitTypes.add(line[columnsToImport[i]]);
						}
						units.put(catalogueName, unitTypes);
						header = false;
					}
					currentRow++;
				} else {
					// check if this is an industry category row
					if (!line[0].contains("–")) {
						// industry category
						industry = line[0];
					} else if (line[0].isEmpty()) {
						footer = true;
					} else {
						// year data row
						// check if we should import this year
						boolean importThisRow = false;
						int i = 0;
						yearCheck: while (i < yearsToImport.length) {
							if (line[yearCol].equals(yearsToImport[i])) {
								importThisRow = true;
								break yearCheck;
							}
							i++;
						}
						if (importThisRow) {
							for (int j = 0; j < columnsToImport.length; j++) {
								// parse the body of the data
								data.get(line[yearCol]).get(seriesId[j]).put(industry, line[columnsToImport[j]]);
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

	/**
	 * Loads ABS 8155.0 Table 5 and Table 6 data.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titleRow             - the zero-based row number of the series titles.
	 * @param unitsRow             - the zero-based rown number of the unit types.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Year, Series Title, State/Size,
	 *                             Industry.
	 */
	private void loadAbsDataCsv_8155_0T5T6(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			int titleRow, int unitsRow, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, Map<String, String>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 0;
			final int yearRow = 5;
			String[] seriesId = new String[columnsToImport.length];
			String[] years = new String[columnsToImport.length];
			String sizeOrState = "";
			boolean isNewCategory = false;
			String[] line = null;
			while (((line = reader.readNext()) != null) && !footer) {
				if (header) {
					if (currentRow == titleRow) {
						// store title
						String prevTitle = "";
						Set<String> titlesInFile = new HashSet<String>(columnsToImport.length);
						int index = 0;
						for (int i = 0; i < line.length; i++) {
							if (!line[i].isEmpty()) {
								prevTitle = line[i];
							}
							boolean importThisColumn = false;
							int j = 0;
							titleLoop: while (j < columnsToImport.length) {
								if (i == columnsToImport[j]) {
									importThisColumn = true;
									break titleLoop;
								}
								j++;
							}
							if (importThisColumn) {
								titlesInFile.add(prevTitle);
								seriesId[index] = new String(prevTitle);
								index++;
							}
						}
						List<String> titlesList = new ArrayList<String>(titlesInFile.size());
						for (String thisTitle : titlesInFile) {
							titlesList.add(thisTitle);
						}
						titles.put(catalogueName, titlesList);
					} else if (currentRow == yearRow) {
						// KEYS: Year, Series Title, State/Size, Industry

						// get unique list of all years
						Set<String> uniqueYears = new HashSet<String>();
						for (int i = 0; i < columnsToImport.length; i++) {
							uniqueYears.add(line[columnsToImport[i]]);
							years[i] = line[columnsToImport[i]]; // non-unique list that can be referenced using indices
						}

						// get unique list of column titles
						Set<String> uniqueColumnTitles = Sets.newHashSet(seriesId);

						// create title maps for each year
						// year, column title, state/size, industry
						for (String thisYear : uniqueYears) {
							Map<String, Map<String, Map<String, String>>> theseTitles = new HashMap<String, Map<String, Map<String, String>>>();
							for (String thisTitle : uniqueColumnTitles) {
								theseTitles.put(thisTitle, new HashMap<String, Map<String, String>>());
							}
							data.put(thisYear, theseTitles);
						}
					} else if (currentRow == unitsRow) {
						// store unit types
						List<String> unitTypes = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							unitTypes.add(line[columnsToImport[i]]);
						}
						units.put(catalogueName, unitTypes);
						header = false;
					}
					currentRow++;
				} else {
					// check if this is a category row
					if (line[0].isEmpty()) {
						footer = true;
					} else if (line[1].isEmpty()) {
						// size or state category
						sizeOrState = line[0];
						isNewCategory = true;
					} else {
						// year, column title, state/size, industry
						if (isNewCategory) {
							// initialise category maps
							for (int i = 0; i < columnsToImport.length; i++) {
								data.get(years[i]).get(seriesId[i]).put(sizeOrState, new HashMap<String, String>());
							}
							isNewCategory = false;
						}
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(years[i]).get(seriesId[i]).get(sizeOrState).put(line[0], line[columnsToImport[i]]);
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

	/**
	 * Loads ABS 8165.0 State-based data.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titleRow             - the zero-based row number of the series titles.
	 * @param unitsRow             - the zero-based rown number of the unit types.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Employment or Turnover Range, State,
	 *                             Industry.
	 */
	private void loadAbsDataCsv_8165_0State(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			int titleRow, int unitsRow, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, String>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			// String state = "";
			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("State")) {
						// store title
						List<String> titlesInFile = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							titlesInFile.add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];
						}
						titles.put(catalogueName, titlesInFile);

						// create title maps for each year
						String[] states = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "Other" };
						for (String columnTitle : seriesId) {
							Map<String, Map<String, String>> stateMap = new HashMap<String, Map<String, String>>(9);
							for (String thisState : states) {
								stateMap.put(thisState, new HashMap<String, String>());
							}
							data.put(columnTitle, stateMap);
						}
					} else if (line[0].equals("Name")) {
						// store unit types
						List<String> unitTypes = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							unitTypes.add(line[columnsToImport[i]]);
						}
						units.put(catalogueName, unitTypes);
						header = false;
					}
				} else {
					// check if this is a data row
					if (line[0].isEmpty()) {
						// sub-total row, so do nothing
					} else if (line[1].isEmpty()) {
						footer = true; // first row of footer, so terminate loop and close file
					} else {
						// column title, state, industry code
						// load data
						for (int i = 0; i < columnsToImport.length; i++) {
							String thisState = this.convertStateNameToAcronym(line[0]);
							data.get(seriesId[i]).get(thisState).put(line[1], line[columnsToImport[i]]);
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

	/**
	 * Loads ABS 8165.0 LGA-based data.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param catalogueName        - the name used to store this series' data in the
	 *                             maps
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param titleRow             - the zero-based row number of the series titles.
	 * @param unitsRow             - the zero-based rown number of the unit types.
	 * @param titles               - column titles in CSV file
	 * @param units                - unit type (e.g. $Billions, Number, '000)
	 * @param data                 - the data map that the values are returned in.
	 *                             Keys are: Employment or Turnover Range, State
	 *                             acronym, LGA code, Industry division code.
	 */
	private void loadAbsDataCsv_8165_0Lga(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, Map<String, String>>>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String prevLgaCode = "";
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("State")) {
						// store title
						List<String> titlesInFile = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							titlesInFile.add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];
						}
						titles.put(catalogueName, titlesInFile);

						// create title maps for each year
						String[] states = { "NSW", "VIC", "QLD", "SA", "WA", "TAS", "NT", "ACT", "Other", "AU" };
						for (String columnTitle : seriesId) {
							Map<String, Map<String, Map<String, String>>> stateMap = new HashMap<String, Map<String, Map<String, String>>>(
									states.length);
							for (String thisState : states) {
								stateMap.put(thisState, new HashMap<String, Map<String, String>>());
							}
							data.put(columnTitle, stateMap);
						}
					} else if (line[0].equals("Name")) {
						// store unit types
						List<String> unitTypes = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							unitTypes.add(line[columnsToImport[i]]);
						}
						units.put(catalogueName, unitTypes);
						header = false;
					}
				} else {
					// check if this is a data row
					if (line[0].isEmpty()) {
						footer = true; // first row of footer, so terminate loop and close file
					} else if (line[3].isEmpty()) {
						if (line[1].isEmpty()) {
							// state or country total
							String thisState = this.convertStateNameToAcronym(line[0].substring(6));
							for (int i = 0; i < columnsToImport.length; i++) {
								Map<String, String> stateTotalMap = new HashMap<String, String>(1);
								stateTotalMap.put("All", line[columnsToImport[i]]);
								data.get(seriesId[i]).get(thisState).put("Total", stateTotalMap);
							}
						} else {
							// LGA sub-total row, so do nothing
						}
					} else {
						// column title, state, LGA code, industry division code
						String thisState = this.convertStateNameToAcronym(line[0]);

						// check if it's a new LGA
						if (!line[1].equals(prevLgaCode)) {
							// new LGA, so create industry-value map
							for (int i = 0; i < columnsToImport.length; i++) {
								data.get(seriesId[i]).get(thisState).put(line[1], new HashMap<String, String>());
							}
							prevLgaCode = line[1];
						}

						// load data
						for (int i = 0; i < columnsToImport.length; i++) {
							data.get(seriesId[i]).get(thisState).get(line[1]).put(line[3], line[columnsToImport[i]]);
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

	/**
	 * Loads a set range of rows and columns. First key is the column titles, second
	 * key is the first cell of the rows.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0).
	 * @param rowsToImport         - a zero-based array of integers specifying which
	 *                             rows to import (i.e. the first row is row 0).
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadAbsDataRowsColumnsCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			int[] rowsToImport, int titleRow, Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			int currentRow = 0;
			String[] seriesId = new String[columnsToImport.length];
			titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title & series ID
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];

							// inistialise data
							data.put(seriesId[i], new HashMap<String, String>());
						}
					}
				} else {
					int i = 0;
					rowCheck: while (i < rowsToImport.length) {
						if (currentRow == rowsToImport[i]) {
							// parse the body of the data
							data.get(seriesId[i]).put(line[0], line[columnsToImport[i]]);
							break rowCheck;
						}
						i++;
					}
				}
				currentRow++;
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
	private void loadAbsCensusTableCsv2D(String fileResourceLocation, String tableName, int[] columnsToImport,
			boolean isInitialised, Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			int currentRow = 1;
			int titleRow = 10;
			int lastHeaderRow = 11;
			String[] seriesId = new String[columnsToImport.length];

			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
						}
						if (!isInitialised) {
							titles.put(tableName, new ArrayList<String>(columnsToImport.length));
							for (int i = 0; i < columnsToImport.length; i++) {
								// store title
								titles.get(tableName).add(line[columnsToImport[i]]);

								// store series ID as key with blank collections to populate with data below
								data.put(line[columnsToImport[i]], new HashMap<String, String>());
							}
						}
					} else if (currentRow == lastHeaderRow) {
						header = false;
					}
				} else if (!footer) {
					if (line[0].equals("Total")) {
						footer = true;
					} else {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							String lgaCode = this.area.getLgaCodeFromName(line[0]);
							data.get(seriesId[i]).put(lgaCode, line[columnsToImport[i]]);
							// TODO: modify this so it can use POA, state, etc. not just LGA
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

	/**
	 * 
	 * @param fileResourceLocation  - the URI of the file to import
	 * @param dataSourceName        - the name used to identify this data source (in
	 *                              the shared maps)
	 * @param columnsToImport       - a zero-based array of integers specifying
	 *                              which columns to import (i.e. the first column
	 *                              is column 0). The first column is assumed to be
	 *                              the ADI's ABN and is imported only as the key
	 *                              for the other columns' data.
	 * @param adiCategoriesToImport - a list of ADI categories to import
	 * @param titles                - column titles in CSV file
	 * @param units                 - units in CSV file
	 * @param data                  - the data map that the values are returned in
	 */
	private void loadAdiDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			String[] adiCategoriesToImport, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			final int titleRow = 6;
			final int unitTypeRow = 7;
			int currentRow = 1;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title & series ID
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];
						}
					} else if (currentRow == unitTypeRow) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
						header = false;
					}
					currentRow++;
				} else {
					int i = 0;
					adiCat: while (i < adiCategoriesToImport.length) {
						if (line[2].equals(adiCategoriesToImport[i])) {
							data.put(line[0], new HashMap<String, String>());
							for (int j = 0; j < columnsToImport.length; j++) {
								// parse the body of the data
								data.get(line[0]).put(seriesId[j], line[columnsToImport[j]]);
							}
							break adiCat;
						}
						i++;
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
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             currency's ISO-4217 code and is imported only as
	 *                             the key for the other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadCurrencyDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			final int titleRow = 2;
			int currentRow = 1;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title & series ID
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];
						}
						header = false;
					}
					currentRow++;
				} else {
					if (line[65].equals("Y")) { // FX data exists, so import this row
						data.put(line[0], new HashMap<String, String>());
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(line[0]).put(seriesId[i], line[columnsToImport[i]]);
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

	/**
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param columnsToImport      - a zero-based array of integers specifying which
	 *                             columns to import (i.e. the first column is
	 *                             column 0). The first column is assumed to be the
	 *                             currency's ISO-4217 code and is imported only as
	 *                             the key for the other columns' data.
	 * @param titles               - column titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 */
	private void loadCountryDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			final int titleRow = 4;
			int currentRow = 1;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				if (header) {
					if (currentRow == titleRow) {
						// store title & series ID
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
							seriesId[i] = line[columnsToImport[i]];
						}
						header = false;
					}
					currentRow++;
				} else {
					if (line[2].equals("Y")) { // FX data exists, so import this row
						data.put(line[0], new HashMap<String, String>());
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(line[0]).put(seriesId[i], line[columnsToImport[i]]);
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

	/**
	 * Loads a single year's values from a pre-formatted CSV file containing a
	 * Balance Sheet or Profit and Loss Statement.
	 * 
	 * @param fileResourceLocation - the URI of the file to import
	 * @param dataSourceName       - the name used to identify this data source (in
	 *                             the shared maps)
	 * @param columnToImport       - a zero-based integer specifying which column to
	 *                             import (i.e. the first column is column 0). The
	 *                             first column is assumed to be the field's name
	 *                             and is imported only as the key for the other
	 *                             column's data.
	 * @param rowsToImport         - a zero-based array of integers specifying which
	 *                             rows to import
	 * @param titles               - row titles in CSV file
	 * @param data                 - the data map that the values are returned in
	 * @param multiplier           - the amounts in the file are multiplied by this
	 *                             number. The intention is to scale them from $m to
	 *                             $.
	 */
	private void loadFinancialStatementCsv(String fileResourceLocation, String dataSourceName, int columnToImport,
			int[] rowsToImport, Map<String, List<String>> titles, Map<String, Double> data, double multiplier) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			int currentRow = 0;
			String[] seriesId = new String[rowsToImport.length];
			titles.put(dataSourceName, new ArrayList<String>(rowsToImport.length));
			String[] line = null;
			while ((line = reader.readNext()) != null) {
				int i = 0;
				rowCheck: while (i < rowsToImport.length) {
					if (currentRow == rowsToImport[i]) {
						// store title & series ID
						titles.get(dataSourceName).add(line[columnToImport]);
						seriesId[i] = line[columnToImport];

						// parse the body of the data
						data.put(seriesId[i],
								Double.valueOf(line[columnToImport].replaceAll(",", "").replaceAll("$", ""))
										* multiplier);

						break rowCheck;
					}
					i++;
				}
				currentRow++;
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
	 * Converts State text to state acronym. e.g. "New South Wales" becomes "NSW".
	 */
	public String convertStateNameToAcronym(String stateName) {
		String thisState = "";
		switch (stateName) {
		case "New South Wales":
			thisState = "NSW";
			break;
		case "Victoria":
			thisState = "VIC";
			break;
		case "Queensland":
			thisState = "QLD";
			break;
		case "South Australia":
			thisState = "SA";
			break;
		case "Western Australia":
			thisState = "WA";
			break;
		case "Tasmania":
			thisState = "TAS";
			break;
		case "Northern Territory":
			thisState = "NT";
			break;
		case "Australian Capital Territory":
			thisState = "ACT";
			break;
		case "Australia":
			thisState = "AU";
			break;
		default:
			thisState = "Other";
		}
		return thisState;
	}

	@PostConstruct
	private void init() {
		this.dataLoaded = false;
		this.totalPopulation = null;
		this.adjustedPeopleByLga = null;

		this.title = null;
		this.unitType = null;

		this.rbaE1 = null;
		this.rbaE2 = null;
		this.abs1292_0_55_002ANZSIC = null;
		this.abs1410_0Economy = null;
		this.abs1410_0Family = null;
		this.abs1410_0Income = null;
		this.abs3222_0 = null;
		this.abs5368_0Table14a = null;
		this.abs5368_0Table14b = null;
		this.abs5368_0Table36a = null;
		this.abs5368_0Table36b = null;
		this.abs5368_0Table36c = null;
		this.abs5368_0Table36d = null;
		this.abs5368_0Table36e = null;
		this.abs5368_0Table36f = null;
		this.abs5368_0Table36g = null;
		this.abs5368_0Table36h = null;
		this.abs5368_0Table37a = null;
		this.abs5368_0Table37b = null;
		this.abs5368_0Table37c = null;
		this.abs5368_0Table37d = null;
		this.abs5368_0Table37e = null;
		this.abs5368_0Table37f = null;
		this.abs5368_0Table37g = null;
		this.abs5368_0Table37h = null;
		this.abs5368_0Exporters = null;
		this.abs5676_0Table7 = null;
		this.abs5676_0Table19 = null;
		this.abs5676_0Table21 = null;
		this.abs5676_0Table22 = null;
		this.abs6524_055_002EmployeeTable5 = null;
		this.abs6524_055_002InvestmentTable5 = null;
		this.abs6524_055_002IncomeTable5 = null;
		this.abs8155_0Table2 = null;
		this.abs8155_0Table4 = null;
		this.abs8155_0Table5 = null;
		this.abs8155_0Table6 = null;
		this.abs8165_0StateEmployment = null;
		this.abs8165_0StateTurnover = null;
		this.abs8165_0LgaEmployment = null;
		this.abs8165_0LgaTurnover = null;
		this.abs8165_0Table4 = null;
		this.abs8165_0Table13 = null;
		this.abs8165_0Table17 = null;
		this.abs8167_0Table3 = null;
		this.abs8167_0Table6 = null;
		this.censusLgaByINCP = null;
		this.censusLgaByMRERD = null;
		this.censusLgaByRNTRD = null;
		this.initialisedCensusLgaByINCP = false;
		this.initialisedCensusLgaByMRERD = false;
		this.initialisedCensusLgaByRNTRD = false;
		this.adiData = null;
		this.currencyData = null;
		this.countryData = null;
		this.rbaBalSht = null;
		this.rbaProfitLoss = null;
		this.govtBalSht = null;
		this.govtProfitLoss = null;
	}

	/**
	 * @return the title
	 */
	public Map<String, List<String>> getTitle() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return title;
	}

	/**
	 * @return the unitType
	 */
	public Map<String, List<String>> getUnitType() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return unitType;
	}

	/**
	 * @return the rbaE1
	 */
	public Map<String, Map<Date, String>> getRbaE1() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaE1;
	}

	/**
	 * @return the rbaE2
	 */
	public Map<String, Map<Date, String>> getRbaE2() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaE2;
	}

	/**
	 * @return the abs1292_0_55_002ANZSIC
	 */
	public Map<String, Map<String, String>> getAbs1292_0_55_002ANZSIC() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1292_0_55_002ANZSIC;
	}

	/**
	 * @return the abs1410_0Economy
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Economy() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Economy;
	}

	/**
	 * @return the abs1410_0Family
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Family() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Family;
	}

	/**
	 * @return the abs1410_0Income
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs1410_0Income() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs1410_0Income;
	}

	/**
	 * @return the abs3222_0
	 */
	public Map<String, Map<Date, String>> getAbs3222_0() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs3222_0;
	}

	/**
	 * @return the abs5368_0Exporters
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs5368_0Exporters() {
		return abs5368_0Exporters;
	}

	/**
	 * @return the abs5368_0Table14a
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table14a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table14a;
	}

	/**
	 * @return the abs5368_0Table14b
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table14b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table14b;
	}

	/**
	 * @return the abs5368_0Table36a
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36a;
	}

	/**
	 * @return the abs5368_0Table36b
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36b;
	}

	/**
	 * @return the abs5368_0Table36c
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36c() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36c;
	}

	/**
	 * @return the abs5368_0Table36d
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36d() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36d;
	}

	/**
	 * @return the abs5368_0Table36e
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36e() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36e;
	}

	/**
	 * @return the abs5368_0Table36f
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36f() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36f;
	}

	/**
	 * @return the abs5368_0Table36g
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36g() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36g;
	}

	/**
	 * @return the abs5368_0Table36h
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table36h() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36h;
	}

	/**
	 * @return the abs5368_0Table37a
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37a;
	}

	/**
	 * @return the abs5368_0Table37b
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37b;
	}

	/**
	 * @return the abs5368_0Table37c
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37c() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37c;
	}

	/**
	 * @return the abs5368_0Table37d
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37d() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37d;
	}

	/**
	 * @return the abs5368_0Table37e
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37e() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37e;
	}

	/**
	 * @return the abs5368_0Table37f
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37f() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37f;
	}

	/**
	 * @return the abs5368_0Table37g
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37g() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37g;
	}

	/**
	 * @return the abs5368_0Table37h
	 */
	public Map<String, Map<Date, String>> getAbs5368_0Table37h() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37h;
	}

	/**
	 * @return the abs5676_0Table7
	 */
	public Map<String, Map<Date, String>> getAbs5676_0Table7() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5676_0Table7;
	}

	/**
	 * @return the abs5676_0Table19
	 */
	public Map<String, Map<Date, String>> getAbs5676_0Table19() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5676_0Table19;
	}

	/**
	 * @return the abs5676_0Table21
	 */
	public Map<String, Map<Date, String>> getAbs5676_0Table21() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5676_0Table21;
	}

	/**
	 * @return the abs5676_0Table22
	 */
	public Map<String, Map<Date, String>> getAbs5676_0Table22() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5676_0Table22;
	}

	/**
	 * @return the abs6524_055_002EmployeeTable5
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs6524_055_002EmployeeTable5() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs6524_055_002EmployeeTable5;
	}

	/**
	 * @return the abs6524_055_002InvestmentTable5
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs6524_055_002InvestmentTable5() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs6524_055_002InvestmentTable5;
	}

	/**
	 * @return the abs6524_055_002IncomeTable5
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs6524_055_002IncomeTable5() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs6524_055_002IncomeTable5;
	}

	/**
	 * @return the abs8155_0Table2
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs8155_0Table2() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8155_0Table2;
	}

	/**
	 * @return the abs8155_0Table4
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs8155_0Table4() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8155_0Table4;
	}

	/**
	 * @return the abs8155_0Table5
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8155_0Table5() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8155_0Table5;
	}

	/**
	 * @return the abs8155_0Table6
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8155_0Table6() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8155_0Table6;
	}

	/**
	 * @return the abs8165_0StateEmployment
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs8165_0StateEmployment() {
		return abs8165_0StateEmployment;
	}

	/**
	 * @return the abs8165_0StateTurnover
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs8165_0StateTurnover() {
		return abs8165_0StateTurnover;
	}

	/**
	 * @return the abs8165_0LgaEmployment
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8165_0LgaEmployment() {
		return abs8165_0LgaEmployment;
	}

	/**
	 * @return the abs8165_0LgaTurnover
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8165_0LgaTurnover() {
		return abs8165_0LgaTurnover;
	}

	/**
	 * @return the abs8165_0Table4
	 */
	public Map<String, Map<String, String>> getAbs8165_0Table4() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0Table4;
	}

	/**
	 * @return the abs8165_0Table13
	 */
	public Map<String, Map<String, String>> getAbs8165_0Table13() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0Table13;
	}

	/**
	 * @return the abs8165_0Table17
	 */
	public Map<String, Map<String, String>> getAbs8165_0Table17() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0Table17;
	}

	/**
	 * @return the abs8167_0Table3
	 */
	public Map<String, Map<String, String>> getAbs8167_0Table3() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8167_0Table3;
	}

	/**
	 * @return the abs8167_0Table6
	 */
	public Map<String, Map<String, String>> getAbs8167_0Table6() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8167_0Table6;
	}

	/**
	 * @return the censusLgaByINCP (title, LGA code, value)
	 */
	public Map<String, Map<String, String>> getCensusLgaByINCP() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusLgaByINCP;
	}

	/**
	 * @return the censusLgaByMRERD (title, LGA code, value)
	 */
	public Map<String, Map<String, String>> getCensusLgaByMRERD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusLgaByMRERD;
	}

	/**
	 * @return the censusLgaByRNTRD (title, LGA code, value)
	 */
	public Map<String, Map<String, String>> getCensusLgaByRNTRD() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return censusLgaByRNTRD;
	}

	/**
	 * 
	 * @return ADI data (ADI ABN, field name, value)
	 */
	public Map<String, Map<String, String>> getAdiData() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return adiData;
	}

	/**
	 * 
	 * @return currency data (ISO code, field name, value)
	 */
	public Map<String, Map<String, String>> getCurrencyData() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return currencyData;
	}

	/**
	 * 
	 * @return country data (country name, field name, value)
	 */
	public Map<String, Map<String, String>> getCountryData() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return countryData;
	}

	/**
	 * @return the rbaBalSht
	 */
	public Map<String, Double> getRbaBalSht() {
		return rbaBalSht;
	}

	/**
	 * @return the rbaProfitLoss
	 */
	public Map<String, Double> getRbaProfitLoss() {
		return rbaProfitLoss;
	}

	/**
	 * @return the govtBalSht
	 */
	public Map<String, Double> getGovtBalSht() {
		return govtBalSht;
	}

	/**
	 * @return the govtProfitLoss
	 */
	public Map<String, Double> getGovtProfitLoss() {
		return govtProfitLoss;
	}

	/**
	 * @return the areaMapping
	 */
	public AreaMapping getAreaMapping() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return area;
	}

	/**
	 * @param area the area to set
	 */
	@Autowired
	public void setArea(AreaMapping area) {
		this.area = area;
	}

}
