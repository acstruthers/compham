package xyz.struthers.rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GzipCpvInterface extends Remote {
	/**
	 * 
	 * @param cpvInputBytes - cpvInputs zipped using GZip
	 * @return covOutputs zipped using GZip
	 * @throws RemoteException
	 */
	public byte[] calculate(byte[] cpvInputBytes) throws RemoteException;
}
