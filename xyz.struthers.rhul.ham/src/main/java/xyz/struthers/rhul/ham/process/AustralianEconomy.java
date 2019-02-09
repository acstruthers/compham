/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.Serializable;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
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

	// Agents
	Household[] households;
	Individual[] individuals;
	Business[] businesses;
	AuthorisedDepositTakingInstitution[] adis;
	ForeignCountry[] countries;
	Currencies currencies;
	ReserveBankOfAustralia rba;
	AustralianGovernment government;

	/**
	 * 
	 */
	public AustralianEconomy() {
		super();
		this.init();
	}

	@PostConstruct
	private void init() {
		this.households = null;
		this.individuals = null;
		this.businesses = null;
		this.adis = null;
		this.countries = null;
		this.currencies = null;
		this.rba = null;
		this.government = null;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 */
	@PreDestroy
	public void close() {
		// TODO: implement me
	}

	/**
	 * @return the households
	 */
	public Household[] getHouseholds() {
		return households;
	}

	/**
	 * @param households
	 *            the households to set
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
	 * @param individuals
	 *            the individuals to set
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
	 * @param businesses
	 *            the businesses to set
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
	 * @param adis
	 *            the adis to set
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
	 * @param countries
	 *            the countries to set
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
	 * @param currencies
	 *            the currencies to set
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
	 * @param rba
	 *            the rba to set
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
	 * @param government
	 *            the government to set
	 */
	public void setGovernment(AustralianGovernment government) {
		this.government = government;
	}

}
