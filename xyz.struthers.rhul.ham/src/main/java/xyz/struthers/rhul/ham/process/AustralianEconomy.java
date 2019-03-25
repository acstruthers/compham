/**
 * 
 */
package xyz.struthers.rhul.ham.process;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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

	// Agents
	Household[] households;
	Individual[] individuals;
	Business[] businesses;
	AuthorisedDepositTakingInstitution[] adis;
	ForeignCountry[] countries;
	Currencies currencies;
	ReserveBankOfAustralia rba;
	AustralianGovernment government;

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

		// Analytics
		this.businessTypeCount = null;
	}

	/**
	 * Deletes all the field variables, freeing up memory.
	 */
	@PreDestroy
	public void close() {
		// TODO: implement me
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

		// filename = this.saveCurrencyDetailsToFile(iteration);
		// filenames.add(filename);

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
