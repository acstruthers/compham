/**
 * 
 */
package xyz.struthers.rmi;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/**
 * @author acstr
 *
 */
public class HelloClient {

	public static final String HOST = "Adam-E590";
	public static final int PORT = 1099;
	
	public HelloClient() {
		super();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//String host = (args.length < 1) ? null : args[0];
		try {
			Registry registry = LocateRegistry.getRegistry(HOST, PORT);
			IHello stub = (IHello) registry.lookup("Hello");
			String response = stub.sayHello("Hello");
			System.out.println("response: " + response);
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}
	}

}
