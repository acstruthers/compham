/**
 * 
 */
package xyz.struthers.kryonet;

import java.io.IOException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.nqzero.permit.Permit;

/**
 * @author acstr
 *
 */
public class KryonetServer {

	Server server;

	public KryonetServer() {
		super();

		Permit.godMode();
		
		// create server
		server = new Server();

		// For consistency, the classes to be sent over the network are
		// registered by the same method for both the client and server.
		KryonetNetwork.register(server);

		server.addListener(new Listener() {
			public void connected(Connection connection) {
			}

			public void received(Connection c, Object object) {
				if (object instanceof KryonetRequest) {
					KryonetRequest request = (KryonetRequest) object;
					System.out.println("Kryonet server request: " + request.getText());

					KryonetResponse response = new KryonetResponse(request.getText());
					response.setText(request.getText() + ", Kryonet world!");
					System.out.println(response.getText());
					server.sendToAllTCP(response);
				}
			}

			public void disconnected(Connection c) {
			}
		});

		try {
			server.bind(KryonetNetwork.NETWORK_PORT_TCP, KryonetNetwork.NETWORK_PORT_UDP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.start();
	}

	public static void main(String[] args) {
		new KryonetServer();
	}

}
