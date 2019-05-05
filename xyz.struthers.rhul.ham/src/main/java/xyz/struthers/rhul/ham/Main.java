/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

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
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.config.SpringConfiguration;
import xyz.struthers.rhul.ham.data.Currencies;
import xyz.struthers.rhul.ham.data.Currency;
import xyz.struthers.rhul.ham.process.AustralianEconomy;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentVector;
import xyz.struthers.rhul.ham.process.NodePayment;

/**
 * @author acstr
 *
 */
public class Main {

	public static final String RMI_HOST = "Adam-E590";
	public static final int RMI_PORT = 1099;

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

		try {
			DecimalFormat formatter = new DecimalFormat("#,##0.00");
			long memoryBefore = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			float megabytesBefore = memoryBefore / 1024f / 1024f;
			System.out.println("################################################");
			System.out.println(new Date(System.currentTimeMillis()) + ": MEMORY USAGE IN MAIN AT BEGINNING: "
					+ formatter.format(megabytesBefore) + "MB");
			System.out.println("################################################");

			AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext(SpringConfiguration.class);
			InitialiseEconomy init = new InitialiseEconomy();
			ClearingPaymentInputs cpvInputs = init.initialiseEconomy(ctx);
			AustralianEconomy economy = init.getEconomy();
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
			// Get RMI registry for Clearing Payment Vector calculation
			Registry registry = LocateRegistry.getRegistry(Properties.RMI_HOST, Properties.RMI_PORT);
			ClearingPaymentVectorInterface stub = (ClearingPaymentVectorInterface) registry
					.lookup("ClearingPaymentVector");
			System.out.println(new Date(System.currentTimeMillis()) + ": Invoking CPV via RMI.");
			// using default Java RMI with compressed objects
			ByteArrayOutputStream baos = null;
			try {
				// compress outputs
				baos = new ByteArrayOutputStream(Properties.NETWORK_BUFFER_BYTES);
				GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
				ObjectOutputStream objectOut = new ObjectOutputStream(gzipOut);
				objectOut.writeObject(cpvInputs);
				objectOut.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			byte[] cpvInputBytes = baos.toByteArray();
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs compressed to "
					+ cpvInputBytes.length + " bytes.");
			byte[] cpvOutputBytes = stub.calculate(cpvInputBytes);
			System.out.println(new Date(System.currentTimeMillis()) + ": " + cpvOutputBytes.length
					+ " bytes of output returned from CPV via RMI.");
			ClearingPaymentOutputs cpvOutputs = null;
			try {
				// uncompress CPV outputs
				ByteArrayInputStream bais = new ByteArrayInputStream(cpvOutputBytes);
				GZIPInputStream gzipIn = new GZIPInputStream(bais);
				ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
				cpvOutputs = (ClearingPaymentOutputs) objectIn.readObject();
				objectIn.close();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			}
			System.out.println(new Date(System.currentTimeMillis()) + ": CPV outputs decompressed.");

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
			// update financial statements
			economy.updateOneMonth(cpvOutputs);

			System.gc();
			memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
			megabytesAfter = memoryAfter / 1024f / 1024f;
			System.out.println("################################################");
			System.out.println(new Date(System.currentTimeMillis())
					+ ": MEMORY USAGE IN MAIN AFTER UPDATING ECONOMY WITH CPV OUTPUTS: "
					+ formatter.format(megabytesAfter) + "MB)");
			System.out.println("################################################");
			memoryBefore = memoryAfter;

			// details after being updated with CPV output
			// save data to file for later analysis
			economy.saveSummaryToFile(iteration);
			economy.saveDetailsToFile(iteration);
			iteration++;

			ctx.close();

		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
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
		kryo.register(java.util.ArrayList.class);
		kryo.register(java.util.HashMap.class);

		try {
			FileOutputStream fileOut = new FileOutputStream(filePath);
			Output output = new Output(fileOut);
			// kryo.writeObject(output, serObj); // can't handle interfaces
			kryo.writeClassAndObject(output, serObj); // should handle interfaces
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
		kryo.register(java.util.ArrayList.class);
		kryo.register(java.util.HashMap.class);

		AustralianEconomy obj = null;
		try {
			FileInputStream fileIn = new FileInputStream(filePath);
			Input input = new Input(fileIn);
			// can't handle interfaces
			// obj = kryo.readObject(input, AustralianEconomy.class);
			// should handle interfaces
			obj = (AustralianEconomy) kryo.readClassAndObject(input);
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
	 *         deprecated doesn't work in Java 9+ yet.
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
