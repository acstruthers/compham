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

import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;

/**
 * @author acstr
 *
 */
public class Main {

	public static final String FILEPATH_AGENTS_INIT = "C:\\tmp\\Agents\\Agents_init.ser";

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
		writeObjectToFile(economy, FILEPATH_AGENTS_INIT);
		economy.close();
		economy = null;
		System.gc();

		long memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
		megabytesBefore = memoryBefore / 1024f / 1024f;
		float megabytesAfter = memoryAfter / 1024f / 1024f;
		System.out.println("################################################");
		System.out.println(new Date(System.currentTimeMillis())
				+ ": MEMORY USAGE IN MAIN AFTER MAKING CPV INPUTS AND DROPPING AGENTS: "
				+ formatter.format(megabytesBefore) + "MB");
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
		economy = (AustralianEconomy) readObjectFromFile(FILEPATH_AGENTS_INIT);

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
}
