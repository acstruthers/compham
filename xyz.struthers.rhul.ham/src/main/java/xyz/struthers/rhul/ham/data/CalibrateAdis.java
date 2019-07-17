/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import gnu.trove.map.hash.TObjectFloatHashMap;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.ForeignBank;
import xyz.struthers.rhul.ham.agent.MajorBank;
import xyz.struthers.rhul.ham.agent.MutualBank;
import xyz.struthers.rhul.ham.agent.OtherDomesticBank;
import xyz.struthers.rhul.ham.process.AustralianEconomy;

/**
 * @author Adam Struthers
 * @since 26-Jan-2019
 */
@Component
@Scope(value = "singleton")
public class CalibrateAdis {

	private static final boolean DEBUG = false;

	// beans
	private CalibrationData data;
	private AustralianEconomy economy;

	// field variables
	private Map<String, Map<String, String>> allAdiData;
	private List<AuthorisedDepositTakingInstitution> adiAgents;

	/**
	 * 
	 */
	public CalibrateAdis() {
		super();
		this.init();
	}

	/**
	 * Creates the ADI agents for the model, based on the calibration data, then
	 * adds them to the economy.
	 * 
	 * N.B. Does not take into account individual customer loans and deposits. Those
	 * are set when the Individuals are calibrated.
	 */
	public void createAdiAgents() {
		this.allAdiData = this.data.getAdiData();
		this.adiAgents = new ArrayList<AuthorisedDepositTakingInstitution>();

		Set<String> adiKeySet = new HashSet<String>(this.allAdiData.keySet());

		if (DEBUG) {
			System.out.println("adiKeySet: " + adiKeySet);
			boolean first = true;
			for (String key : adiKeySet) {
				if (first) {
					first = false;
					System.out.println("this.allAdiData.get(key).keySet(): " + this.allAdiData.get(key).keySet());
				}
			}
		}

		for (String key : adiKeySet) {
			// Company details
			String australianBusinessNumber = key;
			String shortName = this.allAdiData.get(key).get("Short Name");
			String longName = this.allAdiData.get(key).get("Name");
			String adiCategory = this.allAdiData.get(key).get("Diss Model Category");

			// create map to hold financial statements
			TObjectFloatHashMap<String> financialStatement = new TObjectFloatHashMap<String>();

			// P&L
			financialStatement.put("pnlInterestIncome", Float.valueOf(this.allAdiData.get(key).get("Interest income")));
			financialStatement.put("pnlInterestExpense",
					Float.valueOf(this.allAdiData.get(key).get("Interest expense")));
			financialStatement.put("pnlTradingIncome", Float.valueOf(this.allAdiData.get(key).get("Trading Income")));
			financialStatement.put("pnlInvestmentIncome",
					Float.valueOf(this.allAdiData.get(key).get("Investment income")));
			financialStatement.put("pnlOtherIncome", Float.valueOf(this.allAdiData.get(key).get("Other income")));

			financialStatement.put("pnlPersonnelExpenses", Float.valueOf(this.allAdiData.get(key).get("Personnel")));
			financialStatement.put("pnlLoanImpairmentExpense",
					Float.valueOf(this.allAdiData.get(key).get("Bad & doubtful debts")));
			financialStatement.put("pnlDepreciationAmortisation",
					Float.valueOf(this.allAdiData.get(key).get("Depreciation & Amortisation")));
			financialStatement.put("pnlOtherExpenses", Float.valueOf(this.allAdiData.get(key).get("Other expenses")));

			financialStatement.put("pnlIncomeTaxExpense", Float.valueOf(this.allAdiData.get(key).get("Income Tax")));

			// Bal Sht
			financialStatement.put("bsCash", Float.valueOf(this.allAdiData.get(key).get("Cash")));
			financialStatement.put("bsTradingSecurities",
					Float.valueOf(this.allAdiData.get(key).get("Trading Securities")));
			financialStatement.put("bsDerivativeAssets",
					Float.valueOf(this.allAdiData.get(key).get("Derivative Assets")));
			financialStatement.put("bsInvestments",
					Float.valueOf(this.allAdiData.get(key).get("Investment Securities")));
			financialStatement.put("bsLoansPersonal", Float.valueOf(this.allAdiData.get(key).get("Personal Loans")));
			financialStatement.put("bsLoansHome", Float.valueOf(this.allAdiData.get(key).get("Home Loans")));
			financialStatement.put("bsLoansBusiness", Float.valueOf(this.allAdiData.get(key).get("Business Loans")));
			financialStatement.put("bsLoansADI", Float.valueOf(this.allAdiData.get(key).get("ADI Loans")));
			financialStatement.put("bsLoansGovernment",
					Float.valueOf(this.allAdiData.get(key).get("Government Loans")));
			financialStatement.put("bsOtherNonFinancialAssets",
					Float.valueOf(this.allAdiData.get(key).get("Non-financial assets")));

			financialStatement.put("bsDepositsAtCall", Float.valueOf(this.allAdiData.get(key).get("At Call")));
			financialStatement.put("bsDepositsTerm", Float.valueOf(this.allAdiData.get(key).get("TD/CD")));
			financialStatement.put("bsDepositsAdiRepoEligible",
					Float.valueOf(this.allAdiData.get(key).get("Repo/ADI")));
			financialStatement.put("bsDerivativeLiabilities",
					Float.valueOf(this.allAdiData.get(key).get("Derivative Liabilities")));
			financialStatement.put("bsBondsNotesBorrowings",
					Float.valueOf(this.allAdiData.get(key).get("Bonds, notes & borrowings")));
			financialStatement.put("bsOtherLiabilities",
					Float.valueOf(this.allAdiData.get(key).get("Other liabilities")));

			financialStatement.put("bsRetainedEarnings",
					Float.valueOf(this.allAdiData.get(key).get("Retained Earnings")));
			financialStatement.put("bsReserves", Float.valueOf(this.allAdiData.get(key).get("Reserves")));
			financialStatement.put("bsOtherEquity", Float.valueOf(this.allAdiData.get(key).get("Other Equity")));

			// Metrics
			financialStatement.put("rateCash", Float.valueOf(this.allAdiData.get(key).get("Cash Rate")));
			financialStatement.put("rateTrading", Float.valueOf(this.allAdiData.get(key).get("Trading Security Rate")));
			financialStatement.put("rateInvestment",
					Float.valueOf(this.allAdiData.get(key).get("Investment Security Rate")));
			financialStatement.put("rateAdiLoan", Float.valueOf(this.allAdiData.get(key).get("ADI Loan Rate")));
			financialStatement.put("rateGovernmentLoan",
					Float.valueOf(this.allAdiData.get(key).get("Government Loan Rate")));
			financialStatement.put("rateTotalLoans", Float.valueOf(this.allAdiData.get(key).get("TOTAL Loan Rate")));
			financialStatement.put("rateTotalDeposits", Float.valueOf(this.allAdiData.get(key).get("Deposit Rate")));
			financialStatement.put("rateBondsNotesBorrowings",
					Float.valueOf(this.allAdiData.get(key).get("Bonds, notes & borrowings Rate")));
			financialStatement.put("capitalTotalRatio", Float.valueOf(this.allAdiData.get(key).get("Total Capital %")));
			financialStatement.put("capitalTotalAmount", Float.valueOf(this.allAdiData.get(key).get("Total Capital")));
			financialStatement.put("capitalTotalRWA", Float.valueOf(this.allAdiData.get(key).get("Credit RWA")));
			financialStatement.put("capitalCreditRWA", Float.valueOf(this.allAdiData.get(key).get("RWA")));

			// create ADI
			AuthorisedDepositTakingInstitution adi = null;
			if (adiCategory.equals("Mutual ADI")) {
				adi = new MutualBank(australianBusinessNumber, shortName, longName, adiCategory, financialStatement);
			} else if (adiCategory.equals("Other Domestic Bank")) {
				adi = new OtherDomesticBank(australianBusinessNumber, shortName, longName, adiCategory,
						financialStatement);
			} else if (adiCategory.equals("Foreign Bank")) {
				adi = new ForeignBank(australianBusinessNumber, shortName, longName, adiCategory, financialStatement);
			} else { // Major Bank
				adi = new MajorBank(australianBusinessNumber, shortName, longName, adiCategory, financialStatement);
			}
			adi.setIndustryDivisionCode('K'); // Financial and Insurance Services
			this.adiAgents.add(adi);
		}

		this.addAgentsToEconomy();

		// release memory
		this.data.dropAdiFinancialData();
	}

	private void addAgentsToEconomy() {
		this.economy.setAdis(this.adiAgents);
	}

	private void init() {
		this.data = null;
		this.allAdiData = null;
		this.adiAgents = null;
	}

	/**
	 * @param data the calibration data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param economy the economy to set
	 */
	@Autowired
	public void setEconomy(AustralianEconomy economy) {
		this.economy = economy;
	}

	/**
	 * @return the adiAgents
	 */
	public List<AuthorisedDepositTakingInstitution> getAdiAgents() {
		if (this.adiAgents == null) {
			this.createAdiAgents();
		}
		return adiAgents;
	}

}
