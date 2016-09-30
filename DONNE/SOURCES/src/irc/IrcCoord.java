package irc;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import jvn.JvnCoordImpl;

public class IrcCoord {

	public static void main(String[] args) {
		try {
			// We bind the server in the rmi registry
			JvnCoordImpl jc = JvnCoordImpl.jvnGetServer();
			
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("coord", jc);
		} catch (RemoteException e) {
			System.err.println("Unable to bind coordinator: " + e.getMessage());
			e.printStackTrace();
		}
	}

}
