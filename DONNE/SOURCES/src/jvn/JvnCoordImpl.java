/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.io.Serializable;

public class JvnCoordImpl 	
              extends UnicastRemoteObject 
							implements JvnRemoteCoord{
	
	/**
	 * Static variable to identify all the jvnObject
	 * We consider in our case that we won't reach max int
	 */
	private static int idCount = 0;
	
	/**
	 * HashMap for the names of the JvnObjects
	 */
	private HashMap<String, Integer> nameMap;
	
	/**
	 * HashMap for the identifiers and objects
	 */
	private HashMap<Integer, JvnObject> objectMap;
	
	/**
	 * HashMap for the list of the server and their state
	 */
	private HashMap<Integer, List<ServerState>> lockMap;

  /**
  * Default constructor
  * @throws JvnException
  **/
	private JvnCoordImpl() throws Exception {
		nameMap = new HashMap<String, Integer>();
		objectMap = new HashMap<Integer, JvnObject>();
		lockMap = new HashMap<Integer, List<ServerState>>();
		
		try {
			// We bind the server in the rmi registry
			Registry registry = LocateRegistry.getRegistry();
			registry.rebind("coord", this);
		} catch(RemoteException e) {
			System.err.println("Unable to bind coordinator: " + e.getMessage());
			e.printStackTrace();
		}
	}

  /**
  *  Allocate a NEW JVN object id (usually allocated to a 
  *  newly created JVN object)
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized int jvnGetObjectId()
  throws java.rmi.RemoteException,jvn.JvnException {
    return idCount++;
  }
  
  /**
  * Associate a symbolic name with a JVN object
  * @param jon : the JVN object name
  * @param jo  : the JVN object 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
	// We add the association of the name and id
    nameMap.put(jon, jo.jvnGetObjectId());
    
    // Then we add the object
    objectMap.put(jo.jvnGetObjectId(), jo);
    
    // Finally we add the server to the list
    ArrayList<ServerState> listServerState = new ArrayList<ServerState>();
    ServerState st = new ServerState(js, StateLock.NL);
    listServerState.add(st);
    lockMap.put(jo.jvnGetObjectId(), listServerState);
  }
  
  /**
  * Get the reference of a JVN object managed by a given JVN server 
  * @param jon : the JVN object name
  * @param js : the remote reference of the JVNServer
  * @throws java.rmi.RemoteException,JvnException
  **/
  public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
  throws java.rmi.RemoteException,jvn.JvnException{
	int idObject = nameMap.get(jon);
    JvnObject jo = objectMap.get(idObject);
    
    // If jo is null then we return null
    if (jo == null) {
    	return null;
    }
    
    // Otherwise we register the server and set its state to NL
    List<ServerState> lst = lockMap.get(jo.jvnGetObjectId());
    ServerState st = new ServerState(js, StateLock.NL);
    lst.add(st);
    
    return jo;
  }
  
  /**
  * Get a Read lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
	// to be completed
    return objectMap.get(joi).jvnGetObjectState();
   }

  /**
  * Get a Write lock on a JVN object managed by a given JVN server 
  * @param joi : the JVN object identification
  * @param js  : the remote reference of the server
  * @return the current JVN object state
  * @throws java.rmi.RemoteException, JvnException
  **/
   public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
   throws java.rmi.RemoteException, JvnException{
    // to be completed
    return objectMap.get(joi).jvnGetObjectState();
   }

	/**
	* A JVN server terminates
	* @param js  : the remote reference of the server
	* @throws java.rmi.RemoteException, JvnException
	**/
    public synchronized void jvnTerminate(JvnRemoteServer js)
	 throws java.rmi.RemoteException, JvnException {
    	// We look for each object if the server is using it
    	for(Entry<Integer, List<ServerState>> e : lockMap.entrySet()) {
    		List<ServerState> lst = e.getValue();
    		
    		// We also look in the list
    		for(ServerState s : lst) {
    			
    			// if we find it, we remove it
    			if (js == s.getServer()) {
    				lst.remove(s);
    			}
    		}
    	}
    }
}

 
