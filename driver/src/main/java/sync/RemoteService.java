package sync;

import java.rmi.Remote;
import java.rmi.RemoteException;

// Creating Remote interface for our application 
public interface RemoteService extends Remote {
	void printTestMsg(int insID) throws RemoteException;

	void execRequest(OpType ot) throws RemoteException;
}
