/**
 * 
 */
package xyz.struthers.rhul.ham.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

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

	private CalibrationData data;
	private Map<String, Map<String, String>> allAdiData;
	private List<AuthorisedDepositTakingInstitution> adiAgents;
	private AustralianEconomy economy;

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
		for (String key : adiKeySet) {
			// Company details
			String australianBusinessNumber = key;
			String shortName = this.allAdiData.get(key).get("Short Name");
			String longName = this.allAdiData.get(key).get("Name");
			String adiCategory = this.allAdiData.get(key).get("Diss Model Category");

			// create map to hold financial statements
			Map<String, Double> financialStatement = new HashMap<String, Double>();

			// P&L
			financialStatement.put("pnlInterestIncome",
					Double.valueOf(this.allAdiData.get(key).get("Interest income")));
			financialStatement.put("pnlInterestExpense",
					Double.valueOf(this.allAdiData.get(key).get("Interest expense")));
			financialStatement.put("pnlTradingIncome", Double.valueOf(this.allAdiData.get(key).get("Trading Income")));
			financialStatement.put("pnlInvestmentIncome",
					Double.valueOf(this.allAdiData.get(key).get("Investment income")));
			financialStatement.put("pnlOtherIncome", Double.valueOf(this.allAdiData.get(key).get("Other income")));

			financialStatement.put("pnlPersonnelExpenses", Double.valueOf(this.allAdiData.get(key).get("Personnel")));
			financialStatement.put("pnlLoanImpairmentExpense",
					Double.valueOf(this.allAdiData.get(key).get("Bad & doubtful debts")));
			financialStatement.put("pnlDepreciationAmortisation",
					Double.valueOf(this.allAdiData.get(key).get("Depreciation & Amortisation")));
			financialStatement.put("pnlOtherExpenses", Double.valueOf(this.allAdiData.get(key).get("Other expenses")));

			financialStatement.put("pnlIncomeTaxExpense", Double.valueOf(this.allAdiData.get(key).get("Income Tax")));

			// Bal Sht
			financialStatement.put("bsCash", Double.valueOf(this.allAdiData.get(key).get("Cash")));
			financialStatement.put("bsTradingSecurities",
					Double.valueOf(this.allAdiData.get(key).get("Trading Securities")));
			financialStatement.put("bsDerivativeAssets",
					Double.valueOf(this.allAdiData.get(key).get("Derivative Assets")));
			financialStatement.put("bsInvestments",
					Double.valueOf(this.allAdiData.get(key).get("Investment Securities")));
			financialStatement.put("bsLoansPersonal", Double.valueOf(this.allAdiData.get(key).get("Personal Loans")));
			financialStatement.put("bsLoansHome", Double.valueOf(this.allAdiData.get(key).get("Home Loans")));
			financialStatement.put("bsLoansBusiness", Double.valueOf(this.allAdiData.get(key).get("Business Loans")));
			financialStatement.put("bsLoansADI", Double.valueOf(this.allAdiData.get(key).get("ADI Loans")));
			financialStatement.put("bsLoansGovernment",
					Double.valueOf(this.allAdiData.get(key).get("Government Loans")));
			financialStatement.put("bsOtherNonFinancialAssets",
					Double.valueOf(this.allAdiData.get(key).get("Non-financial assets")));

			financialStatement.put("bsDepositsAtCall", Double.valueOf(this.allAdiData.get(key).get("At Call")));
			financialStatement.put("bsDepositsTerm", Double.valueOf(this.allAdiData.get(key).get("TD/CD")));
			financialStatement.put("bsDepositsAdiRepoEligible",
					Double.valueOf(this.allAdiData.get(key).get("Repo/ADI")));
			financialStatement.put("bsDerivativeLiabilities",
					Double.valueOf(this.allAdiData.get(key).get("Derivative Liabilities")));
			financialStatement.put("bsBondsNotesBorrowings",
					Double.valueOf(this.allAdiData.get(key).get("Bonds, notes & borrowings")));
			financialStatement.put("bsOtherLiabilities",
					Double.valueOf(this.allAdiData.get(key).get("Other liabilities")));

			financialStatement.put("bsRetainedEarnings",
					Double.valueOf(this.allAdiData.get(key).get("Retained Earnings")));
			financialStatement.put("bsReserves", Double.valueOf(this.allAdiData.get(key).get("Reserves")));
			financialStatement.put("bsOtherEquity", Double.valueOf(this.allAdiData.get(key).get("Other")));

			// Metrics
			financialStatement.put("rateCash", Double.valueOf(this.allAdiData.get(key).get("Cash Rate")));
			financialStatement.put("rateTrading",
					Double.valueOf(this.allAdiData.get(key).get("Trading Security Rate")));
			financialStatement.put("rateInvestment",
					Double.valueOf(this.allAdiData.get(key).get("Investment Security Rate")));
			financialStatement.put("rateAdiLoan", Double.valueOf(this.allAdiData.get(key).get("ADI Loan Rate")));
			financialStatement.put("rateGovernmentLoan",
					Double.valueOf(this.allAdiData.get(key).get("Government Loan Rate")));
			financialStatement.put("rateTotalLoans", Double.valueOf(this.allAdiData.get(key).get("TOTAL Loan Rate")));
			financialStatement.put("rateTotalDeposits", Double.valueOf(this.allAdiData.get(key).get("Deposit Rate")));
			financialStatement.put("rateBondsNotesBorrowings",
					Double.valueOf(this.allAdiData.get(key).get("Bonds, notes & borrowings Rate")));
			financialStatement.put("capitalTotalRatio",
					Double.valueOf(this.allAdiData.get(key).get("Total Capital %")));
			financialStatement.put("capitalTotalAmount", Double.valueOf(this.allAdiData.get(key).get("Total Capital")));
			financialStatement.put("capitalTotalRWA", Double.valueOf(this.allAdiData.get(key).get("Credit RWA")));
			financialStatement.put("capitalCreditRWA", Double.valueOf(this.allAdiData.get(key).get("RWA")));

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
			this.adiAgents.add(adi);
		}

		this.addAgentsToEconomy();
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
	 * @param data
	 *            the calibration data to set
	 */
	@Autowired
	public void setData(CalibrationData data) {
		this.data = data;
	}

	/**
	 * @param economy
	 *            the economy to set
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
