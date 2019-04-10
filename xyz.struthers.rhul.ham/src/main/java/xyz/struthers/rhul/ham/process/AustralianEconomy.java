/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.data.Currencies;

/**
 * A class to hold all the Agents so they're all available in the one place.
 * Also allows the memory consumed by the initial calibration data load to be
 * freed up once the agents have been created.
 * 
 * TODO: This class will probably implement the MASON library, but only know
 * about a single step in the simulation. Some other class will drive the
 * simulation process itself.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
@Component
@Scope(value = "singleton")
public class AustralianEconomy implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final boolean DEBUG_RAM_USAGE = true;

	public static final float BUFFER_COUNTRY = 1000000f;
	public static final float BUFFER_GOVT = 1000000f;
	public static final float BUFFER_RBA = 1000000f;

	// Agents
	Household[] households;
	Individual[] individuals;
	Business[] businesses;
	AuthorisedDepositTakingInstitution[] adis;
	ForeignCountry[] countries;
	Currencies currencies;
	ReserveBankOfAustralia rba;
	AustralianGovernment government;

	// Process
	ClearingPaymentVector payments;
	List<List<Float>> liabilitiesAmounts;
	List<List<Integer>> liabilitiesIndices;
	List<Float> operatingCashFlow;
	/**
	 * Clearing Payment Vector output is a map containing:<br>
	 * List<Float> ClearingPaymentVector,<br>
	 * List<List<Float>> ClearingPaymentMatrix,<br>
	 * List<List<Integer>> ClearingPaymentIndices,<br>
	 * List<Float> NodeEquity, and<br>
	 * List<Integer> NodeDefaultOrder.
	 */
	Map<String, Object> clearingPaymentVectorOutput;

	// Analytics
	int[] businessTypeCount;

	/**
	 * 
	 */
	public AustralianEconomy() {
		super();
		this.init();
	}

	@PostConstruct
	private void init() {
		// Agents
		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;
		this.rba = null;
		this.government = null;

		// Process
		this.payments = null;
		this.liabilitiesAmounts = null;
		this.liabilitiesIndices = null;
		this.operatingCashFlow = null;

		// Analytics
		this.businessTypeCount = null;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 */
	@PreDestroy
	public void close() {
		// TODO: implement close method for Clearing Payment Vector
	}

	public ClearingPaymentInputs prepareOneMonth(int iteration) {
		long memoryBefore = 0L;
		long memoryAfter = 0L;
		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE BEFORE SIMULATION ROUND "
					+ iteration + ": " + formatter.format(megabytesAfter) + "MB");
			memoryBefore = memoryAfter;
		}

		// generate the FX rates for this iteration using the desired strategy
		this.currencies.prepareFxRatesSame(iteration);
		for (ForeignCountry country : this.countries) {
			country.updateExchangeRates(); // applies currency FX rates to country
		}

		// prepare the inputs to the Clearing Payments Vector algorithm
		this.preparePaymentsClearingVectorInputs(iteration);

		ClearingPaymentInputs cpvInputs = new ClearingPaymentInputs(this.liabilitiesAmounts, this.liabilitiesIndices,
				this.operatingCashFlow);

		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out
					.println(new Date(System.currentTimeMillis()) + ": MEMORY CONSUMED PREPARING CPV INPUTS FOR ROUND "
							+ iteration + ": " + formatter.format(megabytesAfter - megabytesBefore)
							+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		}

		return cpvInputs;
	}

	public void simulateOneMonth(int iteration) {
		long memoryBefore = 0L;
		long memoryAfter = 0L;
		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE BEFORE SIMULATION ROUND "
					+ iteration + ": " + formatter.format(megabytesAfter) + "MB");
			memoryBefore = memoryAfter;
		}

		// generate the FX rates for this iteration using the desired strategy
		this.currencies.prepareFxRatesSame(iteration);
		for (ForeignCountry country : this.countries) {
			country.updateExchangeRates(); // applies currency FX rates to country
		}

		// generate interest rates for this iteration using the desired strategy
		this.rba.setCashRateSame(iteration);
		for (AuthorisedDepositTakingInstitution adi : this.adis) {
			adi.setRba(this.rba);
			adi.setLoanRate(iteration);
			adi.setDepositRate(iteration);
			adi.setBorrowingsRate(iteration);
			adi.setGovtBondRate(iteration);
		}

		// prepare the inputs to the Clearing Payments Vector algorithm
		this.preparePaymentsClearingVectorInputs(iteration);

		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out
					.println(new Date(System.currentTimeMillis()) + ": MEMORY CONSUMED PREPARING CPV INPUTS FOR ROUND "
							+ iteration + ": " + formatter.format(megabytesAfter - megabytesBefore)
							+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		}

		// FIXME Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
		// when looping through second iteration of CPV, so write agents to disk then
		// drop them

		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;
		this.rba = null;
		this.government = null;
		System.gc();

		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY CLEARED DUMPING AGENTS FOR ROUND "
					+ iteration + ": " + formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
					+ formatter.format(megabytesAfter) + "MB)");
		}

		this.clearingPaymentVectorOutput = this.payments.calculate(this.liabilitiesAmounts, this.liabilitiesIndices,
				this.operatingCashFlow);

		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY CONSUMED BY SIMULATION ROUND "
					+ iteration + ": " + formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
					+ formatter.format(megabytesAfter) + "MB)");
		}

		this.processPaymentsClearingVectorOutputs();

		if (DEBUG_RAM_USAGE) {
			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesAfter = memoryAfter / 1024f / 1024f;
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out.println(
					new Date(System.currentTimeMillis()) + ": MEMORY CONSUMED AFTER PROCESSING CPV OUTPUTS FOR ROUND "
							+ iteration + ": " + formatter.format(megabytesAfter - megabytesBefore)
							+ "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)");
		}
	}

	/**
	 * Prepares input data for Clearing Payments Vector algorithm.
	 * 
	 * Operating cashflow should be at least the sum of the liabilities for foreign
	 * countries because they're outside of the scope of the model. They are only
	 * included so that payments from agents who are in scope to foreign countries
	 * can be included.
	 * 
	 * Businesses can cease to exist if they have gone bankrupt in a prior
	 * iteration, they must still appear in the model so the indices that were set
	 * during calibration remain valid.
	 * 
	 * TODO: Combine employees in the one Household if they work for the same
	 * Employer.
	 * 
	 * TODO: include cash balances as exogeneous inputs because they only default if
	 * cash plus income is less than liabilities
	 */
	void preparePaymentsClearingVectorInputs(int iteration) {
		// initialise local variables
		int totalAgentCount = 1 + 1 + this.households.length + this.businesses.length + this.adis.length
				+ this.countries.length;
		this.liabilitiesAmounts = new ArrayList<List<Float>>(totalAgentCount);
		this.liabilitiesIndices = new ArrayList<List<Integer>>(totalAgentCount);
		this.operatingCashFlow = new ArrayList<Float>(totalAgentCount);
		for (int i = 0; i < totalAgentCount; i++) {
			// initialise them so we can use set without getting index out of bounds errors
			this.liabilitiesAmounts.add(new ArrayList<Float>());
			this.liabilitiesIndices.add(new ArrayList<Integer>());
			this.operatingCashFlow.add(0f);
		}

		// households
		for (Household household : this.households) {
			int paymentClearingIndex = household.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = household.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			float exogeneous = household.getPnlInvestmentIncome() + household.getPnlOtherIncome();
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// businesses
		for (Business business : this.businesses) {
			int paymentClearingIndex = business.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = business.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			float exogeneous = business.getOtherIncome();
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// ADIs
		for (AuthorisedDepositTakingInstitution adi : this.adis) {
			int paymentClearingIndex = adi.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = adi.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			float exogeneous = adi.getPnlTradingIncome() + adi.getPnlInvestmentIncome() + adi.getPnlOtherIncome();
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// countries
		for (ForeignCountry country : this.countries) {
			int paymentClearingIndex = country.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = country.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			// foreign countries are assumed to never default
			float totalLiabilities = (float) liabilityAmounts.stream().mapToDouble(o -> o).sum();
			float exogeneous = totalLiabilities + BUFFER_COUNTRY; // liabilities plus a buffer
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// government
		{
			int paymentClearingIndex = this.government.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = this.government.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			// the government is assumed to never default
			// whatever it is short by it simply borrows
			float exogeneous = this.government.getPnlSaleOfGoodsAndServices() + this.government.getPnlOtherIncome()
					+ BUFFER_GOVT; // liabilities plus a buffer
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// RBA
		{
			int paymentClearingIndex = this.rba.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = this.rba.getAmountsPayable(iteration);
			ArrayList<Float> liabilityAmounts = new ArrayList<Float>(nodePayments.size());
			ArrayList<Integer> liabilityIndices = new ArrayList<Integer>(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				liabilityAmounts.add(nodePayments.get(creditorIdx).getLiabilityAmount());
				liabilityIndices.add(nodePayments.get(creditorIdx).getRecipientIndex());
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			// TODO the RBA is assumed to never default
			// whatever it is short by it simply borrows
			float exogeneous = this.rba.getPnlPersonnelExpenses() + this.rba.getPnlOtherExpenses()
					+ this.rba.getPnlDistributionPayableToCommonwealth() + this.rba.getBsCash() + BUFFER_RBA;
			// liabilities plus a buffer

			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);
		}

		// create Clearing Payments Vector
		this.payments = new ClearingPaymentVector();
	}

	/**
	 * Takes the output of the Payments Clearing Vector algorithm and updates the
	 * status and financial statements of the agents involved.
	 */
	void processPaymentsClearingVectorOutputs() {

	}

	/**
	 * Saves every single Agent to file.
	 * 
	 * @return a Set of the filenames for each Agent class
	 */
	public Set<String> saveDetailsToFile(int iteration) {
		Set<String> filenames = new HashSet<String>((int) Math.ceil(8 / 0.75) + 1);

		String filename = this.saveGovernmentDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveRbaDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveAdiDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveForeignCountryDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveBusinessDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveHouseholdDetailsToFile(iteration);
		filenames.add(filename);
		filename = this.saveIndividualDetailsToFile(iteration);
		filenames.add(filename);

		filename = this.saveCurrencyDetailsToFile(iteration);
		filenames.add(filename);

		return filenames;
	}

	/**
	 * Save Individual to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveHouseholdDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.households[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_Household_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.households.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.households[row].toCsvString(Properties.CSV_SEPARATOR, iteration))
								.split(Properties.CSV_SEPARATOR);
				csvWriter.writeNext(entries);
			}
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save Individual to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveIndividualDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.individuals[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_Individual_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.individuals.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.individuals[row].toCsvString(Properties.CSV_SEPARATOR, iteration))
								.split(Properties.CSV_SEPARATOR);
				csvWriter.writeNext(entries);
			}
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save Business to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveBusinessDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.businesses[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_Business_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.businesses.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.businesses[row].toCsvString(Properties.CSV_SEPARATOR, iteration))
								.split(Properties.CSV_SEPARATOR);
				csvWriter.writeNext(entries);
			}
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save AuthorisedDepositTakingInstitution to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveAdiDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.adis[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_ADI_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.adis.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.adis[row].toCsvString(Properties.CSV_SEPARATOR, iteration))
								.split(Properties.CSV_SEPARATOR);
				csvWriter.writeNext(entries);
			}
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save ForeignCountry to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveForeignCountryDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.countries[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_ForeignCountry_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.countries.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.countries[row].toCsvString(Properties.CSV_SEPARATOR, iteration))
								.split(Properties.CSV_SEPARATOR);
				csvWriter.writeNext(entries);
			}
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save Currencies to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveCurrencyDetailsToFile(int iteration) {
		List<Currencies> beans = Arrays.asList(this.currencies);
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_Currencies_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			StatefulBeanToCsv<Currencies> beanToCsv = new StatefulBeanToCsvBuilder<Currencies>(writer).build();
			beanToCsv.write(beans);
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} catch (CsvDataTypeMismatchException e) {
			// write beans
			e.printStackTrace();
		} catch (CsvRequiredFieldEmptyException e) {
			// write beans
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save ReserveBankOfAustralia to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveRbaDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.rba.toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_RBA_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			entries = (iteration + Properties.CSV_SEPARATOR + this.rba.toCsvString(Properties.CSV_SEPARATOR, iteration))
					.split(Properties.CSV_SEPARATOR);
			csvWriter.writeNext(entries);
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * Save AustralianGovernment to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveGovernmentDetailsToFile(int iteration) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.government.toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + Properties.timestamp + "_Agents_Govt_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			entries = (iteration + Properties.CSV_SEPARATOR
					+ this.government.toCsvString(Properties.CSV_SEPARATOR, iteration)).split(Properties.CSV_SEPARATOR);
			csvWriter.writeNext(entries);
			writer.close();
		} catch (IOException e) {
			// new FileWriter
			e.printStackTrace();
		} finally {
			writer = null;
		}
		return filename;
	}

	/**
	 * @return the households
	 */
	public Household[] getHouseholds() {
		return households;
	}

	/**
	 * @param households the households to set
	 */
	public void setHouseholds(List<Household> households) {
		this.households = households.toArray(new Household[households.size()]);
	}

	/**
	 * @return the individuals
	 */
	public Individual[] getIndividuals() {
		return individuals;
	}

	/**
	 * @param individuals the individuals to set
	 */
	public void setIndividuals(List<Individual> individuals) {
		this.individuals = individuals.toArray(new Individual[individuals.size()]);
	}

	/**
	 * @return the businesses
	 */
	public Business[] getBusinesses() {
		return businesses;
	}

	/**
	 * @param businesses the businesses to set
	 */
	public void setBusinesses(List<Business> businesses) {
		this.businesses = businesses.toArray(new Business[businesses.size()]);
	}

	/**
	 * @return the adis
	 */
	public AuthorisedDepositTakingInstitution[] getAdis() {
		return adis;
	}

	/**
	 * @param adis the adis to set
	 */
	public void setAdis(List<AuthorisedDepositTakingInstitution> adis) {
		this.adis = adis.toArray(new AuthorisedDepositTakingInstitution[adis.size()]);
	}

	/**
	 * @return the countries
	 */
	public ForeignCountry[] getCountries() {
		return countries;
	}

	/**
	 * @param countries the countries to set
	 */
	public void setCountries(List<ForeignCountry> countries) {
		this.countries = countries.toArray(new ForeignCountry[countries.size()]);
	}

	/**
	 * @return the currencies
	 */
	public Currencies getCurrencies() {
		return currencies;
	}

	/**
	 * @param currencies the currencies to set
	 */
	public void setCurrencies(Currencies currencies) {
		this.currencies = currencies;
	}

	/**
	 * @return the rba
	 */
	public ReserveBankOfAustralia getRba() {
		return rba;
	}

	/**
	 * @param rba the rba to set
	 */
	public void setRba(ReserveBankOfAustralia rba) {
		this.rba = rba;
	}

	/**
	 * @return the government
	 */
	public AustralianGovernment getGovernment() {
		return government;
	}

	/**
	 * @param government the government to set
	 */
	public void setGovernment(AustralianGovernment government) {
		this.government = government;
	}

	/**
	 * @return the businessTypeCount
	 */
	public int[] getBusinessTypeCount() {
		return businessTypeCount;
	}

	/**
	 * @param businessTypeCount the businessTypeCount to set
	 */
	public void setBusinessTypeCount(int[] businessTypeCount) {
		this.businessTypeCount = businessTypeCount;
	}

}
