/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;

import xyz.struthers.rhul.ham.config.Properties;

/**
 * @author Adam Struthers
 * @since 2019-05-16
 */
public class CpvSocketServer {

	/**
	 * 
	 */
	public CpvSocketServer() {
		super();
	}
/*
	public void run() {
		// create network connection and start listening
		ServerSocket serverSocket = null;
		try {
			serverSocket = new ServerSocket(Properties.CPV_SERVER_PORT);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		Socket socket = null;
		try {
			System.out.println("Server listens on port " + Properties.CPV_SERVER_PORT + ".");
			socket = serverSocket.accept();
			System.out.println("Connection to client: " + socket.getInetAddress() + " established.");
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// unmarshall network input
		ClearingPaymentInputs cpvInputs = null;
		DataInputStream din;
		try {
			din = new DataInputStream(socket.getInputStream());
			int size = din.readInt();
			byte compression = din.readByte();
			byte[] bytes = new byte[size];
			din.readFully(bytes);

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

			// disconnect from network
			din.close();
			socket.close();
		} catch (IOException ioe) {
			cpvInputs = null;
			ioe.printStackTrace();
			return;
		}
		try {
			serverSocket.close();
		} catch (IOException sc) {
			sc.printStackTrace();
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
		ClearingPaymentOutputs cpvOutputs = this.calculate(liabilitiesAmounts, liabilitiesIndices, operatingCashFlow,
				liquidAssets, iteration);

		System.out.println(new Date(System.currentTimeMillis()) + ": compressing CPV outputs.");

		// send results back to client
		this.sendBackToClient(cpvOutputs);
	}

	public ClearingPaymentOutputs calculate(List<List<Float>> liabilitiesAmounts,
			List<List<Integer>> liabilitiesIndices, List<Float> operatingCashFlow, List<Float> liquidAssets,
			int iteration) {

		System.out.println(new Date(System.currentTimeMillis()) + ": CPV invoked.");

		RunSimulation sim = new RunSimulation();
		ClearingPaymentOutputs cpvOutputs = sim.calculateClearingPaymentVector(liabilitiesAmounts, liabilitiesIndices,
				operatingCashFlow, liquidAssets, iteration);

		System.out.println(
				new Date(System.currentTimeMillis()) + ": CPV outputs calculated and being returned to client.");

		return cpvOutputs;
	}

	private void sendBackToClient(ClearingPaymentOutputs cpvOutputs) {
		Socket clientSocket = null;
		ObjectOutputStream out = null;

		try {
			clientSocket = new Socket(Properties.ECONOMY_CLIENT_HOST, Properties.ECONOMY_CLIENT_PORT);
			out = new ObjectOutputStream(clientSocket.getOutputStream());
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		try {
			DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

			byte[] bytes = null;
			byte compression = 1;
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

			dos.writeInt(bytes.length);
			dos.writeByte(compression);
			dos.write(bytes);
			dos.flush(); // don't know if I really need this, but it probably can't hurt
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			out.close();
			clientSocket.close();
			System.out.println("Connection to client: " + clientSocket.getInetAddress() + " closed.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
*/
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws IOException {
		// new CpvSocketServer().run();

		// server is listening on specified port
		@SuppressWarnings("resource")
		ServerSocket ss = new ServerSocket(Properties.CPV_SERVER_PORT);

		// running infinite loop for getting client request
		while (true) {
			Socket s = null;

			try {
				// socket object to receive incoming client requests
				s = ss.accept();
				System.out.println(new Date(System.currentTimeMillis()) + ": a new CPV client is connected : " + s);

				// obtaining input and out streams
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				// create a new thread and start the CPV client handler
				System.out.println("Assigning new thread for this client");
				Thread t = new CpvSocketClientHandler(s, dis, dos);
				t.start();
			} catch (Exception e) {
				s.close();
				e.printStackTrace();
			}
		}
	}

}
