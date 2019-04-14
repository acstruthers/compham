/**
 * 
 */
package xyz.struthers.rmi;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 * @author acstr
 *
 */
public class HelloServer implements IHello {

	public static final String HOST = "Adam-E590";
	public static final int PORT = 1099;

	public HelloServer() {
		super();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see xyz.struthers.rmi.IHello#sayHello(java.lang.String)
	 */
	@Override
	public String sayHello(String input) throws RemoteException {
		return input + ", world!";
	}

	public static void main(String args[]) {
		try {
			// Create RMI Registry
			System.setProperty("java.rmi.server.hostname", HOST);
			LocateRegistry.createRegistry(PORT);

			// register object
			HelloServer obj = new HelloServer();
			IHello stub = (IHello) UnicastRemoteObject.exportObject(obj, 0);

			// Bind the remote object's stub in the registry
			Registry registry = LocateRegistry.getRegistry(HOST, PORT);
			registry.bind("Hello", stub);

			System.err.println("Server ready");
		} catch (Exception e) {
			System.err.println("HelloServer exception: " + e.toString());
			e.printStackTrace();
		}
	}
}
