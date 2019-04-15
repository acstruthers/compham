package xyz.struthers.kryonet;

import java.io.IOException;
import java.rmi.RemoteException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;

public class KryonetRmiServer {

	Server server;

	public KryonetRmiServer() {
		server = new Server();

		// Register the classes that will be sent over the network.
		KryonetRmiNetwork.register(server);

		try {
			server.bind(KryonetRmiNetwork.NETWORK_PORT_TCP);
		} catch (IOException e) {
			e.printStackTrace();
		}
		server.start();
	}

	class KryonetRmiHello extends Connection implements KryonetRmiHelloInterface {

		public KryonetRmiHello() {
			new ObjectSpace(this).register(KryonetRmiNetwork.HELLO, this);
		}

		@Override
		public String sayHello(String input) throws RemoteException {
			return input + ", World!";
		}
	}

	public static void main(String[] args) {
		new KryonetRmiServer();
	}
}
