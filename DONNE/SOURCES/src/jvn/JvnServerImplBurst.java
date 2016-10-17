/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.io.Serializable;
import java.rmi.ConnectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

public class JvnServerImplBurst 	
              extends UnicastRemoteObject 
							implements JvnLocalServer, JvnRemoteServer{
	
  // A JVN server is managed as a singleton 
	private JvnRemoteCoord jr;
	
	private HashMap<Integer, JvnObject> objectMap;

  /**
  * Default constructor
  * @throws JvnException
  **/
	public JvnServerImplBurst() throws Exception {
		super();
		
		objectMap = new HashMap<>();
	
		connectToCoord();
	}
	
	private void connectToCoord() {
		try{
			Registry registry = LocateRegistry.getRegistry();
			jr = (JvnRemoteCoord) registry.lookup("coord");
		} catch (RemoteException | NotBoundException e){
			System.err.println("Error on client : " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	* The JVN service is not used anymore
	* @throws JvnException
	**/
	public void jvnTerminate()
	throws jvn.JvnException {
    	try {
			jr.jvnTerminate(this);
		} catch (RemoteException e) {
			System.err.println("Problem with termination : " + e.getMessage());
			e.printStackTrace();
		} 
	} 
	
	/**
	* creation of a JVN object
	* @param o : the JVN object state
	* @throws JvnException
	**/
	public  JvnObject jvnCreateObject(Serializable o)
	throws jvn.JvnException { 
		try{
			int id = jr.jvnGetObjectId();
			System.out.println(id);
			JvnObjectImpl jo = new JvnObjectImpl(id, o, StateLock.W);
			objectMap.put(id, jo);
			
			return jo;
		} catch (ConnectException e) {
			// We try to reconnect
			connectToCoord();
		} catch(RemoteException e){
			System.err.println("Problem with object creation : "+e.getMessage());
			e.printStackTrace();
		}
		return null; 
	}
	
	/**
	*  Associate a symbolic name with a JVN object
	* @param jon : the JVN object name
	* @param jo : the JVN object 
	* @throws JvnException
	**/
	public  void jvnRegisterObject(String jon, JvnObject jo)
	throws jvn.JvnException {
		try{
			jr.jvnRegisterObject(jon, jo, this);
		} catch (ConnectException e) {
			// We try to reconnect
			connectToCoord();
		} catch(RemoteException e){
			System.err.println("Problem with object registering : "+e.getMessage());
			e.printStackTrace();
		}
	}
	
	/**
	* Provide the reference of a JVN object beeing given its symbolic name
	* @param jon : the JVN object name
	* @return the JVN object 
	* @throws JvnException
	**/
	public  JvnObject jvnLookupObject(String jon)
	throws jvn.JvnException {
		try{
			JvnObject jo = jr.jvnLookupObject(jon, this);
			
			if (jo != null) {
				objectMap.put(jo.jvnGetObjectId(), jo);
			}	
			
			return jo;
		} catch (ConnectException e) {
			// We try to reconnect
			connectToCoord();
		} catch(RemoteException e){
			System.err.println("Problem with object lookup : "+e.getMessage());
			e.printStackTrace();
		}
		return null;
	}	
	
	/**
	* Get a Read lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockRead(int joi)
	 throws JvnException {
		try {
			return jr.jvnLockRead(joi, this);
		} catch (ConnectException e) {
			// We try to reconnect
			connectToCoord();
		} catch (RemoteException e) {
			System.err.println("Problem with lock read : " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}	
	/**
	* Get a Write lock on a JVN object 
	* @param joi : the JVN object identification
	* @return the current JVN object state
	* @throws  JvnException
	**/
   public Serializable jvnLockWrite(int joi)
	 throws JvnException {
	   try {
			return jr.jvnLockWrite(joi, this);
		} catch (ConnectException e) {
			// We try to reconnect
			connectToCoord();
		} catch (RemoteException e) {
			System.err.println("Problem with lock write : " + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}	

	
  /**
	* Invalidate the Read lock of the JVN object identified by id 
	* called by the JvnCoord
	* @param joi : the JVN object id
	* @return void
	* @throws java.rmi.RemoteException,JvnException
	**/
  public void jvnInvalidateReader(int joi)
	throws java.rmi.RemoteException,jvn.JvnException {
		JvnObject jo = objectMap.get(joi);
		
		jo.jvnInvalidateReader();
	};
	    
	/**
	* Invalidate the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
  public Serializable jvnInvalidateWriter(int joi)
	throws java.rmi.RemoteException,jvn.JvnException { 
	  	JvnObject jo = objectMap.get(joi);
	  	
	  	return jo.jvnInvalidateWriter();
	};
	
	/**
	* Reduce the Write lock of the JVN object identified by id 
	* @param joi : the JVN object id
	* @return the current JVN object state
	* @throws java.rmi.RemoteException,JvnException
	**/
   public Serializable jvnInvalidateWriterForReader(int joi)
	 throws java.rmi.RemoteException,jvn.JvnException { 
	   	JvnObject jo = objectMap.get(joi);
	   	
	   	return jo.jvnInvalidateWriterForReader();
	 };

}

 
