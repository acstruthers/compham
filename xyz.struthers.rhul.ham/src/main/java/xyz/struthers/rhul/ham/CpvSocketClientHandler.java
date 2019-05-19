package xyz.struthers.rhul.ham;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.List;

import xyz.struthers.io.Serialization;
import xyz.struthers.io.Serialization.CompressionType;
import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author Adam Struthers
 * @since 2019-05-16
 */
public class CpvSocketClientHandler extends Thread {

	final DataInputStream dis;
	final DataOutputStream dos;
	final Socket s;

	/**
	 * Creates CPV client handler for the given socket connection and streams
	 */
	public CpvSocketClientHandler(Socket s, DataInputStream dis, DataOutputStream dos) {
		this.s = s;
		this.dis = dis;
		this.dos = dos;
	}

	@Override
	public void run() {
		ClearingPaymentInputs cpvInputs = null;
		ClearingPaymentOutputs cpvOutputs = null;
		//byte compression = Byte.valueOf(null); // use the same compression for input & output

		// receive CPV input from client
		try {
			// read stream
			System.out.println(new Date(System.currentTimeMillis()) + ": receiving CPV inputs.");
			/*int size = this.dis.readInt();
			compression = this.dis.readByte();
			byte[] bytes = new byte[size];
			this.dis.readFully(bytes);

			// deserialize bytes to CPV input
			switch (compression) {
			case 1:
				// GZIP compression
				cpvInputs = (ClearingPaymentInputs) Serialization.toObjectFromGZIP(bytes);
				break;
			case 2:
				// Deflater compression
				cpvInputs = (ClearingPaymentInputs) Serialization.toObjectFromDeflater(bytes);
				break;
			default:
				// no compression
				cpvInputs = (ClearingPaymentInputs) Serialization.toObject(bytes);
				break;
			}
			*/
			cpvInputs = (ClearingPaymentInputs) Serialization.readObjectFromStream(dis);
		} catch (IOException e) {
			cpvInputs = null;
			e.printStackTrace();
			return;
		}

		// process CPV inputs
		System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs unmarshalled.");
		List<List<Float>> liabilitiesAmounts = cpvInputs.getLiabilitiesAmounts();
		List<List<Integer>> liabilitiesIndices = cpvInputs.getLiabilitiesIndices();
		List<Float> operatingCashFlow = cpvInputs.getOperatingCashFlow();
		List<Float> liquidAssets = cpvInputs.getLiquidAssets();
		int iteration = cpvInputs.getIteration();
		System.out.println(new Date(System.currentTimeMillis()) + ": CPV inputs split into separate lists.");

		// calculate CPV
		cpvOutputs = this.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow, liquidAssets, iteration);

		// write to output stream
		System.out.println(new Date(System.currentTimeMillis()) + ": compressing CPV outputs.");
		try {
			// serialize data to byte array
			/*byte[] bytes = null;
			switch (compression) {
			case 1:
				// GZIP compression
				bytes = Serialization.toBytesGZIP(cpvOutputs, Properties.SOCKET_BUFFER_BYTES);
				break;
			case 2:
				// Deflater compression
				bytes = Serialization.toBytesDeflater(cpvOutputs, Properties.SOCKET_BUFFER_BYTES);
				break;
			default:
				// no compression
				bytes = Serialization.toBytes(cpvOutputs, Properties.SOCKET_BUFFER_BYTES);
				break;
			}

			// write to stream
			System.out.println(new Date(System.currentTimeMillis()) + ": writing CPV outputs to stream.");
			dos.writeInt(bytes.length);
			dos.writeByte(compression);
			dos.write(bytes);
			dos.flush(); // don't know if I really need this, but it probably can't hurt
			*/
			Serialization.writeToDataStream(dos, cpvOutputs, Properties.SOCKET_BUFFER_BYTES, Properties.SOCKET_MSG_BYTES,
					CompressionType.GZIP);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// close resources
		try {
			System.out.println(new Date(System.currentTimeMillis()) + ": closing data input and output streams.");
			this.dis.close();
			this.dos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Calculates the Clearing Payments Vector for the given input.
	 * 
	 * @param liabilitiesAmounts
	 * @param liabilitiesIndices
	 * @param operatingCashFlow
	 * @param liquidAssets
	 * @param iteration
	 * @return
	 */
	public ClearingPaymentOutputs calculate(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, List<Float> liquidAssets,
			int iteration) {

		System.out.println(new Date(System.currentTimeMillis()) + ": CPV calculation invoked.");

		RunSimulation sim = new RunSimulation();
		ClearingPaymentOutputs cpvOutputs = sim.calculateClearingPaymentVector(liabilitiesAmounts, liabilitiesIndices,
				operatingCashFlow, liquidAssets, iteration);

		System.out.println(new Date(System.currentTimeMillis()) + ": CPV outputs calculated.");

		return cpvOutputs;
	}
}
