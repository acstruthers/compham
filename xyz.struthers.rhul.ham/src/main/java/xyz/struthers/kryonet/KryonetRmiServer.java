package xyz.struthers.kryonet;

import java.io.IOException;
import java.rmi.RemoteException;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.kryonet.rmi.ObjectSpace;
import com.nqzero.permit.Permit;

public class KryonetRmiServer {

	Server server;
	KryonetRmiHello hello;

	public KryonetRmiServer() {
		Permit.godMode();
		
		server = new Server() {
			protected Connection newConnection() {
				Connection conn = new KryonetRmiHello();
				hello = (KryonetRmiHello) conn;
				return conn;
			}
		};
		
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
