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
public class KryonetHelloServer {

	Server server;

	public KryonetHelloServer() {
		super();

		Permit.godMode();
		
		// create server
		server = new Server();

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
					c.sendTCP(response);
					System.out.println(response.getText());
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
