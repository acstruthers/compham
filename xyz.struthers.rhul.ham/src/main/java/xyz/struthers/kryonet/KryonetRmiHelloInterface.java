package xyz.struthers.kryonet;

import java.rmi.RemoteException;

public interface KryonetRmiHelloInterface {
	String sayHello(String input) throws RemoteException;
}
