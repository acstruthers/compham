/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Map;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.nqzero.permit.Permit;

import xyz.struthers.rhul.ham.agent.AustralianGovernment;
import xyz.struthers.rhul.ham.agent.AuthorisedDepositTakingInstitution;
import xyz.struthers.rhul.ham.agent.Business;
import xyz.struthers.rhul.ham.agent.ForeignCountry;
import xyz.struthers.rhul.ham.agent.Household;
import xyz.struthers.rhul.ham.agent.Individual;
import xyz.struthers.rhul.ham.agent.ReserveBankOfAustralia;
import xyz.struthers.rhul.ham.data.Currencies;
import xyz.struthers.rhul.ham.data.Currency;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.Clearable;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentVector;
import xyz.struthers.rhul.ham.process.Employer;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * @author acstr
 *
 */
public class Main {

	public static final String FILEPATH_AGENTS_INIT = "C:\\tmp\\Agents\\Agents_init.ser";
	public static final String FILEPATH_AGENTS_INIT_FST = "C:\\tmp\\Agents\\Agents_init.fst";
	public static final String FILEPATH_AGENTS_INIT_KRYO = "C:\\tmp\\Agents\\Agents_init.kryo";

	public Main() {
		super();
	}

	/**
	 * Runs the simulation in a series of stages, releasing memory as it goes so it
	 * will run on a computer with 32GB of RAM.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		// work around Java 9+ reflection illegal access exceptions
		Permit.godMode();

		DecimalFormat formatter = new DecimalFormat("#,##0.00");
		long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		float megabytesBefore = memoryBefore / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AT BEGINNING: "
				+ formatter.format(megabytesBefore) + "MB");
		System.out.println("################################################");

		InitialiseEconomy init = new InitialiseEconomy();
		ClearingPaymentInputs cpvInputs = init.initialiseEconomy();
		AustralianEconomy economy = init.getEconomy();
		// writeObjectToFile(economy, FILEPATH_AGENTS_INIT);
		// writeFstEconomyToFile(economy, FILEPATH_AGENTS_INIT_FST); // using FST
		// serialization
		writeKryoObjectToFile(economy, FILEPATH_AGENTS_INIT_KRYO);

		economy.close();
		economy = null;
		System.gc();

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesBefore = memoryBefore / 1024f / 1024f;
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis())
				+ ": MEMORY USAGE IN MAIN AFTER MAKING CPV INPUTS AND DROPPING AGENTS: "
				+ formatter.format(megabytesAfter) + "MB");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		int iteration = 0;
		RunSimulation sim = new RunSimulation();
		Map<String, Object> cpvOutputs = sim.calculateClearingPaymentVector(cpvInputs.getLiabilitiesAmounts(),
				cpvInputs.getLiabilitiesIndices(), cpvInputs.getOperatingCashFlow(), iteration);

		cpvInputs.clear();
		cpvInputs = null;
		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER CALCULATING CPV: "
				+ formatter.format(megabytesAfter) + "MB)");
		System.out.println("################################################");
		memoryBefore = memoryAfter;

		// TODO process CPV outputs
		// read agents back in from object file, then update financial statements
		// economy = (AustralianEconomy) readObjectFromFile(FILEPATH_AGENTS_INIT);
		// economy = readFstEconomyFromFile(FILEPATH_AGENTS_INIT_FST); // using FST
		economy = readKryoObjectFromFile(FILEPATH_AGENTS_INIT_KRYO);

		System.gc();
		memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AFTER DESERIALIZING ECONOMY: "
				+ formatter.format(megabytesAfter) + "MB)");
		System.out.println("################################################");
		memoryBefore = memoryAfter;
	}

	/**
	 * Writes objects to file. This helps the program fit on a computer with only
	 * 32GB of RAM.
	 * 
	 * SOURCE:
	 * https://examples.javacodegeeks.com/core-java/io/fileoutputstream/how-to-write-an-object-to-file-in-java/
	 * 
	 * @param serObj   - the object to be serialized to file.
	 * @param filePath - the full file path to save the serialized object to.
	 * @author Byron Kiourtzoglou (javacodegeeks.com)
	 */
	public static void writeObjectToFile(Object serObj, String filePath) {
		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			ObjectOutputStream objectOut = new ObjectOutputStream(fileOut);
			objectOut.writeObject(serObj);
			objectOut.close();
			System.out.println("The Object was succesfully written to the file: " + filePath);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void writeKryoObjectToFile(AustralianEconomy serObj, String filePath) {
		Kryo kryo = new Kryo();
		kryo.register(AustralianEconomy.class);
		kryo.register(int[].class);
		kryo.register(Household[].class);
		kryo.register(Individual[].class);
		kryo.register(Business[].class);
		kryo.register(AuthorisedDepositTakingInstitution[].class);
		kryo.register(ForeignCountry[].class);
		kryo.register(Individual[].class);
		kryo.register(AustralianGovernment.class);
		kryo.register(AuthorisedDepositTakingInstitution.class);
		kryo.register(Business.class);
		kryo.register(ForeignCountry.class);
		kryo.register(Household.class);
		kryo.register(Individual.class);
		kryo.register(ReserveBankOfAustralia.class);
		kryo.register(Currencies.class);
		kryo.register(Currency.class);
		kryo.register(ClearingPaymentVector.class);
		kryo.register(ClearingPaymentInputs.class);
		kryo.register(NodePayment.class);
		kryo.register(Employer.class);
		kryo.register(Clearable.class);
		kryo.register(java.util.ArrayList.class);
		kryo.register(java.util.HashMap.class);

		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			Output output = new Output(fileOut);
			kryo.writeObject(output, serObj);
			output.close();
			System.out.println("The Object was succesfully written to the file: " + filePath);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * FST has a 1.3GB limit per object (int size limit).
	 * 
	 * @param serObj
	 * @param filePath
	 * 
	 *                 deprecated doesn't work in Java 9+ yet.
	 */
	public static void writeFstEconomyToFile(Object serObj, String filePath) {
		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			FSTObjectOutput objectOut = new FSTObjectOutput(fileOut);
			objectOut.writeObject(serObj, AustralianEconomy.class);
			objectOut.close();
			System.out.println("The Object was succesfully written to the file using FST: " + filePath);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Allows objects to be read back in from file so we can continue working with
	 * them after freeing up enough RAM.
	 * 
	 * SOURCE:
	 * https://examples.javacodegeeks.com/core-java/io/file/how-to-read-an-object-from-file-in-java/
	 * 
	 * @param filePath - the full file path to open the serialized object from.
	 * @return the deserialized object (as an Object, which needs to be cast to the
	 *         correct class).
	 * @author Ilias Tsagklis (javacodegeeks.com)
	 */
	public static Object readObjectFromFile(String filePath) {
		Object obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			ObjectInputStream objectIn = new ObjectInputStream(fileIn);
			obj = objectIn.readObject();
			System.out.println("The Object has been read from the file: " + filePath);
			objectIn.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			obj = null;
		}
		return obj;
	}

	public static AustralianEconomy readKryoObjectFromFile(String filePath) {
		Kryo kryo = new Kryo();
		kryo.register(AustralianEconomy.class);
		kryo.register(int[].class);
		kryo.register(Household[].class);
		kryo.register(Individual[].class);
		kryo.register(Business[].class);
		kryo.register(AuthorisedDepositTakingInstitution[].class);
		kryo.register(ForeignCountry[].class);
		kryo.register(Individual[].class);
		kryo.register(AustralianGovernment.class);
		kryo.register(AuthorisedDepositTakingInstitution.class);
		kryo.register(Business.class);
		kryo.register(ForeignCountry.class);
		kryo.register(Household.class);
		kryo.register(Individual.class);
		kryo.register(ReserveBankOfAustralia.class);
		kryo.register(Currencies.class);
		kryo.register(Currency.class);
		kryo.register(ClearingPaymentVector.class);
		kryo.register(ClearingPaymentInputs.class);
		kryo.register(NodePayment.class);
		kryo.register(Employer.class);
		kryo.register(Clearable.class);
		kryo.register(java.util.ArrayList.class);
		kryo.register(java.util.HashMap.class);

		AustralianEconomy obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			Input input = new Input(fileIn);
			obj = kryo.readObject(input, AustralianEconomy.class);
			System.out.println("The Object has been read from the file: " + filePath);
			input.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			obj = null;
		}
		return obj;
	}

	/**
	 * FST has a 1.3GB limit per object (int size limit).
	 * 
	 * @param filePath
	 * @return
	 * 
	 * 		deprecated doesn't work in Java 9+ yet.
	 */
	public static AustralianEconomy readFstEconomyFromFile(String filePath) {
		AustralianEconomy obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			FSTObjectInput objectIn = new FSTObjectInput(fileIn);
			obj = (AustralianEconomy) objectIn.readObject(AustralianEconomy.class);
			System.out.println("The Object has been read from the file: " + filePath);
			objectIn.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			obj = null;
		}
		return obj;
	}
}
