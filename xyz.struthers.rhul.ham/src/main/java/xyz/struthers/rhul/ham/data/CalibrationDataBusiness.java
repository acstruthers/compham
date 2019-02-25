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
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;
import com.opencsv.CSVReader;

/**
 * Loads CSV data downloaded using Table Builder from the ABS, RBA, APRA and
 * ATO. This class contains the data that is only needed when calibrating
 * businesses.
 * 
 * @author Adam Struthers
 * @since 2019-02-24
 */
@Component
@Scope(value = "singleton")
public class CalibrationDataBusiness {

	// series names
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

	public static final String ATO_COMPANY_T4A = "ATO_CompanyTable4A";
	public static final String ATO_COMPANY_T4B = "ATO_CompanyTable4B";

	// beans
	private AreaMapping area;
	private CalibrationData sharedData;

	// shared data from beans
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC; // ANZSIC industry code mapping

	// data
	private boolean dataLoaded;
	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;

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

	private Map<String, Map<String, String>> atoCompanyTable4a; // ATO Fine Industry Detailed P&L and Bal Sht
	private Map<String, Map<String, String>> atoCompanyTable4b; // Industry Code Total P&L

	/**
	 * 
	 */
	public CalibrationDataBusiness() {
		super();
		this.init();
	}

	@PostConstruct
	private void init() {
		this.dataLoaded = false;

		this.title = null;
		this.unitType = null;

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

		this.atoCompanyTable4a = null;
		this.atoCompanyTable4b = null;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Does a deep delete, but only of data that is not shared outside this class.
	 */
	@PreDestroy
	public void close() {
		if (this.dataLoaded) {
			Set<String> keys = this.title.keySet();
			for (String key : keys) {
				this.title.get(key).clear();
				this.title.put(key, null);
			}
			keys = this.unitType.keySet();
			for (String key : keys) {
				this.unitType.get(key).clear();
				this.unitType.put(key, null);
			}

			keys = this.abs5676_0Table7.keySet();
			for (String key : keys) {
				this.abs5676_0Table7.get(key).clear();
				this.abs5676_0Table7.put(key, null);
			}
			keys = this.abs5676_0Table19.keySet();
			for (String key : keys) {
				this.abs5676_0Table19.get(key).clear();
				this.abs5676_0Table19.put(key, null);
			}
			keys = this.abs5676_0Table21.keySet();
			for (String key : keys) {
				this.abs5676_0Table21.get(key).clear();
				this.abs5676_0Table21.put(key, null);
			}
			keys = this.abs5676_0Table22.keySet();
			for (String key : keys) {
				this.abs5676_0Table22.get(key).clear();
				this.abs5676_0Table22.put(key, null);
			}
			keys = this.abs6524_055_002EmployeeTable5.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs6524_055_002EmployeeTable5.get(key).keySet();
				for (String key2 : keys2) {
					this.abs6524_055_002EmployeeTable5.get(key).get(key2).clear();
					this.abs6524_055_002EmployeeTable5.get(key).put(key2, null);
				}
				this.abs6524_055_002EmployeeTable5.get(key).clear();
				this.abs6524_055_002EmployeeTable5.put(key, null);
			}
			keys = this.abs6524_055_002InvestmentTable5.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs6524_055_002InvestmentTable5.get(key).keySet();
				for (String key2 : keys2) {
					this.abs6524_055_002InvestmentTable5.get(key).get(key2).clear();
					this.abs6524_055_002InvestmentTable5.get(key).put(key2, null);
				}
				this.abs6524_055_002InvestmentTable5.get(key).clear();
				this.abs6524_055_002InvestmentTable5.put(key, null);
			}
			keys = this.abs6524_055_002IncomeTable5.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs6524_055_002IncomeTable5.get(key).keySet();
				for (String key2 : keys2) {
					this.abs6524_055_002IncomeTable5.get(key).get(key2).clear();
					this.abs6524_055_002IncomeTable5.get(key).put(key2, null);
				}
				this.abs6524_055_002IncomeTable5.get(key).clear();
				this.abs6524_055_002IncomeTable5.put(key, null);
			}
			keys = this.abs8155_0Table2.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8155_0Table2.get(key).keySet();
				for (String key2 : keys2) {
					this.abs8155_0Table2.get(key).get(key2).clear();
					this.abs8155_0Table2.get(key).put(key2, null);
				}
				this.abs8155_0Table2.get(key).clear();
				this.abs8155_0Table2.put(key, null);
			}
			keys = this.abs8155_0Table4.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8155_0Table4.get(key).keySet();
				for (String key2 : keys2) {
					this.abs8155_0Table4.get(key).get(key2).clear();
					this.abs8155_0Table4.get(key).put(key2, null);
				}
				this.abs8155_0Table4.get(key).clear();
				this.abs8155_0Table4.put(key, null);
			}
			keys = this.abs8155_0Table5.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8155_0Table5.get(key).keySet();
				for (String key2 : keys2) {
					Set<String> keys3 = this.abs8155_0Table5.get(key).get(key2).keySet();
					for (String key3 : keys3) {
						this.abs8155_0Table5.get(key).get(key2).get(key3).clear();
						this.abs8155_0Table5.get(key).get(key2).put(key3, null);
					}
					this.abs8155_0Table5.get(key).get(key2).clear();
					this.abs8155_0Table5.get(key).put(key2, null);
				}
				this.abs8155_0Table5.get(key).clear();
				this.abs8155_0Table5.put(key, null);
			}
			keys = this.abs8155_0Table6.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8155_0Table6.get(key).keySet();
				for (String key2 : keys2) {
					Set<String> keys3 = this.abs8155_0Table6.get(key).get(key2).keySet();
					for (String key3 : keys3) {
						this.abs8155_0Table6.get(key).get(key2).get(key3).clear();
						this.abs8155_0Table6.get(key).get(key2).put(key3, null);
					}
					this.abs8155_0Table6.get(key).get(key2).clear();
					this.abs8155_0Table6.get(key).put(key2, null);
				}
				this.abs8155_0Table6.get(key).clear();
				this.abs8155_0Table6.put(key, null);
			}
			keys = this.abs8165_0StateEmployment.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8165_0StateEmployment.get(key).keySet();
				for (String key2 : keys2) {
					this.abs8165_0StateEmployment.get(key).get(key2).clear();
					this.abs8165_0StateEmployment.get(key).put(key2, null);
				}
				this.abs8165_0StateEmployment.get(key).clear();
				this.abs8165_0StateEmployment.put(key, null);
			}
			keys = this.abs8165_0StateTurnover.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8165_0StateTurnover.get(key).keySet();
				for (String key2 : keys2) {
					this.abs8165_0StateTurnover.get(key).get(key2).clear();
					this.abs8165_0StateTurnover.get(key).put(key2, null);
				}
				this.abs8165_0StateTurnover.get(key).clear();
				this.abs8165_0StateTurnover.put(key, null);
			}
			keys = this.abs8165_0LgaEmployment.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8165_0LgaEmployment.get(key).keySet();
				for (String key2 : keys2) {
					Set<String> keys3 = this.abs8165_0LgaEmployment.get(key).get(key2).keySet();
					for (String key3 : keys3) {
						this.abs8165_0LgaEmployment.get(key).get(key2).get(key3).clear();
						this.abs8165_0LgaEmployment.get(key).get(key2).put(key3, null);
					}
					this.abs8165_0LgaEmployment.get(key).get(key2).clear();
					this.abs8165_0LgaEmployment.get(key).put(key2, null);
				}
				this.abs8165_0LgaEmployment.get(key).clear();
				this.abs8165_0LgaEmployment.put(key, null);
			}
			keys = this.abs8165_0LgaTurnover.keySet();
			for (String key : keys) {
				Set<String> keys2 = this.abs8165_0LgaTurnover.get(key).keySet();
				for (String key2 : keys2) {
					Set<String> keys3 = this.abs8165_0LgaTurnover.get(key).get(key2).keySet();
					for (String key3 : keys3) {
						this.abs8165_0LgaTurnover.get(key).get(key2).get(key3).clear();
						this.abs8165_0LgaTurnover.get(key).get(key2).put(key3, null);
					}
					this.abs8165_0LgaTurnover.get(key).get(key2).clear();
					this.abs8165_0LgaTurnover.get(key).put(key2, null);
				}
				this.abs8165_0LgaTurnover.get(key).clear();
				this.abs8165_0LgaTurnover.put(key, null);
			}
			keys = this.abs8165_0Table4.keySet();
			for (String key : keys) {
				this.abs8165_0Table4.get(key).clear();
				this.abs8165_0Table4.put(key, null);
			}
			keys = this.abs8165_0Table13.keySet();
			for (String key : keys) {
				this.abs8165_0Table13.get(key).clear();
				this.abs8165_0Table13.put(key, null);
			}
			keys = this.abs8165_0Table17.keySet();
			for (String key : keys) {
				this.abs8165_0Table17.get(key).clear();
				this.abs8165_0Table17.put(key, null);
			}
			keys = this.atoCompanyTable4a.keySet();
			for (String key : keys) {
				this.atoCompanyTable4a.get(key).clear();
				this.atoCompanyTable4a.put(key, null);
			}
			keys = this.atoCompanyTable4b.keySet();
			for (String key : keys) {
				this.atoCompanyTable4b.get(key).clear();
				this.atoCompanyTable4b.put(key, null);
			}
		}
		this.init(); // set all the pointers to null and dataLoaded to false
	}

	private void loadData() {
		// get shared data that this method relies on
		this.abs1292_0_55_002ANZSIC = this.sharedData.getAbs1292_0_55_002ANZSIC();

		// initialise field variables
		this.title = new HashMap<String, List<String>>();
		this.unitType = new HashMap<String, List<String>>();

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
		this.loadAbsDataCsv_Catalogue("/data/ABS/5676.0_BusinessIndicators/Table22_ProfitsVsSalesRatio.csv",
				ABS_5676_0_T22, abs5676_0Table22Columns, this.title, this.unitType, this.abs5676_0Table22);

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
		this.loadAbsDataCsv_8155_0T2T4("/data/ABS/8155.0_IndustryByDivision/Table4_IndustryPerformance.csv",
				ABS8155_0_T4, abs8155_0Table4Columns, abs8155_0Table4Years, abs8155_0Table4TitleRow,
				abs8155_0Table4UnitsRow, this.title, this.unitType, this.abs8155_0Table4);

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
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8165.0_CountOfBusinesses/8165.0_Table13_EmploymentSize.csv",
				ABS8165_0_T13, abs8165_0Table13Columns, abs8165_0Table13Rows, abs8165_0Table13TitleRow, this.title,
				this.abs8165_0Table13);

		System.out.println(", Table 17.");
		this.abs8165_0Table17 = new HashMap<String, Map<String, String>>(1);
		int[] abs8165_0Table17Columns = { 5 };
		int[] abs8165_0Table17Rows = { 31, 32, 33, 34, 35, 36 };
		int abs8165_0Table17TitleRow = 4;
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8165.0_CountOfBusinesses/8165.0_Table17_Turnover.csv", ABS8165_0_T17,
				abs8165_0Table17Columns, abs8165_0Table17Rows, abs8165_0Table17TitleRow, this.title,
				this.abs8165_0Table17);

		// load ATO Company Table 4
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Company Table 4A data");
		this.atoCompanyTable4a = new HashMap<String, Map<String, String>>();
		int[] atoCompanyTable4aColumns = { 2, 3, 4, 9, 10, 11, 12, 17, 18, 21, 22, 23, 24, 31, 32, 33, 34, 35, 36, 37,
				38, 41, 42, 51, 52, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 111, 112, 115, 116 };
		this.loadAtoCompanyTable4A("/data/ATO/Company/CompanyTable4A.csv", ATO_COMPANY_T4A, atoCompanyTable4aColumns,
				this.title, this.atoCompanyTable4a);

		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ATO Company Table 4B data");
		this.atoCompanyTable4b = new HashMap<String, Map<String, String>>();
		int[] atoCompanyTable4bColumns = { 2, 3, 4, 5, 6 };
		this.loadAtoCompanyTable4B("/data/ATO/Company/CompanyTable4B.csv", ATO_COMPANY_T4B, atoCompanyTable4bColumns,
				this.title, this.atoCompanyTable4b);

		// set flag so we only load the data once
		System.out.println(new Date(System.currentTimeMillis()) + ": Business data loaded");
		this.dataLoaded = true;
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
	 * Loads ABS 6524.0.55.002 catalogue data.
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
						// CHECKME: store industry code not description
						industry = abs1292_0_55_002ANZSIC.get("Division to Division Code").get(line[0].toUpperCase());
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
	 *                             Industry Division Code.
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
						// change size or state category from description to code
						isNewCategory = true;
						switch (line[0].toUpperCase()) {
						case "SMALL":
							sizeOrState = "S";
							break;
						case "MEDIUM":
							sizeOrState = "M";
							break;
						case "LARGE":
							sizeOrState = "L";
							break;
						case "NEW SOUTH WALES":
							sizeOrState = "NSW";
							break;
						case "VICTORIA":
							sizeOrState = "VIC";
							break;
						case "QUEENSLAND":
							sizeOrState = "QLD";
							break;
						case "SOUTH AUSTRALIA":
							sizeOrState = "SA";
							break;
						case "WESTERN AUSTRALIA":
							sizeOrState = "WA";
							break;
						case "TASMANIA":
							sizeOrState = "TAS";
							break;
						case "NORTHERN TERRITORY":
							sizeOrState = "NT";
							break;
						case "AUSTRALIAN CAPITAL TERRITORY":
							sizeOrState = "ACT";
							break;
						case "AUSTRALIA":
						case "TOTAL":
							sizeOrState = "AU";
							break;
						default:
							sizeOrState = "Other";
						}
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
							// CHECKME: store industry code not description
							String anzsicCode = abs1292_0_55_002ANZSIC.get("Division to Division Code")
									.get(line[0].toUpperCase());
							data.get(years[i]).get(seriesId[i]).get(sizeOrState).put(anzsicCode,
									line[columnsToImport[i]]);
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
	 * Load data from ATO Company Table 4A
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
	private void loadAtoCompanyTable4A(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

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
					if (line[0].equals("Broad Industry2")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]], new HashMap<String, String>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[1].equals("Other")) {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							String fineIndustryCode = line[1].substring(0, 3);
							data.get(seriesId[i]).put(fineIndustryCode, line[columnsToImport[i]]);
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
	 * Load data from ATO Company Table 4B
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
	private void loadAtoCompanyTable4B(String fileResourceLocation, String tableName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, Map<String, String>> data) {

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
					if (line[0].equals("Industry code")) {
						// title row
						List<String> thesecolumnNames = new ArrayList<String>(columnsToImport.length);
						for (int i = 0; i < columnsToImport.length; i++) {
							// store title
							seriesId[i] = line[columnsToImport[i]];
							thesecolumnNames.add(line[columnsToImport[i]]);

							// store series ID as key with blank collections to populate with data below
							data.put(line[columnsToImport[i]], new HashMap<String, String>());
						}
						titles.put(tableName, thesecolumnNames);
						header = false;
					}
				} else {
					if (!line[0].equals("Other")) {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							data.get(seriesId[i]).put(line[0], line[columnsToImport[i]]);
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
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0StateEmployment;
	}

	/**
	 * @return the abs8165_0StateTurnover
	 */
	public Map<String, Map<String, Map<String, String>>> getAbs8165_0StateTurnover() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0StateTurnover;
	}

	/**
	 * @return the abs8165_0LgaEmployment
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8165_0LgaEmployment() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8165_0LgaEmployment;
	}

	/**
	 * @return the abs8165_0LgaTurnover
	 */
	public Map<String, Map<String, Map<String, Map<String, String>>>> getAbs8165_0LgaTurnover() {
		if (!this.dataLoaded) {
			this.loadData();
		}
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
	 * @return the atoCompanyTable4a
	 */
	public Map<String, Map<String, String>> getAtoCompanyTable4a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoCompanyTable4a;
	}

	/**
	 * @return the atoCompanyTable4b
	 */
	public Map<String, Map<String, String>> getAtoCompanyTable4b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return atoCompanyTable4b;
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
