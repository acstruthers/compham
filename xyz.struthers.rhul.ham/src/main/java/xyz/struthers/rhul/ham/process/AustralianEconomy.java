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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.opencsv.CSVWriterBuilder;
import com.opencsv.ICSVWriter;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.data.Currencies;
import xyz.struthers.rhul.ham.data.Currency;

/**
 * A class to hold all the Agents so they're all available in the one place.
 * Also allows the memory consumed by the initial calibration data load to be
 * freed up once the agents have been created.
 * <p>
 * This class only knows about a single step in the simulation. Some other class
 * will drive the simulation process itself.
 * 
 * @author Adam Struthers
 * @since 02-Feb-2019
 */
@Component
@Scope(value = "singleton")
public class AustralianEconomy implements Serializable {

	private static final long serialVersionUID = 1L;

	public static final boolean DEBUG_RAM_USAGE = true;

	public static final float BUFFER_COUNTRY = Float.MAX_VALUE / 2f;
	public static final float BUFFER_GOVT = Float.MAX_VALUE / 2f;
	public static final float BUFFER_RBA = Float.MAX_VALUE / 2f;

	// Agents
	Household[] households;
	Individual[] individuals;
	Business[] businesses;
	AuthorisedDepositTakingInstitution[] adis;
	ForeignCountry[] countries;
	Currencies currencies;
	ReserveBankOfAustralia rba;
	AustralianGovernment government;
	Properties properties;
	Random random;

	// Process
	ClearingPaymentVector payments;
	// List<List<Float>> liabilitiesAmounts;
	// List<List<Integer>> liabilitiesIndices;
	// List<Float> operatingCashFlow;
	// List<Float> liquidAssets; // cash at bank, etc.
	List<TFloatArrayList> liabilitiesAmounts;
	List<TIntArrayList> liabilitiesIndices;
	TFloatArrayList operatingCashFlow;
	TFloatArrayList liquidAssets; // cash at bank, etc.
	/**
	 * Clearing Payment Vector output is a map containing:<br>
	 * List<Float> ClearingPaymentVector,<br>
	 * List<List<Float>> ClearingPaymentMatrix,<br>
	 * List<List<Integer>> ClearingPaymentIndices,<br>
	 * List<Float> NodeEquity, and<br>
	 * List<Integer> NodeDefaultOrder.
	 */
	ClearingPaymentOutputs clearingPaymentVectorOutput;

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
	 * 
	 * 
	 * The economy stays alive as long as the model is running, so keep this simple
	 * because the entire JVM will be destroyed and its memory freed by the OS.
	 */
	@PreDestroy
	public void close() {
		int i = 0;
		for (i = 0; i < this.households.length; i++) {
			this.households = null;
		}
		for (i = 0; i < this.individuals.length; i++) {
			this.individuals = null;
		}
		for (i = 0; i < this.businesses.length; i++) {
			this.businesses = null;
		}
		for (i = 0; i < this.adis.length; i++) {
			this.adis = null;
		}
		for (i = 0; i < this.countries.length; i++) {
			this.countries = null;
		}
		this.currencies = null;
		this.rba = null;
		this.government = null;
		this.payments = null;

		for (i = 0; i < this.liabilitiesAmounts.size(); i++) {
			this.liabilitiesAmounts.clear();
			this.liabilitiesAmounts.set(i, null);
		}
		this.liabilitiesAmounts.clear();
		this.liabilitiesAmounts = null;
		for (i = 0; i < this.liabilitiesIndices.size(); i++) {
			this.liabilitiesIndices.clear();
			this.liabilitiesIndices.set(i, null);
		}
		this.liabilitiesIndices.clear();
		this.liabilitiesIndices = null;
		this.operatingCashFlow.clear();
		this.operatingCashFlow = null;
		this.liquidAssets.clear();
		this.liquidAssets = null;
		this.clearingPaymentVectorOutput.close();
		this.businessTypeCount = null;
	}

	public ClearingPaymentInputs prepareOneMonth(int iteration, String scenarioName) {
		if (this.random == null) {
			this.random = this.properties.getRandom();
		}

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
		if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_1YR) {
			this.currencies.prepareFxRatesRandom1yr(iteration, this.random);
		} else if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_1YR_UP) {
			this.currencies.prepareFxRatesRandom1yrUp(iteration, this.random);
		} else if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_1YR_DOWN) {
			this.currencies.prepareFxRatesRandom1yrDown(iteration, this.random);
		} else if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_5YR) {
			this.currencies.prepareFxRatesRandom5yr(iteration, this.random);
		} else if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_5YR_UP) {
			this.currencies.prepareFxRatesRandom5yrUp(iteration, this.random);
		} else if (Properties.FX_RATE_STRATEGY == Currencies.RANDOM_5YR_DOWN) {
			this.currencies.prepareFxRatesRandom5yrDown(iteration, this.random);
		} else {
			// same FX rates for all iterations
			this.currencies.prepareFxRatesSame(iteration);
		}
		for (ForeignCountry country : this.countries) {
			country.updateExchangeRates(); // applies currency FX rates to country
		}

		// generate interest rates for this iteration using the desired strategy
		if (Properties.INTEREST_RATE_STRATEGY == ReserveBankOfAustralia.RATES_CUSTOM) {
			this.rba.setCashRateCustomPath(iteration, Properties.INTEREST_RATE_CUSTOM_PATH);
		} else {
			// same interest rates for all iterations
			this.rba.setCashRateSame(iteration);
		}
		for (AuthorisedDepositTakingInstitution adi : this.adis) {
			adi.setRba(this.rba);
			adi.setLoanRate(iteration);
			adi.setDepositRate(iteration);
			adi.setBorrowingsRate(iteration);
			adi.setGovtBondRate(iteration);
		}

		// prepare the inputs to the Clearing Payments Vector algorithm
		this.preparePaymentsClearingVectorInputs(iteration, scenarioName);

		ClearingPaymentInputs cpvInputs = new ClearingPaymentInputs(this.liabilitiesAmounts, this.liabilitiesIndices,
				this.operatingCashFlow, this.liquidAssets, iteration);

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

	/**
	 * Pretty sure this is deprecated?
	 * 
	 * @deprecated
	 * 
	 * @param iteration
	 */
	/*
	 * public void simulateOneMonth(int iteration) { long memoryBefore = 0L; long
	 * memoryAfter = 0L; DecimalFormat formatter = new DecimalFormat("#,##0.00"); if
	 * (DEBUG_RAM_USAGE) { System.gc(); memoryAfter =
	 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); float
	 * megabytesAfter = memoryAfter / 1024f / 1024f; System.out.println(new
	 * Date(System.currentTimeMillis()) + ": MEMORY USAGE BEFORE SIMULATION ROUND "
	 * + iteration + ": " + formatter.format(megabytesAfter) + "MB"); memoryBefore =
	 * memoryAfter; }
	 * 
	 * // generate the FX rates for this iteration using the desired strategy
	 * this.currencies.prepareFxRatesSame(iteration); for (ForeignCountry country :
	 * this.countries) { country.updateExchangeRates(); // applies currency FX rates
	 * to country }
	 * 
	 * // generate interest rates for this iteration using the desired strategy
	 * this.rba.setCashRateSame(iteration); for (AuthorisedDepositTakingInstitution
	 * adi : this.adis) { adi.setRba(this.rba); adi.setLoanRate(iteration);
	 * adi.setDepositRate(iteration); adi.setBorrowingsRate(iteration);
	 * adi.setGovtBondRate(iteration); }
	 * 
	 * // prepare the inputs to the Clearing Payments Vector algorithm
	 * this.preparePaymentsClearingVectorInputs(iteration);
	 * 
	 * if (DEBUG_RAM_USAGE) { System.gc(); memoryAfter =
	 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); float
	 * megabytesAfter = memoryAfter / 1024f / 1024f; float megabytesBefore =
	 * memoryBefore / 1024f / 1024f; System.out .println(new
	 * Date(System.currentTimeMillis()) +
	 * ": MEMORY CONSUMED PREPARING CPV INPUTS FOR ROUND " + iteration + ": " +
	 * formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
	 * + formatter.format(megabytesAfter) + "MB)"); }
	 * 
	 * // drops all agents to reduce memory footprint //this.households = null;
	 * //this.individuals = null; //this.businesses = null; //this.adis = null;
	 * //this.countries = null; //this.currencies = null; //this.rba = null;
	 * //this.government = null;
	 * 
	 * System.gc();
	 * 
	 * if (DEBUG_RAM_USAGE) { System.gc(); memoryAfter =
	 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); float
	 * megabytesAfter = memoryAfter / 1024f / 1024f; float megabytesBefore =
	 * memoryBefore / 1024f / 1024f; System.out.println(new
	 * Date(System.currentTimeMillis()) +
	 * ": MEMORY CLEARED DUMPING AGENTS FOR ROUND " + iteration + ": " +
	 * formatter.format(megabytesAfter - megabytesBefore) + "MB (CURRENT TOTAL IS: "
	 * + formatter.format(megabytesAfter) + "MB)"); }
	 * 
	 * this.clearingPaymentVectorOutput =
	 * this.payments.calculate(this.liabilitiesAmounts, this.liabilitiesIndices,
	 * this.operatingCashFlow, this.liquidAssets);
	 * 
	 * if (DEBUG_RAM_USAGE) { System.gc(); memoryAfter =
	 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); float
	 * megabytesAfter = memoryAfter / 1024f / 1024f; float megabytesBefore =
	 * memoryBefore / 1024f / 1024f; System.out.println(new
	 * Date(System.currentTimeMillis()) + ": MEMORY CONSUMED BY SIMULATION ROUND " +
	 * iteration + ": " + formatter.format(megabytesAfter - megabytesBefore) +
	 * "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)"); }
	 * 
	 * this.processPaymentsClearingVectorOutputs();
	 * 
	 * if (DEBUG_RAM_USAGE) { System.gc(); memoryAfter =
	 * Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory(); float
	 * megabytesAfter = memoryAfter / 1024f / 1024f; float megabytesBefore =
	 * memoryBefore / 1024f / 1024f; System.out.println( new
	 * Date(System.currentTimeMillis()) +
	 * ": MEMORY CONSUMED AFTER PROCESSING CPV OUTPUTS FOR ROUND " + iteration +
	 * ": " + formatter.format(megabytesAfter - megabytesBefore) +
	 * "MB (CURRENT TOTAL IS: " + formatter.format(megabytesAfter) + "MB)"); } }
	 */

	/**
	 * Updates agents with the output from the CPV.
	 * 
	 * @param iteration
	 */
	public void updateOneMonth(ClearingPaymentOutputs cpvOutput) {
		this.clearingPaymentVectorOutput = cpvOutput;
		this.processPaymentsClearingVectorOutputs();
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
	 * Includes cash balances as exogeneous inputs because they only default if cash
	 * plus income is less than liabilities.
	 * 
	 * MAYBE: Combine employees in the one Household if they work for the same
	 * Employer.
	 * 
	 * N.B. Agents MUST appear in the exact same order as they are assigned CPV
	 * indices. This ensures that the right agents are addressed in in the
	 * liabilities matrix. It also makes it far more efficient to process the output
	 * of the CPV.
	 */
	void preparePaymentsClearingVectorInputs(int iteration, String scenarioName) {
		// initialise local variables
		int totalAgentCount = 1 + 1 + this.households.length + this.businesses.length + this.adis.length
				+ this.countries.length;
		this.liabilitiesAmounts = new ArrayList<TFloatArrayList>(totalAgentCount);
		this.liabilitiesIndices = new ArrayList<TIntArrayList>(totalAgentCount);
		this.operatingCashFlow = new TFloatArrayList(totalAgentCount);
		this.liquidAssets = new TFloatArrayList(totalAgentCount);
		ArrayList<Float> receivableFromAnotherAgent = new ArrayList<Float>(totalAgentCount);
		for (int i = 0; i < totalAgentCount; i++) {
			// initialise them so we can use set without getting index out of bounds errors
			this.liabilitiesAmounts.add(new TFloatArrayList());
			this.liabilitiesIndices.add(new TIntArrayList());
			this.operatingCashFlow.add(0f);
			this.liquidAssets.add(0f);
			receivableFromAnotherAgent.add(0f);
		}

		// households
		for (Household household : this.households) {
			int paymentClearingIndex = household.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = household.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate liquid assets
			float liquid = household.getBsBankDeposits()
					+ household.getBsSuperannuation() * (1f - Properties.SUPERANNUATION_HAIRCUT);
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		// businesses
		for (Business business : this.businesses) {
			int paymentClearingIndex = business.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = business.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate liquid assets
			float liquid = business.getBankDeposits()
					+ business.getOtherFinancialAssets() * (1f - Properties.INVESTMENT_HAIRCUT)
					+ business.getForeignEquities() * (1f - Properties.FOREIGN_INVESTMENT_HAIRCUT);
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		// ADIs
		for (AuthorisedDepositTakingInstitution adi : this.adis) {
			int paymentClearingIndex = adi.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = adi.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate liquid assets
			float liquid = adi.getBsCash() + adi.getBsInvestments() * Properties.ADI_HQLA_PROPORTION;
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		// foreign countries
		for (ForeignCountry country : this.countries) {
			int paymentClearingIndex = country.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = country.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate exogeneous cash flow (i.e. not from another Agent)
			// foreign countries are assumed to never default
			// float totalLiabilities = (float) liabilityAmounts.stream().mapToDouble(o ->
			// o).sum();
			float totalLiabilities = 0f;
			for (int liabIdx = 0; liabIdx < liabilityAmounts.size(); liabIdx++) {
				totalLiabilities += liabilityAmounts.get(liabIdx);
			}
			// (float) liabilityAmounts.stream().mapToDouble(o -> o).sum();
			float exogeneous = totalLiabilities;
			this.operatingCashFlow.set(paymentClearingIndex, exogeneous);

			// calculate liquid assets
			float liquid = BUFFER_COUNTRY; // buffer so countries never default
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		// government
		{
			int paymentClearingIndex = this.government.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = this.government.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate liquid assets
			float liquid = BUFFER_GOVT; // buffer so government never defaults
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		// RBA
		{
			int paymentClearingIndex = this.rba.getPaymentClearingIndex();

			// calculate liabilities
			List<NodePayment> nodePayments = this.rba.getAmountsPayable(iteration);
			TFloatArrayList liabilityAmounts = new TFloatArrayList(nodePayments.size());
			TIntArrayList liabilityIndices = new TIntArrayList(nodePayments.size());
			for (int creditorIdx = 0; creditorIdx < nodePayments.size(); creditorIdx++) {
				float liabAmt = nodePayments.get(creditorIdx).getLiabilityAmount();
				liabilityAmounts.add(liabAmt);
				int liabIdx = nodePayments.get(creditorIdx).getRecipientIndex();
				liabilityIndices.add(liabIdx);

				// add liabilities to recipient's receivables so we can calculate exogeneous
				// cash flow. N.B. exogeneous cash flow calculated below after all liabilities
				// are set
				receivableFromAnotherAgent.set(liabIdx, receivableFromAnotherAgent.get(liabIdx) + liabAmt);
			}
			liabilityAmounts.trimToSize();
			liabilityIndices.trimToSize();
			this.liabilitiesAmounts.set(paymentClearingIndex, liabilityAmounts);
			this.liabilitiesIndices.set(paymentClearingIndex, liabilityIndices);

			// calculate liquid assets
			float liquid = BUFFER_RBA; // buffer so RBA never defaults
			this.liquidAssets.set(paymentClearingIndex, liquid);
		}

		/*
		 * SET EXOGENEOUS CASH FLOW
		 * 
		 * Take the cash income from the agent's P&L, and subtract the amounts
		 * receivable from other agents. The difference is the agent's exogeneous cash
		 * flow income.
		 * 
		 * TODO Exogeneous cash flow expenses must be handled via a dummy agent because
		 * the CPV needs to be able to apply pro-rata payments to the exogeneous cash
		 * flow expenses too if the agent defaults.
		 */
		// create CSV file header
		DecimalFormat wholeNumber = new DecimalFormat("000");
		DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_EXOGENEOUS_" + wholeNumber.format(iteration)
				+ ".csv";
		String[] entries = { "IterationNo", "AgentType", "ExogenousIncome", "TotalIncome" };
		Writer writer;
		// households
		if (iteration == 0) {
			try {
				writer = new FileWriter(filename);
				ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
				csvWriter.writeNext(entries); // write header row
				for (Household household : this.households) {
					int paymentClearingIndex = household.getPaymentClearingIndex();

					// calculate exogeneous cash flow (i.e. not from another Agent)
					float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
					float calibratedIncome = household.getGrossIncome();
					float exogeneousIncome = calibratedIncome - receivable;
					this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);

					// save ratio of exogeneous to total income to CSV so it can be graphed
					entries = new String[] { String.valueOf(iteration), "H", decimalFormat.format(exogeneousIncome),
							decimalFormat.format(calibratedIncome) };
					csvWriter.writeNext(entries);
				}
				writer.close();
			} catch (IOException e) {
				// new FileWriter
				e.printStackTrace();
			} finally {
				writer = null;
			}
		} else {
			for (Household household : this.households) {
				int paymentClearingIndex = household.getPaymentClearingIndex();

				// calculate exogeneous cash flow (i.e. not from another Agent)
				float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
				float calibratedIncome = household.getGrossIncome();
				float exogeneousIncome = calibratedIncome - receivable;
				this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);
			}
		}

		// businesses
		if (iteration == 0) {
			try {
				// FIXME: this should append, not overwrite
				writer = new FileWriter(filename);
				ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
				for (Business business : this.businesses) {
					int paymentClearingIndex = business.getPaymentClearingIndex();

					// calculate exogeneous cash flow (i.e. not from another Agent)
					float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
					float calibratedIncome = business.getTotalIncome();
					float exogeneousIncome = calibratedIncome - receivable;
					this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);

					// save ratio of exogeneous to total income to CSV so it can be graphed
					entries = new String[] { String.valueOf(iteration), "B", decimalFormat.format(exogeneousIncome),
							decimalFormat.format(calibratedIncome) };
					csvWriter.writeNext(entries);
				}
				writer.close();
			} catch (IOException e) {
				// new FileWriter
				e.printStackTrace();
			} finally {
				writer = null;
			}
		} else {
			for (Business business : this.businesses) {
				int paymentClearingIndex = business.getPaymentClearingIndex();

				// calculate exogeneous cash flow (i.e. not from another Agent)
				float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
				float calibratedIncome = business.getTotalIncome();
				float exogeneousIncome = calibratedIncome - receivable;
				this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);
			}
		}

		// ADIs
		if (iteration == 0) {
			try {
				// FIXME: this should append, not overwrite
				writer = new FileWriter(filename);
				ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
				for (AuthorisedDepositTakingInstitution adi : this.adis) {
					int paymentClearingIndex = adi.getPaymentClearingIndex();

					// calculate exogeneous cash flow (i.e. not from another Agent)
					float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
					float calibratedIncome = adi.getTotalIncome();
					float exogeneousIncome = calibratedIncome - receivable;
					this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);

					// save ratio of exogeneous to total income to CSV so it can be graphed
					entries = new String[] { String.valueOf(iteration), "A", decimalFormat.format(exogeneousIncome),
							decimalFormat.format(calibratedIncome) };
					csvWriter.writeNext(entries);
				}
				writer.close();
			} catch (IOException e) {
				// new FileWriter
				e.printStackTrace();
			} finally {
				writer = null;
			}
		} else {
			for (AuthorisedDepositTakingInstitution adi : this.adis) {
				int paymentClearingIndex = adi.getPaymentClearingIndex();

				// calculate exogeneous cash flow (i.e. not from another Agent)
				float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
				float calibratedIncome = adi.getTotalIncome();
				float exogeneousIncome = calibratedIncome - receivable;
				this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);
			}
		}

		// government
		{
			int paymentClearingIndex = this.government.getPaymentClearingIndex();
			// N.B. The government is assumed to never default.
			// Whatever it is short by it simply borrows.

			// calculate exogeneous cash flow (i.e. not from another Agent)
			float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
			float calibratedIncome = this.government.getTotalIncome();
			float exogeneousIncome = calibratedIncome - receivable;
			this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);
		}

		// RBA
		{
			int paymentClearingIndex = this.rba.getPaymentClearingIndex();
			// N.B. The RBA is assumed to never default.
			// Whatever it is short by it simply borrows.

			// calculate exogeneous cash flow (i.e. not from another Agent)
			float receivable = receivableFromAnotherAgent.get(paymentClearingIndex);
			float calibratedIncome = this.rba.getTotalIncome();
			float exogeneousIncome = calibratedIncome - receivable;
			this.operatingCashFlow.set(paymentClearingIndex, exogeneousIncome);
		}
	}

	/**
	 * Takes the output of the Payments Clearing Vector algorithm and updates the
	 * status and financial statements of the agents involved.
	 * 
	 * @param cpvOutput - ClearingPaymentVector output map
	 */
	void processPaymentsClearingVectorOutputs() {
		// unmarshall CPV outputs into their original data structures
		// net cash flow of each node after paying liabilities
		TFloatArrayList equityOfNode = this.clearingPaymentVectorOutput.getEquityOfNode();
		// Which round of the CPV algorithm caused the node to default (0 = no default)
		TIntArrayList defaultOrderOfNode = this.clearingPaymentVectorOutput.getDefaultOrderOfNode();
		int iteration = this.clearingPaymentVectorOutput.getIteration();

		// update agents
		for (int cpvIdx = 0; cpvIdx < equityOfNode.size(); cpvIdx++) {
			if (cpvIdx == 0) {
				// government
				this.government.processClearingPaymentVectorOutput(equityOfNode.get(cpvIdx), iteration,
						defaultOrderOfNode.get(cpvIdx));
			} else if (cpvIdx == 1) {
				// RBA
				this.rba.processClearingPaymentVectorOutput(equityOfNode.get(cpvIdx), iteration,
						defaultOrderOfNode.get(cpvIdx));
			} else if (cpvIdx < (this.households.length + 2)) {
				// households
				this.households[cpvIdx - 2].processClearingPaymentVectorOutput(equityOfNode.get(cpvIdx), iteration,
						defaultOrderOfNode.get(cpvIdx));
			} else if (cpvIdx < (this.businesses.length + this.households.length + 2)) {
				// businesses
				this.businesses[cpvIdx - 2 - this.households.length].processClearingPaymentVectorOutput(
						equityOfNode.get(cpvIdx), iteration, defaultOrderOfNode.get(cpvIdx));
			} else if (cpvIdx < (this.adis.length + this.businesses.length + this.households.length + 2)) {
				// ADIs
				int adiStatus = this.adis[cpvIdx - 2 - this.households.length - this.businesses.length]
						.processClearingPaymentVectorOutput(equityOfNode.get(cpvIdx), iteration,
								defaultOrderOfNode.get(cpvIdx));
				if (adiStatus == -1) {
					// assign ADI customers to other ADIs if it defaults
					this.reassignCustomersToAnotherAdi(
							this.adis[cpvIdx - 2 - this.households.length - this.businesses.length]);
				}
			} else {
				// foreign countries
				this.countries[cpvIdx - 2 - this.households.length - this.businesses.length - this.adis.length]
						.processClearingPaymentVectorOutput(equityOfNode.get(cpvIdx), iteration,
								defaultOrderOfNode.get(cpvIdx));
			}
		}
	}

	/**
	 * Assigns customers of a failed ADI to another ADI. Assumes the FCS has already
	 * been applied, so doesn't change deposit balances - just transfers them to the
	 * new ADI.
	 * 
	 * Retail depositors (i.e. Households) are assigned randomly, weighted using the
	 * deposit balances of the other ADIs.
	 * 
	 * Businesses are assigned randomly, weighted using the business loan balances
	 * of the other ADIs.
	 * 
	 * @param failedAdi
	 */
	private void reassignCustomersToAnotherAdi(AuthorisedDepositTakingInstitution failedAdi) {
		if (this.random == null) {
			this.random = this.properties.getRandom();
		}

		// Assign businesses randomly to new ADI, weighted by business loan balance

		// link to ADI (loans & deposits with same ADI)
		// get total business loans for entire ADI industry (bankrupt ADI is already
		// zeroed out)
		float totalLoanBal = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsLoansBusiness()).sum();
		// populate indices with relative amounts of each ADI
		ArrayList<Business> businessesToReallocate = failedAdi.getCommercialDepositors();
		ArrayList<Integer> shuffledIndices = null;
		int nextShuffledIdx = 0;
		if (businessesToReallocate != null) {
			shuffledIndices = new ArrayList<Integer>(businessesToReallocate.size());
			for (int i = 0; i < this.adis.length; i++) {
				if (this.adis[i].getPaymentClearingIndex() != failedAdi.getPaymentClearingIndex()) {
					// calculate ratio of ADI to total
					float adiLoanBal = this.adis[i].getBsLoansBusiness();
					if (adiLoanBal > 0f) {
						// convert to indices, rounding up so we have at least enough
						int adiCustomerCount = (int) Math
								.ceil(adiLoanBal / totalLoanBal * businessesToReallocate.size());
						shuffledIndices.addAll(Collections.nCopies(adiCustomerCount, i));
					}
				}
			}
			// shuffle indices, and assign ADIs to Businesses
			Collections.shuffle(shuffledIndices, this.random);
			nextShuffledIdx = 0;
			for (int i = 0; i < businessesToReallocate.size(); i++) {
				// Assign loan ADI to Business. It doesn't matter if the Business doesn't have a
				// loan. We assume businesses only bank with ADIs who have business loans.
				AuthorisedDepositTakingInstitution adi = this.adis[shuffledIndices.get(nextShuffledIdx++)];
				businessesToReallocate.get(i).setAdi(adi);

				// add business to ADI and increase loan & deposit balances
				adi.addCommercialDepositor(businessesToReallocate.get(i)); // adds link to business
				float businessDepositBalance = adi.getBsDepositsAtCall();
				adi.setBsDepositsAtCall(businessDepositBalance + businessesToReallocate.get(i).getBankDeposits());
				float businessLoanBalance = adi.getBsLoansBusiness();
				adi.setBsLoansBusiness(businessLoanBalance + businessesToReallocate.get(i).getLoans());
				// TODO update ADI's RWA? Or is it calculated elsewhere?
			}
			// release memory
			if (shuffledIndices != null) {
				shuffledIndices.clear();
				shuffledIndices = null;
			}
		}

		////////////////////////////////////////////////////////////////////////////////////
		// FIXME assign retail depositors & borrowers (Households) randomly to new ADI,
		// weighted by deposit balance

		// link to retail depositors (Households)
		// get total deposits for entire ADI industry (bankrupt ADI is already zeroed
		// out)
		float totalDepositBal = (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsDepositsAtCall())
				.sum();
		totalDepositBal += (float) Arrays.asList(this.adis).stream().mapToDouble(o -> o.getBsDepositsTerm()).sum();
		// populate indices with relative amounts of each ADI
		shuffledIndices = new ArrayList<Integer>(this.households.length);
		for (int i = 0; i < this.adis.length; i++) {
			// calculate ratio of ADI to total
			float adiDepositBal = this.adis[i].getBsDepositsAtCall() + this.adis[i].getBsDepositsTerm();
			if (adiDepositBal > 0f) {
				// convert to indices, rounding up so we have at least enough
				int adiCustomerCount = (int) Math.ceil(adiDepositBal / totalDepositBal * this.households.length);
				shuffledIndices.addAll(Collections.nCopies(adiCustomerCount, i));
			}
		}
		// shuffle indices, and assign ADIs to Households
		shuffledIndices.trimToSize();
		Collections.shuffle(shuffledIndices, this.random);
		nextShuffledIdx = 0;
		for (int i = 0; i < this.households.length; i++) {
			if (this.households[i].getBsLoans() > 0f) {
				// assign depositor Household to ADI
				this.adis[shuffledIndices.get(nextShuffledIdx++)].addRetailDepositor(this.households[i]);
				nextShuffledIdx = (nextShuffledIdx + 1) % shuffledIndices.size();
			}
		}
		// release memory
		shuffledIndices.clear();
		shuffledIndices = null;
	}

	/**
	 * Saves a summary of the economy and agents to a file for easier (and faster)
	 * analysis.
	 * 
	 * @param iteration
	 * @return
	 */
	public Set<String> saveSummaryToFile(int iteration, String scenarioName) {
		Set<String> filenames = new HashSet<String>((int) Math.ceil(8 / 0.75) + 1);

		String filename = this.saveGovernmentSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveRbaSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveAdiSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveForeignCountrySummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveBusinessSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveHouseholdSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveHouseholdIncomeBySourceSummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveHouseholdSalaryByIndustrySummaryToFile(iteration, scenarioName);
		filenames.add(filename);
		// filename = this.saveIndividualSummaryToFile(iteration, scenarioName);
		// filenames.add(filename);

		filename = this.saveCurrencySummaryToFile(iteration, scenarioName);
		filenames.add(filename);

		return filenames;
	}

	/**
	 * Saves every single Agent to file.
	 * 
	 * @return a Set of the filenames for each Agent class
	 */
	public Set<String> saveDetailsToFile(int iteration, String scenarioName) {
		Set<String> filenames = new HashSet<String>((int) Math.ceil(8 / 0.75) + 1);

		String filename = this.saveGovernmentDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveRbaDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveAdiDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveForeignCountryDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveBusinessDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveHouseholdDetailsToFile(iteration, scenarioName);
		filenames.add(filename);
		filename = this.saveIndividualDetailsToFile(iteration, scenarioName);
		filenames.add(filename);

		filename = this.saveCurrencyDetailsToFile(iteration, scenarioName);
		filenames.add(filename);

		return filenames;
	}

	/**
	 * Save Household to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveHouseholdDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.households[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_Household_"
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
	 * Save Household summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveHouseholdSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.households[0].toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR))
						.split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Household_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.households.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.households[row].toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	 * Save Household income by source to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveHouseholdIncomeBySourceSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.households[0].toCsvIncomeBySourceStringHeaders(Properties.CSV_SEPARATOR))
						.split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Household_IncomeBySource_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.households.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.households[row].toCsvIncomeBySourceString(Properties.CSV_SEPARATOR, iteration))
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
	 * Save Household salary by industry division to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveHouseholdSalaryByIndustrySummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.households[0].toCsvSalaryByIndustryStringHeaders(Properties.CSV_SEPARATOR))
						.split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Household_SalaryByIndustry_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.households.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.households[row].toCsvSalaryByIndustryString(Properties.CSV_SEPARATOR, iteration))
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
	public String saveIndividualDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.individuals[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_Individual_"
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
	 * Save Individual summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveIndividualSummaryToFile(int iteration, String scenarioName) {
		// do nothing for now
		// probably won't really need this method
		// might want to analyse demographics, but can do that later
		return "";
	}

	/**
	 * Save Business to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveBusinessDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.businesses[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_Business_"
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
	 * Save Business summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveBusinessSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.businesses[0].toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR))
						.split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Business_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.businesses.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.businesses[row].toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	public String saveAdiDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.adis[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_ADI_" + wholeNumber.format(iteration)
				+ ".csv";
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
	 * Save AuthorisedDepositTakingInstitution to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveAdiSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.adis[0].toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_ADI_" + wholeNumber.format(iteration)
				+ ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.adis.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.adis[row].toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	public String saveForeignCountryDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.countries[0].toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_ForeignCountry_"
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
	 * Save ForeignCountry summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveForeignCountrySummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.countries[0].toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR))
						.split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_ForeignCountry_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			for (int row = 0; row < this.countries.length; row++) {
				entries = (iteration + Properties.CSV_SEPARATOR
						+ this.countries[row].toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	public String saveCurrencyDetailsToFile(int iteration, String scenarioName) {
		List<Currencies> beans = Arrays.asList(this.currencies);
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_Currencies_"
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
	 * Save Currencies summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveCurrencySummaryToFile(int iteration, String scenarioName) {
		// get data
		Map<String, Currency> currenciesMap = this.currencies.getAllCurrencies();

		// save CSV file
		boolean headerWritten = false;
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Currencies_"
				+ wholeNumber.format(iteration) + ".csv";
		Writer writer;
		String[] entries = null;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries); // writes column titles
			for (String currencyCode : currenciesMap.keySet()) {
				if (!headerWritten) {
					entries = ("IterationNo" + Properties.CSV_SEPARATOR
							+ currenciesMap.get(currencyCode).toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR))
									.split(Properties.CSV_SEPARATOR);
					headerWritten = true;
				}
				entries = (iteration + Properties.CSV_SEPARATOR
						+ currenciesMap.get(currencyCode).toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	 * Save ReserveBankOfAustralia to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveRbaDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.rba.toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_RBA_" + wholeNumber.format(iteration)
				+ ".csv";
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
	 * Save ReserveBankOfAustralia to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveRbaSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.rba.toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_RBA_" + wholeNumber.format(iteration)
				+ ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			entries = (iteration + Properties.CSV_SEPARATOR
					+ this.rba.toCsvSummaryString(Properties.CSV_SEPARATOR, iteration)).split(Properties.CSV_SEPARATOR);
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
	public String saveGovernmentDetailsToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.government.toCsvStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_Agents_Govt_" + wholeNumber.format(iteration)
				+ ".csv";
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
	 * Save AustralianGovernment summary to CSV.
	 * 
	 * @return the output filename
	 */
	public String saveGovernmentSummaryToFile(int iteration, String scenarioName) {
		// get data
		String[] entries = ("IterationNo" + Properties.CSV_SEPARATOR
				+ this.government.toCsvSummaryStringHeaders(Properties.CSV_SEPARATOR)).split(Properties.CSV_SEPARATOR);

		// save CSV file
		DecimalFormat wholeNumber = new DecimalFormat("000");
		String filename = Properties.OUTPUT_DIRECTORY + scenarioName + "_SUMMARY_Govt_" + wholeNumber.format(iteration)
				+ ".csv";
		Writer writer;
		try {
			writer = new FileWriter(filename);
			ICSVWriter csvWriter = new CSVWriterBuilder(writer).build();
			csvWriter.writeNext(entries);
			entries = (iteration + Properties.CSV_SEPARATOR
					+ this.government.toCsvSummaryString(Properties.CSV_SEPARATOR, iteration))
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
	 * @param properties the properties to set
	 */
	@Autowired
	public void setProperties(Properties properties) {
		this.properties = properties;
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
