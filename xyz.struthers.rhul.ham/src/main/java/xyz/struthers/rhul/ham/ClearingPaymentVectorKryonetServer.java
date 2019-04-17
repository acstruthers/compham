/**
 * 
 */
package xyz.struthers.rhul.ham;

import java.io.IOException;
import java.util.Date;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.nqzero.permit.Permit;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author Adam Struthers
 * @since 16-Apr-2019
 */
public class ClearingPaymentVectorKryonetServer {

	Server server;

	/**
	 * Calculates the Clearing Payment Vector when invoked, and returns the results
	 * to the client.
	 */
	public ClearingPaymentVectorKryonetServer() {
		super();
		Permit.godMode(); // allows reflection in Java 11

		// create server
		server = new Server(Properties.NETWORK_BUFFER_BYTES, Properties.NETWORK_BUFFER_BYTES);

		// For consistency, the classes to be sent over the network are
		// registered by the same method for both the client and server.
		KryonetNetwork.register(server);

		server.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("Kryonet CPV server connected.");
			}

			public void received(Connection conn, Object object) {
				if (object instanceof ClearingPaymentInputs) {
					ClearingPaymentInputs cpvInputs = (ClearingPaymentInputs) object;
					System.out.println("CPV inputs received at " + new Date(System.currentTimeMillis()) + ".");
					System.out.println("Calculating Clearing Payment Vector now.");

					RunSimulation sim = new RunSimulation();
					ClearingPaymentOutputs cpvOutputs = sim.calculateClearingPaymentVector(
							cpvInputs.getLiabilitiesAmounts(), cpvInputs.getLiabilitiesIndices(),
							cpvInputs.getOperatingCashFlow(), cpvInputs.getLiquidAssets(), cpvInputs.getIteration());

					System.out.println(new Date(System.currentTimeMillis())
							+ ": CPV outputs calculated and being returned to Kryonet client.");

					server.sendToAllTCP(cpvOutputs);
					// conn.sendTCP(cpvOutputs);
					System.out.println("CPV outputs sent at " + new Date(System.currentTimeMillis()) + ".");
				}
			}

			public void disconnected(Connection c) {
				System.out.println("Kryonet CPV server disconnected.");
			}
		});

		try {
			server.bind(KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.start();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ClearingPaymentVectorKryonetServer();
	}

}
