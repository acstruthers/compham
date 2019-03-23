/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVReader;

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
	public static final float BILLION = 1000000000f;
	public static final float MILLION = 1000000f;
	public static final float THOUSAND = 1000f;
	public static final float PERCENT = 0.01f;

	// map implementation optimisation
	public static final float MAP_LOAD_FACTOR = 0.75f;
	public static final int NUM_ADIS = 86;
	public static final int NUM_CURRENCIES = 51;
	public static final int NUM_COUNTRIES = 111;
	public static final int NUM_DATA_SERIES = 30 + 1; // 30 rounds to an integer, so add 1

	// series names
	public static final String RBA_E1 = "RBA_E1"; // household & business Balance Sheet

	public static final String ABS1292_0_55_002_ANZSIC = "ABS_1292.0.55.002_ANZSIC";
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
	public static final String ABS8167_0_T3 = "ABS_8167.0_Table3";
	public static final String ABS8167_0_T6 = "ABS_8167.0_Table3";

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

	private Map<String, List<String>> title;
	private Map<String, List<String>> unitType;
	private Map<String, Map<Date, Float>> rbaE1; // AU Bal Sht totals
	private Map<String, Map<String, String>> abs1292_0_55_002ANZSIC; // ANZSIC industry code mapping
	// private Map<String, Map<Date, String>> abs3222_0; // AU by gender and age
	// (population projections)
	private Map<String, Map<Date, Float>> abs5368_0Table14a; // exports by country
	private Map<String, Map<Date, Float>> abs5368_0Table14b; // imports by country
	private Map<String, Map<Date, Float>> abs5368_0Table36a; // exports NSW
	private Map<String, Map<Date, Float>> abs5368_0Table36b; // exports VIC
	private Map<String, Map<Date, Float>> abs5368_0Table36c; // exports QLD
	private Map<String, Map<Date, Float>> abs5368_0Table36d; // exports SA
	private Map<String, Map<Date, Float>> abs5368_0Table36e; // exports WA
	private Map<String, Map<Date, Float>> abs5368_0Table36f; // exports TAS
	private Map<String, Map<Date, Float>> abs5368_0Table36g; // exports NT
	private Map<String, Map<Date, Float>> abs5368_0Table36h; // exports ACT
	private Map<String, Map<Date, Float>> abs5368_0Table37a; // imports NSW
	private Map<String, Map<Date, Float>> abs5368_0Table37b; // imports VIC
	private Map<String, Map<Date, Float>> abs5368_0Table37c; // imports QLD
	private Map<String, Map<Date, Float>> abs5368_0Table37d; // imports SA
	private Map<String, Map<Date, Float>> abs5368_0Table37e; // imports WA
	private Map<String, Map<Date, Float>> abs5368_0Table37f; // imports TAS
	private Map<String, Map<Date, Float>> abs5368_0Table37g; // imports NT
	private Map<String, Map<Date, Float>> abs5368_0Table37h; // imports ACT
	private Map<String, Map<String, Map<String, Map<String, Float>>>> abs5368_0Exporters; // formatted export data
																							// (keys: industry, state,
																							// country, value range)
	private Map<String, Map<String, Float>> abs8167_0Table3; // main source of income
	private Map<String, Map<String, Float>> abs8167_0Table6; // main supplier

	private Map<String, Map<String, String>> adiData; // banks, building societies & credit unions
	private Map<String, Map<String, String>> currencyData; // (ISO code, field name, value)
	private Map<String, Map<String, String>> countryData; // (Country name, field name, value)
	private Map<String, Float> rbaBalSht;
	private Map<String, Float> rbaProfitLoss;
	private Map<String, Float> govtBalSht;
	private Map<String, Float> govtProfitLoss;

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
	 * Deletes all the field variables, freeing up memory.
	 * 
	 * Does not do a deep delete because most objects are passed to other classes by
	 * reference, and the other classes will probably still need to refer to them.
	 */
	@PreDestroy
	public void close() {
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

		// just make these null because the classes they came from will do a deep delete
		// at an appropriate time
		this.area = null;

		for (String key : this.title.keySet()) {
			this.title.get(key).clear();
			this.title.put(key, null);
		}
		this.title = null;
		for (String key : this.unitType.keySet()) {
			this.unitType.get(key).clear();
			this.unitType.put(key, null);
		}
		this.unitType = null;

		this.dropRbaE1Data();

		this.dropAnzsicData();

		this.dropAbs5368_0Data();
		this.dropAbs5368_0ExportersData();

		this.dropAbs8167_0Data();

		this.dropAdiFinancialData();
		this.dropRbaFinancialData();
		this.dropGovtFinancialData();

		this.dropCurrencyData();
		this.dropCountryData();

		this.dataLoaded = false;

		// invoke garbage collector
		System.gc();

		// report how much RAM was released
		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesConsumed = (memoryAfter - memoryBefore) / 1024f / 1024f;
		DecimalFormat decimalFormatter = new DecimalFormat("#,##0.00");
		System.out.println(">>> Memory released after clearing common calibration data: "
				+ decimalFormatter.format(megabytesConsumed) + "MB");
		System.out.println(
				">>> Current memory consumption: " + decimalFormatter.format(memoryAfter / 1024f / 1024f) + "MB");
	}

	public void dropRbaE1Data() {
		if (this.rbaE1 != null) {
			// RBA E1
			for (String key1 : this.rbaE1.keySet()) {
				for (Date key2 : this.rbaE1.get(key1).keySet()) {
					this.rbaE1.get(key1).put(key2, null);
				}
				this.rbaE1.get(key1).clear();
				this.rbaE1.put(key1, null);
			}
			this.rbaE1.clear();
			this.rbaE1 = null;
		}
	}

	public void dropAnzsicData() {
		if (this.abs1292_0_55_002ANZSIC != null) {
			// ANZSIC
			for (String key1 : this.abs1292_0_55_002ANZSIC.keySet()) {
				for (String key2 : this.abs1292_0_55_002ANZSIC.get(key1).keySet()) {
					this.abs1292_0_55_002ANZSIC.get(key1).put(key2, null);
				}
				this.abs1292_0_55_002ANZSIC.get(key1).clear();
				this.abs1292_0_55_002ANZSIC.put(key1, null);
			}
			this.abs1292_0_55_002ANZSIC.clear();
			this.abs1292_0_55_002ANZSIC = null;
		}
	}

	public void dropAbs5368_0Data() {
		if (this.abs5368_0Table14a != null) {
			// ABS 5368.0 Table 14A
			for (String key1 : this.abs5368_0Table14a.keySet()) {
				for (Date key2 : this.abs5368_0Table14a.get(key1).keySet()) {
					this.abs5368_0Table14a.get(key1).put(key2, null);
				}
				this.abs5368_0Table14a.get(key1).clear();
				this.abs5368_0Table14a.put(key1, null);
			}
			this.abs5368_0Table14a.clear();
			this.abs5368_0Table14a = null;
		}

		if (this.abs5368_0Table14b != null) {
			// ABS 5368.0 Table 14B
			for (String key1 : this.abs5368_0Table14b.keySet()) {
				for (Date key2 : this.abs5368_0Table14b.get(key1).keySet()) {
					this.abs5368_0Table14b.get(key1).put(key2, null);
				}
				this.abs5368_0Table14b.get(key1).clear();
				this.abs5368_0Table14b.put(key1, null);
			}
			this.abs5368_0Table14b.clear();
			this.abs5368_0Table14b = null;
		}

		if (this.abs5368_0Table36a != null) {
			// ABS 5368.0 Table 36A
			for (String key1 : this.abs5368_0Table36a.keySet()) {
				for (Date key2 : this.abs5368_0Table36a.get(key1).keySet()) {
					this.abs5368_0Table36a.get(key1).put(key2, null);
				}
				this.abs5368_0Table36a.get(key1).clear();
				this.abs5368_0Table36a.put(key1, null);
			}
			this.abs5368_0Table36a.clear();
			this.abs5368_0Table36a = null;
		}

		if (this.abs5368_0Table36b != null) {
			// ABS 5368.0 Table 36B
			for (String key1 : this.abs5368_0Table36b.keySet()) {
				for (Date key2 : this.abs5368_0Table36b.get(key1).keySet()) {
					this.abs5368_0Table36b.get(key1).put(key2, null);
				}
				this.abs5368_0Table36b.get(key1).clear();
				this.abs5368_0Table36b.put(key1, null);
			}
			this.abs5368_0Table36b.clear();
			this.abs5368_0Table36b = null;
		}

		if (this.abs5368_0Table36c != null) {
			// ABS 5368.0 Table 36C
			for (String key1 : this.abs5368_0Table36c.keySet()) {
				for (Date key2 : this.abs5368_0Table36c.get(key1).keySet()) {
					this.abs5368_0Table36c.get(key1).put(key2, null);
				}
				this.abs5368_0Table36c.get(key1).clear();
				this.abs5368_0Table36c.put(key1, null);
			}
			this.abs5368_0Table36c.clear();
			this.abs5368_0Table36c = null;
		}

		if (this.abs5368_0Table36d != null) {
			// ABS 5368.0 Table 36D
			for (String key1 : this.abs5368_0Table36d.keySet()) {
				for (Date key2 : this.abs5368_0Table36d.get(key1).keySet()) {
					this.abs5368_0Table36d.get(key1).put(key2, null);
				}
				this.abs5368_0Table36d.get(key1).clear();
				this.abs5368_0Table36d.put(key1, null);
			}
			this.abs5368_0Table36d.clear();
			this.abs5368_0Table36d = null;
		}

		if (this.abs5368_0Table36e != null) {
			// ABS 5368.0 Table 36E
			for (String key1 : this.abs5368_0Table36e.keySet()) {
				for (Date key2 : this.abs5368_0Table36e.get(key1).keySet()) {
					this.abs5368_0Table36e.get(key1).put(key2, null);
				}
				this.abs5368_0Table36e.get(key1).clear();
				this.abs5368_0Table36e.put(key1, null);
			}
			this.abs5368_0Table36e.clear();
			this.abs5368_0Table36e = null;
		}

		if (this.abs5368_0Table36f != null) {
			// ABS 5368.0 Table 36F
			for (String key1 : this.abs5368_0Table36f.keySet()) {
				for (Date key2 : this.abs5368_0Table36f.get(key1).keySet()) {
					this.abs5368_0Table36f.get(key1).put(key2, null);
				}
				this.abs5368_0Table36f.get(key1).clear();
				this.abs5368_0Table36f.put(key1, null);
			}
			this.abs5368_0Table36f.clear();
			this.abs5368_0Table36f = null;
		}

		if (this.abs5368_0Table36g != null) {
			// ABS 5368.0 Table 36G
			for (String key1 : this.abs5368_0Table36g.keySet()) {
				for (Date key2 : this.abs5368_0Table36g.get(key1).keySet()) {
					this.abs5368_0Table36g.get(key1).put(key2, null);
				}
				this.abs5368_0Table36g.get(key1).clear();
				this.abs5368_0Table36g.put(key1, null);
			}
			this.abs5368_0Table36g.clear();
			this.abs5368_0Table36g = null;
		}

		if (this.abs5368_0Table36h != null) {
			// ABS 5368.0 Table 36H
			for (String key1 : this.abs5368_0Table36h.keySet()) {
				for (Date key2 : this.abs5368_0Table36h.get(key1).keySet()) {
					this.abs5368_0Table36h.get(key1).put(key2, null);
				}
				this.abs5368_0Table36h.get(key1).clear();
				this.abs5368_0Table36h.put(key1, null);
			}
			this.abs5368_0Table36h.clear();
			this.abs5368_0Table36h = null;
		}

		if (this.abs5368_0Table37a != null) {
			// ABS 5368.0 Table 37A
			for (String key1 : this.abs5368_0Table37a.keySet()) {
				for (Date key2 : this.abs5368_0Table37a.get(key1).keySet()) {
					this.abs5368_0Table37a.get(key1).put(key2, null);
				}
				this.abs5368_0Table37a.get(key1).clear();
				this.abs5368_0Table37a.put(key1, null);
			}
			this.abs5368_0Table37a.clear();
			this.abs5368_0Table37a = null;
		}

		if (this.abs5368_0Table37b != null) {
			// ABS 5368.0 Table 37B
			for (String key1 : this.abs5368_0Table37b.keySet()) {
				for (Date key2 : this.abs5368_0Table37b.get(key1).keySet()) {
					this.abs5368_0Table37b.get(key1).put(key2, null);
				}
				this.abs5368_0Table37b.get(key1).clear();
				this.abs5368_0Table37b.put(key1, null);
			}
			this.abs5368_0Table37b.clear();
			this.abs5368_0Table37b = null;
		}

		if (this.abs5368_0Table37c != null) {
			// ABS 5368.0 Table 37C
			for (String key1 : this.abs5368_0Table37c.keySet()) {
				for (Date key2 : this.abs5368_0Table37c.get(key1).keySet()) {
					this.abs5368_0Table37c.get(key1).put(key2, null);
				}
				this.abs5368_0Table37c.get(key1).clear();
				this.abs5368_0Table37c.put(key1, null);
			}
			this.abs5368_0Table37c.clear();
			this.abs5368_0Table37c = null;
		}

		if (this.abs5368_0Table37d != null) {
			// ABS 5368.0 Table 37D
			for (String key1 : this.abs5368_0Table37d.keySet()) {
				for (Date key2 : this.abs5368_0Table37d.get(key1).keySet()) {
					this.abs5368_0Table37d.get(key1).put(key2, null);
				}
				this.abs5368_0Table37d.get(key1).clear();
				this.abs5368_0Table37d.put(key1, null);
			}
			this.abs5368_0Table37d.clear();
			this.abs5368_0Table37d = null;
		}

		if (this.abs5368_0Table37e != null) {
			// ABS 5368.0 Table 37E
			for (String key1 : this.abs5368_0Table37e.keySet()) {
				for (Date key2 : this.abs5368_0Table37e.get(key1).keySet()) {
					this.abs5368_0Table37e.get(key1).put(key2, null);
				}
				this.abs5368_0Table37e.get(key1).clear();
				this.abs5368_0Table37e.put(key1, null);
			}
			this.abs5368_0Table37e.clear();
			this.abs5368_0Table37e = null;
		}

		if (this.abs5368_0Table37f != null) {
			// ABS 5368.0 Table 37F
			for (String key1 : this.abs5368_0Table37f.keySet()) {
				for (Date key2 : this.abs5368_0Table37f.get(key1).keySet()) {
					this.abs5368_0Table37f.get(key1).put(key2, null);
				}
				this.abs5368_0Table37f.get(key1).clear();
				this.abs5368_0Table37f.put(key1, null);
			}
			this.abs5368_0Table37f.clear();
			this.abs5368_0Table37f = null;
		}

		if (this.abs5368_0Table37g != null) {
			// ABS 5368.0 Table 37G
			for (String key1 : this.abs5368_0Table37g.keySet()) {
				for (Date key2 : this.abs5368_0Table37g.get(key1).keySet()) {
					this.abs5368_0Table37g.get(key1).put(key2, null);
				}
				this.abs5368_0Table37g.get(key1).clear();
				this.abs5368_0Table37g.put(key1, null);
			}
			this.abs5368_0Table37g.clear();
			this.abs5368_0Table37g = null;
		}

		if (this.abs5368_0Table37h != null) {
			// ABS 5368.0 Table 37H
			for (String key1 : this.abs5368_0Table37h.keySet()) {
				for (Date key2 : this.abs5368_0Table37h.get(key1).keySet()) {
					this.abs5368_0Table37h.get(key1).put(key2, null);
				}
				this.abs5368_0Table37h.get(key1).clear();
				this.abs5368_0Table37h.put(key1, null);
			}
			this.abs5368_0Table37h.clear();
			this.abs5368_0Table37h = null;
		}
	}

	public void dropAbs5368_0ExportersData() {
		if (this.abs5368_0Exporters != null) {
			// ABS 5368 exporters
			for (String key1 : this.abs5368_0Exporters.keySet()) {
				for (String key2 : this.abs5368_0Exporters.get(key1).keySet()) {
					for (String key3 : this.abs5368_0Exporters.get(key1).get(key2).keySet()) {
						for (String key4 : this.abs5368_0Exporters.get(key1).get(key2).get(key3).keySet()) {
							this.abs5368_0Exporters.get(key1).get(key2).get(key3).put(key4, null);
						}
						this.abs5368_0Exporters.get(key1).get(key2).get(key3).clear();
						this.abs5368_0Exporters.get(key1).get(key2).put(key3, null);
					}
					this.abs5368_0Exporters.get(key1).get(key2).clear();
					this.abs5368_0Exporters.get(key1).put(key2, null);
				}
				this.abs5368_0Exporters.get(key1).clear();
				this.abs5368_0Exporters.put(key1, null);
			}
			this.abs5368_0Exporters.clear();
			this.abs5368_0Exporters = null;
		}
	}

	public void dropAbs8167_0Data() {
		if (this.abs8167_0Table3 != null) {
			// ABS 8167.0 Table 3
			for (String key1 : this.abs8167_0Table3.keySet()) {
				for (String key2 : this.abs8167_0Table3.get(key1).keySet()) {
					this.abs8167_0Table3.get(key1).put(key2, null);
				}
				this.abs8167_0Table3.get(key1).clear();
				this.abs8167_0Table3.put(key1, null);
			}
			this.abs8167_0Table3.clear();
			this.abs8167_0Table3 = null;
		}

		if (this.abs8167_0Table6 != null) {
			// ABS 8167.0 Table 6
			for (String key1 : this.abs8167_0Table6.keySet()) {
				for (String key2 : this.abs8167_0Table6.get(key1).keySet()) {
					this.abs8167_0Table6.get(key1).put(key2, null);
				}
				this.abs8167_0Table6.get(key1).clear();
				this.abs8167_0Table6.put(key1, null);
			}
			this.abs8167_0Table6.clear();
			this.abs8167_0Table6 = null;
		}
	}

	public void dropAdiFinancialData() {
		// ADI data
		if (this.adiData != null) {
			for (String key1 : this.adiData.keySet()) {
				for (String key2 : this.adiData.get(key1).keySet()) {
					this.adiData.get(key1).put(key2, null);
				}
				this.adiData.get(key1).clear();
				this.adiData.put(key1, null);
			}
			this.adiData.clear();
			this.adiData = null;
		}
	}

	public void dropRbaFinancialData() {
		// RBA Bal Sht
		if (this.rbaBalSht != null) {
			for (String key1 : this.rbaBalSht.keySet()) {
				this.rbaBalSht.put(key1, null);
			}
			this.rbaBalSht.clear();
			this.rbaBalSht = null;
		}

		if (this.rbaProfitLoss != null) {
			// RBA P&L
			for (String key1 : this.rbaProfitLoss.keySet()) {
				this.rbaProfitLoss.put(key1, null);
			}
			this.rbaProfitLoss.clear();
			this.rbaProfitLoss = null;
		}
	}

	public void dropCurrencyData() {
		if (this.currencyData != null) {
			// Currency data
			for (String key1 : this.currencyData.keySet()) {
				for (String key2 : this.currencyData.get(key1).keySet()) {
					this.currencyData.get(key1).put(key2, null);
				}
				this.currencyData.get(key1).clear();
				this.currencyData.put(key1, null);
			}
			this.currencyData.clear();
			this.currencyData = null;
		}
	}

	public void dropCountryData() {
		if (this.countryData != null) {
			// Country data
			for (String key1 : this.countryData.keySet()) {
				for (String key2 : this.countryData.get(key1).keySet()) {
					this.countryData.get(key1).put(key2, null);
				}
				this.countryData.get(key1).clear();
				this.countryData.put(key1, null);
			}
			this.countryData.clear();
			this.countryData = null;
		}
	}

	public void dropGovtFinancialData() {
		if (this.govtBalSht != null) {
			// Govt Bal Sht
			for (String key1 : this.govtBalSht.keySet()) {
				this.govtBalSht.put(key1, null);
			}
			this.govtBalSht.clear();
			this.govtBalSht = null;
		}

		if (this.govtProfitLoss != null) {
			// Govt P&L
			for (String key1 : this.govtProfitLoss.keySet()) {
				this.govtProfitLoss.put(key1, null);
			}
			this.govtProfitLoss.clear();
			this.govtProfitLoss = null;
		}
	}

	private void loadData() {
		int titleMapCapacity = (int) Math.ceil(NUM_DATA_SERIES / MAP_LOAD_FACTOR);
		this.title = new HashMap<String, List<String>>(titleMapCapacity);
		this.unitType = new HashMap<String, List<String>>(titleMapCapacity);

		// load ABS 1292.0.55.002 ANZSIC mapping table
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 1292.0.55.002 ANZSIC mapping table");
		int abs1292_0_55_002ANZSICMapCapacity = (int) Math.ceil((4 + 4 + 3 + 2 + 1) / MAP_LOAD_FACTOR);
		this.abs1292_0_55_002ANZSIC = new HashMap<String, Map<String, String>>(abs1292_0_55_002ANZSICMapCapacity);
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
		int[] rbaE1Columns = { 1, 3, 4, 5, 6, 7, 9, 10, 11, 14, 15, 16, 17, 18, 20, 23 };
		int rbaE1MapCapacity = (int) Math.ceil(rbaE1Columns.length / MAP_LOAD_FACTOR);
		this.rbaE1 = new HashMap<String, Map<Date, Float>>(rbaE1MapCapacity);
		this.loadRbaDataCsv("/data/RBA/E_HouseholdBusiness/e1-data.csv", RBA_E1, rbaE1Columns, this.title,
				this.unitType, this.rbaE1);

		// load ABS 5368.0 International Trade data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ABS 5368.0 International Trade data");
		System.out.print("   Table 14a");
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
		int abs5368_0MapCapacity = (int) Math.ceil(abs5368_0Columns.length / MAP_LOAD_FACTOR);
		this.abs5368_0Table14a = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368014a - exports by country.csv", ABS_5368_0_T14A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table14a);

		System.out.print(", 14b");
		this.abs5368_0Table14b = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368014b - imports by country.csv", ABS_5368_0_T14B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table14b);

		System.out.print(", 36a");
		this.abs5368_0Table36a = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036a - merch exports NSW.csv", ABS_5368_0_T36A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36a);

		System.out.print(", 36b");
		this.abs5368_0Table36b = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036b - merch exports VIC.csv", ABS_5368_0_T36B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36b);

		System.out.print(", 36c");
		this.abs5368_0Table36c = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036c - merch exports QLD.csv", ABS_5368_0_T36C,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36c);

		System.out.print(", 36d");
		this.abs5368_0Table36d = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036d - merch exports SA.csv", ABS_5368_0_T36D,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36d);

		System.out.print(", 36e");
		this.abs5368_0Table36e = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036e - merch exports WA.csv", ABS_5368_0_T36E,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36e);

		System.out.print(", 36f");
		this.abs5368_0Table36f = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036f - merch exports TAS.csv", ABS_5368_0_T36F,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36f);

		System.out.print(", 36g");
		this.abs5368_0Table36g = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036g - merch exports NT.csv", ABS_5368_0_T36G,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36g);

		System.out.print(", 36h");
		this.abs5368_0Table36h = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368036h - merch exports ACT.csv", ABS_5368_0_T36H,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table36h);

		System.out.print(", 37a");
		this.abs5368_0Table37a = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037a - merch imports NSW.csv", ABS_5368_0_T37A,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37a);

		System.out.print(", 37b");
		this.abs5368_0Table37b = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037b - merch imports VIC.csv", ABS_5368_0_T37B,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37b);

		System.out.print(", 37c");
		this.abs5368_0Table37c = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037c - merch imports QLD.csv", ABS_5368_0_T37C,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37c);

		System.out.print(", 37d");
		this.abs5368_0Table37d = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037d - merch imports SA.csv", ABS_5368_0_T37D,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37d);

		System.out.print(", 37e");
		this.abs5368_0Table37e = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037e - merch imports WA.csv", ABS_5368_0_T37E,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37e);

		System.out.print(", 37f");
		this.abs5368_0Table37f = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037f - merch imports TAS.csv", ABS_5368_0_T37F,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37f);

		System.out.print(", 37g");
		this.abs5368_0Table37g = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037g - merch imports NT.csv", ABS_5368_0_T37G,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37g);

		System.out.println(", 37h");
		this.abs5368_0Table37h = new HashMap<String, Map<Date, Float>>(abs5368_0MapCapacity);
		this.loadAbsDataCsv_Catalogue("/data/ABS/5368.0_IntlTrade/5368037h - merch imports ACT.csv", ABS_5368_0_T37H,
				abs5368_0Columns, this.title, this.unitType, this.abs5368_0Table37h);

		// load ABS 53686.0 exporters data
		System.out.println(
				new Date(System.currentTimeMillis()) + ": Loading ABS 5368.0.55.006 Exporters (formatted data)");
		int[] abs5368_0ExportersColumns = { 4, 5, 6, 7, 8, 9, 10, 11, 13, 14, 15, 16, 17, 18, 19, 20 };
		int abs5368_0ExportersMapCapacity = (int) Math.ceil(abs5368_0ExportersColumns.length / MAP_LOAD_FACTOR);
		this.abs5368_0Exporters = new HashMap<String, Map<String, Map<String, Map<String, Float>>>>(
				abs5368_0ExportersMapCapacity);
		this.loadAbsDataCsv_5368_0Exporters("/data/ABS/5368.0.55.006_Exporters/5368.0_exporter data.csv",
				ABS_5368_0_EXPORTERS, abs5368_0ExportersColumns, this.title, this.unitType, this.abs5368_0Exporters);

		// ABS 8167.0 Business Markets and Competition
		System.out.println(
				new Date(System.currentTimeMillis()) + ": Loading ABS 8167.0 business markets and competition");
		int[] abs8167_0Table3Columns = { 1, 2, 3, 4, 5 };
		int abs8167_0Table3MapCapacity = (int) Math.ceil(abs8167_0Table3Columns.length / MAP_LOAD_FACTOR);
		this.abs8167_0Table3 = new HashMap<String, Map<String, Float>>(abs8167_0Table3MapCapacity);
		int[] abs8167_0Table3Rows = { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 };
		int abs8167_0titleRow = 6;
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8167.0_BusMktAndComp/Table3.csv", ABS8167_0_T3,
				abs8167_0Table3Columns, abs8167_0Table3Rows, abs8167_0titleRow, this.title, this.abs8167_0Table3);

		int[] abs8167_0Table6Columns = { 1, 2, 3, 4, 5 };
		int abs8167_0Table6MapCapacity = (int) Math.ceil(abs8167_0Table6Columns.length / MAP_LOAD_FACTOR);
		this.abs8167_0Table6 = new HashMap<String, Map<String, Float>>(abs8167_0Table6MapCapacity);
		int[] abs8167_0Table6Rows = { 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31 };
		this.loadAbsDataRowsColumnsCsv("/data/ABS/8167.0_BusMktAndComp/Table6.csv", ABS8167_0_T6,
				abs8167_0Table6Columns, abs8167_0Table6Rows, abs8167_0titleRow, this.title, this.abs8167_0Table6);

		// load pre-formatted ADI data
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading ADI data");
		int adiDataMapCapacity = (int) Math.ceil(NUM_ADIS / MAP_LOAD_FACTOR);
		this.adiData = new HashMap<String, Map<String, String>>(adiDataMapCapacity);
		int[] adiColumns = { 2, 3, 4, 5, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27,
				28, 29, 30, 31, 32, 34, 35, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 50, 51, 52, 56, 57, 58, 59,
				60, 63, 66, 67, 68, 79 };
		String[] adiCategories = { "Major Bank", "Other Domestic Bank", "Foreign Bank", "Mutual ADI" };
		this.loadAdiDataCsv("/data/ADI/ADI_data.csv", CalibrationData.ADI_DATA, adiColumns, adiCategories, this.title,
				this.unitType, this.adiData);

		// load pre-formatted currency data (approx 5.51 kB)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading currency data");
		int currencyDataMapCapacity = (int) Math.ceil(NUM_CURRENCIES / MAP_LOAD_FACTOR);
		this.currencyData = new HashMap<String, Map<String, String>>(currencyDataMapCapacity);
		int[] currencyColumns = { 1, 61, 62, 63, 64 };
		this.loadCurrencyDataCsv("/data/FxRates/FX Rates_Monthly.csv", CalibrationData.CCY_DATA, currencyColumns,
				this.title, this.currencyData);

		// load pre-formatted country data (approx 4.83 kB)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading country data");
		int countryDataMapCapacity = (int) Math.ceil(NUM_COUNTRIES / MAP_LOAD_FACTOR);
		this.countryData = new HashMap<String, Map<String, String>>(countryDataMapCapacity);
		int[] countryColumns = { 1, 3 };
		this.loadCountryDataCsv("/data/FxRates/CountriesCurrencies_ABS.csv", CalibrationData.COUNTRY_DATA,
				countryColumns, this.title, this.countryData);

		// load RBA Balance Sheet (337 bytes) and Profit & Loss Statement (335 bytes)
		System.out.println(new Date(System.currentTimeMillis()) + ": Loading RBA financial statements");
		int[] rbaBalShtRows = { 3, 4, 5, 6, 7, 11, 12, 13, 14, 18, 19, 25, 26, 27, 28, 29, 30, 31 };
		int rbaBalShtMapCapacity = (int) Math.ceil(rbaBalShtRows.length / MAP_LOAD_FACTOR);
		this.rbaBalSht = new HashMap<String, Float>(rbaBalShtMapCapacity);
		int rbaBalShtColumn = 2;
		float rbaMultiplier = 1f;
		this.loadFinancialStatementCsv("/data/RBA/RBA_BalSht.csv", CalibrationData.RBA_BS, rbaBalShtColumn,
				rbaBalShtRows, this.title, this.rbaBalSht, rbaMultiplier);

		int[] rbaProfitLossRows = { 3, 4, 5, 6, 7, 8, 9, 13, 14, 15, 18, 19 };
		int rbaProfitLossMapCapacity = (int) Math.ceil(rbaProfitLossRows.length / MAP_LOAD_FACTOR);
		this.rbaProfitLoss = new HashMap<String, Float>(rbaProfitLossMapCapacity);
		int rbaProfitLossColumn = 2;
		this.loadFinancialStatementCsv("/data/RBA/RBA_PnL.csv", CalibrationData.RBA_PL, rbaProfitLossColumn,
				rbaProfitLossRows, this.title, this.rbaProfitLoss, rbaMultiplier);

		// load Australian Government Financial Statistics
		System.out
				.println(new Date(System.currentTimeMillis()) + ": Loading Australian Government financial statements");
		int[] govtBalShtRows = { 8, 10, 12, 13, 15, 16, 22, 23, 25, 29 };
		int govtBalShtMapCapacity = (int) Math.ceil(govtBalShtRows.length / MAP_LOAD_FACTOR);
		this.govtBalSht = new HashMap<String, Float>(govtBalShtMapCapacity);
		int govtBalShtColumn = 10;
		float govtMultiplier = 1000000f;
		this.loadFinancialStatementCsv("/data/ABS/5512.0_GovtFinStats/55120DO057_201617 - Table 3 - Bal Sht.csv",
				CalibrationData.GOVT_PL, govtBalShtColumn, govtBalShtRows, this.title, this.govtBalSht, govtMultiplier);

		int[] govtProfitLossRows = { 7, 9, 10, 12, 17, 18, 22, 23, 37, 48 };
		int govtProfitLossMapCapacity = (int) Math.ceil(govtProfitLossRows.length / MAP_LOAD_FACTOR);
		this.govtProfitLoss = new HashMap<String, Float>(govtProfitLossMapCapacity);
		int govtProfitLossColumn = 10;
		this.loadFinancialStatementCsv("/data/ABS/5512.0_GovtFinStats/55120DO057_201617 - Table 1 - P&L.csv",
				CalibrationData.GOVT_PL, govtProfitLossColumn, govtProfitLossRows, this.title, this.govtProfitLoss,
				govtMultiplier);

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
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: Series ID, Date
	 */
	private void loadRbaDataCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units, Map<String, Map<Date, Float>> data) {

		CSVReader reader = null;
		try {
			InputStream is = this.getClass().getResourceAsStream(fileResourceLocation);
			reader = new CSVReader(new InputStreamReader(is));
			boolean header = true;
			boolean footer = false;
			String[] seriesId = new String[columnsToImport.length];
			String[] line = null;
			DateFormat dateFormat = new SimpleDateFormat("MMM-yyyy", Locale.ENGLISH);
			while ((line = reader.readNext()) != null && !footer) {
				if (header) {
					if (line[0].equals("Title")) {
						// store title
						titles.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							titles.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Units")) {
						// store unit types
						units.put(dataSourceName, new ArrayList<String>(columnsToImport.length));
						for (int i = 0; i < columnsToImport.length; i++) {
							units.get(dataSourceName).add(line[columnsToImport[i]]);
						}
					} else if (line[0].equals("Series ID")) {
						// store series ID as key with blank collections to populate with data below
						for (int i = 0; i < columnsToImport.length; i++) {
							seriesId[i] = line[columnsToImport[i]];
							data.put(line[columnsToImport[i]], new HashMap<Date, Float>());
						}
						header = false;
					}
				} else {
					if (line[0].isEmpty()) {
						footer = true;
					} else {
						for (int i = 0; i < columnsToImport.length; i++) {
							// parse the body of the data
							float value = 0f;
							try {
								value = Float.valueOf(line[columnsToImport[i]].replace(",", ""));
							} catch (NumberFormatException e) {
								// do nothing and leave it as zero.
							}
							data.get(seriesId[i]).put(dateFormat.parse(line[0]), value);
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
	 * Stores description-to-code mapping in UPPER CASE, so use toUpperCase() when
	 * getting the mapping from descriptions to codes.
	 * 
	 * Mappings are:<br>
	 * "Division Code to Division"<br>
	 * "Subdivision Code to Subdivision"<br>
	 * "Group Code to Group"<br>
	 * "Class Code to Class"<br>
	 * "Industry Code to Industry"<br>
	 * "Division to Division Code"<br>
	 * "Subdivision to Subdivision Code"<br>
	 * "Group to Group Code"<br>
	 * "Class to Class Code"<br>
	 * "Industry to Industry Code"<br>
	 * "Industry Code to Class Code"<br>
	 * "Industry Code to Group Code"<br>
	 * "Industry Code to Subdivision Code"<br>
	 * "Industry Code to Division Code"<br>
	 * "Class Code to Group Code"<br>
	 * "Class Code to Subdivision Code"<br>
	 * "Class Code to Division Code"<br>
	 * "Group Code to Subdivision Code"<br>
	 * "Group Code to Division Code"<br>
	 * "Subdivision Code to Division Code"<br>
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
						mappingTitles.add("Industry Code to Industry");
						mappingTitles.add("Division to Division Code");
						mappingTitles.add("Subdivision to Subdivision Code");
						mappingTitles.add("Group to Group Code");
						mappingTitles.add("Class to Class Code");
						mappingTitles.add("Industry to Industry Code");
						mappingTitles.add("Industry Code to Class Code");
						mappingTitles.add("Industry Code to Group Code");
						mappingTitles.add("Industry Code to Subdivision Code");
						mappingTitles.add("Industry Code to Division Code");
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
					data.get("Industry Code to Industry").put(line[8], line[9]);
					data.get("Division to Division Code").put(line[1].toUpperCase(), line[0]);
					data.get("Subdivision to Subdivision Code").put(line[3].toUpperCase(), line[2]);
					data.get("Group to Group Code").put(line[5].toUpperCase(), line[4]);
					data.get("Class to Class Code").put(line[7].toUpperCase(), line[6]);
					data.get("Industry to Industry Code").put(line[9].toUpperCase(), line[8]);
					data.get("Industry Code to Class Code").put(line[8], line[6]);
					data.get("Industry Code to Group Code").put(line[8], line[4]);
					data.get("Industry Code to Subdivision Code").put(line[8], line[2]);
					data.get("Industry Code to Division Code").put(line[8], line[0]);
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
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: series ID, date
	 */
	private void loadAbsDataCsv_Catalogue(String fileResourceLocation, String catalogueName, int[] columnsToImport,
			Map<String, List<String>> titles, Map<String, List<String>> units, Map<String, Map<Date, Float>> data) {

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
							data.put(line[columnsToImport[i]], new HashMap<Date, Float>());
						}
						header = false;
					}
				} else {
					for (int i = 0; i < columnsToImport.length; i++) {
						// parse the body of the data
						float value = 0f;
						try {
							value = Float.valueOf(line[columnsToImport[i]].replace(",", ""));
						} catch (NumberFormatException e) {
							// do nothing and leave it as zero.
						}
						data.get(seriesId[i]).put(dateFormat.parse(line[0]), value);
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
	 * The first key is the column headings, then the rest of the keys are in the
	 * same order that the data is sorted in. This makes knowing when to create a
	 * new child map easier because the key will be different to the previous line's
	 * key.
	 * 
	 * @param fileResourceLocation
	 * @param catalogueName
	 * @param columnsToImport
	 * @param titles
	 * @param units
	 * @param data                 - the data map returned.<br>
	 *                             Keys: industry, country, state, value range
	 */
	private void loadAbsDataCsv_5368_0Exporters(String fileResourceLocation, String catalogueName,
			int[] columnsToImport, Map<String, List<String>> titles, Map<String, List<String>> units,
			Map<String, Map<String, Map<String, Map<String, Float>>>> data) {

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
							data.put(seriesId[i], new HashMap<String, Map<String, Map<String, Float>>>());
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
							data.get(seriesId[i]).put(line[0], new HashMap<String, Map<String, Float>>());
						}
						// check if we need to create a new State map
						if (!line[0].equals(prevState)) {
							data.get(seriesId[i]).get(line[0]).put(line[2], new HashMap<String, Float>());
						}

						// parse the body of the data
						float value = 0f;
						try {
							value = Float.valueOf(line[columnsToImport[i]].replace(",", ""));
						} catch (NumberFormatException e) {
							// do nothing and leave it as zero.
						}
						data.get(seriesId[i]).get(line[0]).get(line[2]).put(line[1], value);
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
	 * @param data                 - the data map that the values are returned
	 *                             in.<br>
	 *                             Keys: column title, row name (first column).
	 */
	private void loadAbsDataRowsColumnsCsv(String fileResourceLocation, String dataSourceName, int[] columnsToImport,
			int[] rowsToImport, int titleRow, Map<String, List<String>> titles, Map<String, Map<String, Float>> data) {

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
							data.put(seriesId[i], new HashMap<String, Float>());
						}
					}
				} else {
					int i = 0;
					rowCheck: while (i < rowsToImport.length) {
						if (currentRow == rowsToImport[i]) {
							// parse the body of the data
							float value = 0f;
							try {
								value = Float.valueOf(line[columnsToImport[i]].replace(",", ""));
							} catch (NumberFormatException e) {
								// do nothing and leave it as zero.
							}
							data.get(seriesId[i]).put(line[0], value);
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
			int[] rowsToImport, Map<String, List<String>> titles, Map<String, Float> data, float multiplier) {

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
								Float.valueOf(line[columnToImport].replaceAll(",", "").replaceAll("$", ""))
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

	@PostConstruct
	private void init() {
		this.dataLoaded = false;

		this.title = null;
		this.unitType = null;

		this.rbaE1 = null;

		this.abs1292_0_55_002ANZSIC = null;
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
		this.abs8167_0Table3 = null;
		this.abs8167_0Table6 = null;

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
	public Map<String, Map<Date, Float>> getRbaE1() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaE1;
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
	 * @return the abs5368_0Exporters
	 */
	public Map<String, Map<String, Map<String, Map<String, Float>>>> getAbs5368_0Exporters() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Exporters;
	}

	/**
	 * @return the abs5368_0Table14a
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table14a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table14a;
	}

	/**
	 * @return the abs5368_0Table14b
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table14b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table14b;
	}

	/**
	 * @return the abs5368_0Table36a
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36a;
	}

	/**
	 * @return the abs5368_0Table36b
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36b;
	}

	/**
	 * @return the abs5368_0Table36c
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36c() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36c;
	}

	/**
	 * @return the abs5368_0Table36d
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36d() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36d;
	}

	/**
	 * @return the abs5368_0Table36e
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36e() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36e;
	}

	/**
	 * @return the abs5368_0Table36f
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36f() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36f;
	}

	/**
	 * @return the abs5368_0Table36g
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36g() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36g;
	}

	/**
	 * @return the abs5368_0Table36h
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table36h() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table36h;
	}

	/**
	 * @return the abs5368_0Table37a
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37a() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37a;
	}

	/**
	 * @return the abs5368_0Table37b
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37b() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37b;
	}

	/**
	 * @return the abs5368_0Table37c
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37c() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37c;
	}

	/**
	 * @return the abs5368_0Table37d
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37d() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37d;
	}

	/**
	 * @return the abs5368_0Table37e
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37e() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37e;
	}

	/**
	 * @return the abs5368_0Table37f
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37f() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37f;
	}

	/**
	 * @return the abs5368_0Table37g
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37g() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37g;
	}

	/**
	 * @return the abs5368_0Table37h
	 */
	public Map<String, Map<Date, Float>> getAbs5368_0Table37h() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs5368_0Table37h;
	}

	/**
	 * @return the abs8167_0Table3
	 */
	public Map<String, Map<String, Float>> getAbs8167_0Table3() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8167_0Table3;
	}

	/**
	 * @return the abs8167_0Table6
	 */
	public Map<String, Map<String, Float>> getAbs8167_0Table6() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return abs8167_0Table6;
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
	public Map<String, Float> getRbaBalSht() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaBalSht;
	}

	/**
	 * @return the rbaProfitLoss
	 */
	public Map<String, Float> getRbaProfitLoss() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return rbaProfitLoss;
	}

	/**
	 * @return the govtBalSht
	 */
	public Map<String, Float> getGovtBalSht() {
		if (!this.dataLoaded) {
			this.loadData();
		}
		return govtBalSht;
	}

	/**
	 * @return the govtProfitLoss
	 */
	public Map<String, Float> getGovtProfitLoss() {
		if (!this.dataLoaded) {
			this.loadData();
		}
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
