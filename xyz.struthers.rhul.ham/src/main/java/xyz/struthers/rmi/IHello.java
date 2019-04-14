/**
 * 
 */
package xyz.struthers.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author acstr
 *
 */
public interface IHello extends Remote {
	String sayHello(String input) throws RemoteException;
}
