/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.nqzero.permit.Permit;

import xyz.struthers.rhul.ham.config.Properties;
import xyz.struthers.rhul.ham.process.ClearingPaymentInputs;
import xyz.struthers.rhul.ham.process.ClearingPaymentOutputs;

/**
 * @author acstr
 *
 */
public class KryonetHelloServer {

	Server server;

	public KryonetHelloServer() {
		super();
		Permit.godMode();
		
		// create server
		server = new Server(Properties.NETWORK_BUFFER_BYTES, Properties.NETWORK_BUFFER_BYTES);

		// For consistency, the classes to be sent over the network are
		// registered by the same method for both the client and server.
		KryonetHelloNetwork.register(server);

		server.addListener(new Listener() {
			public void connected(Connection connection) {
				System.out.println("server connected");
			}

			public void received(Connection c, Object object) {
				if (object instanceof KryonetHelloRequest) {
					KryonetHelloRequest request = (KryonetHelloRequest) object;
					System.out.println("Kryonet client request: " + request.getText());

					KryonetHelloResponse response = new KryonetHelloResponse(request.getText());
					response.setText(request.getText() + ", Kryonet world!");
					server.sendToAllTCP(response);
					//c.sendTCP(response);
					System.out.println(response.getText());
				} else if (object instanceof ClearingPaymentInputs) {
					System.out.println("CPV inputs received at " + new Date(System.currentTimeMillis()) + ".");
					@SuppressWarnings("unused")
					ClearingPaymentInputs cpvInputs = (ClearingPaymentInputs) object;
					System.out.println("CPV inputs unmarshalled at " + new Date(System.currentTimeMillis()) + ".");

					ClearingPaymentOutputs cpvOutputs = new ClearingPaymentOutputs();
					final int numAgents = 12500000;
					List<Float> equityOfNode = new ArrayList<Float>(numAgents);
					List<Integer> defaultOrderOfNode = new ArrayList<Integer>(numAgents);
					for (int i=0; i<numAgents; i++) {
						equityOfNode.add(0f);
						defaultOrderOfNode.add(i);
					}
					int iteration = 0;
					cpvOutputs.setEquityOfNode(equityOfNode);
					cpvOutputs.setDefaultOrderOfNode(defaultOrderOfNode);
					cpvOutputs.setIteration(iteration);
					
					System.out.println(new Date(System.currentTimeMillis())
							+ ": CPV outputs calculated and being returned to Kryonet client.");
					server.sendToAllTCP(cpvOutputs);
					//c.sendTCP(response);
				}
			}
			
			public void disconnected(Connection c) {
				System.out.println("server disconnected");
			}
		});

		try {
			server.bind(KryonetHelloNetwork.NETWORK_PORT_TCP, KryonetHelloNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.start();
	}

	public static void main(String[] args) {
		new KryonetHelloServer();
	}

}
