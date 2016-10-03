/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.io.Serializable;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import irc.Sentence;

public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord {
	
	private static JvnCoordImpl jc;

	/**
	 * Static variable to identify all the jvnObject We consider in our case
	 * that we won't reach max int
	 */
	private static int idCount = 0;

	/**
	 * HashMap for the names of the JvnObjects
	 */
	private ConcurrentHashMap<String, Integer> nameMap;

	/**
	 * HashMap for the identifiers and objects
	 */
	private ConcurrentHashMap<Integer, Serializable> objectMap;

	/**
	 * HashMap for the list of the server and their state
	 */
	private ConcurrentHashMap<Integer, List<ServerState>> lockMap;

	/**
	 * Default constructor
	 * 
	 * @throws JvnException
	 **/
	private JvnCoordImpl() throws Exception {
		super();
		
		nameMap = new ConcurrentHashMap<String, Integer>();
		objectMap = new ConcurrentHashMap<Integer, Serializable>();
		lockMap = new ConcurrentHashMap<Integer, List<ServerState>>();
	}
	
	public static JvnCoordImpl jvnGetServer() {
		if (jc == null) {
			try {
				jc = new JvnCoordImpl();
			} catch (Exception e) {
				return null;
			}
		}
		return jc;
	}

	/**
	 * Allocate a NEW JVN object id (usually allocated to a newly created JVN
	 * object)
	 * 
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public synchronized int jvnGetObjectId() throws java.rmi.RemoteException, jvn.JvnException {
		return idCount++;
	}

	/**
	 * Associate a symbolic name with a JVN object
	 * 
	 * @param jon
	 *            : the JVN object name
	 * @param jo
	 *            : the JVN object
	 * @param joi
	 *            : the JVN object identification
	 * @param js
	 *            : the remote reference of the JVNServer
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
			throws java.rmi.RemoteException, jvn.JvnException {
		
		if (nameMap.contains(jon)) {
			throw new JvnException();
		} else {
			// We add the association of the name and id
			nameMap.put(jon, jo.jvnGetObjectId());

			// Then we add the object
			objectMap.put(jo.jvnGetObjectId(), jo.jvnGetObjectState());
			
			System.out.println("Object " + jon + "registered");
			printNames();
		}
	}

	/**
	 * Get the reference of a JVN object managed by a given JVN server
	 * 
	 * @param jon
	 *            : the JVN object name
	 * @param js
	 *            : the remote reference of the JVNServer
	 * @throws java.rmi.RemoteException,JvnException
	 **/
	public JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
			throws java.rmi.RemoteException, jvn.JvnException {
		try {
			Integer idObject = nameMap.get(jon);
			Serializable jo = objectMap.get(idObject);
			return new JvnObjectImpl(idObject, jo);
		} catch (NullPointerException e) {
			return null;
		}
	}

	/**
	 * Get a Read lock on a JVN object managed by a given JVN server
	 * 
	 * @param joi
	 *            : the JVN object identification
	 * @param js
	 *            : the remote reference of the server
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,
	 *             JvnException
	 **/
	public synchronized Serializable jvnLockRead(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		List<ServerState> ls = lockMap.get(joi);
		Serializable r = objectMap.get(joi);
		
		// Check if there is no writers
		ServerState writer = null;
		ServerState self = null;
		for(ServerState st : ls) {
			if (!js.equals(st.getServer())) {
				if (st.getState() == StateLock.W) {
					writer = st;
					break;
				}
			} else {
				self = st;
			}
		}
		
		System.out.println("Lock read sur " + joi);
	
		// If there is a writer, we invalidate it
		if (writer != null) {
			r = writer.getServer().jvnInvalidateWriterForReader(joi);
			objectMap.put(joi, r);
			writer.setState(StateLock.R);
			
			System.out.println("Invalidation d'un writer sur " + joi);
		}
		
		// We add the server in read mode
		if (self == null) {
			ServerState s = new ServerState(js, StateLock.R);
			ls.add(s);
		}
		
		printStates();
		
		return r;
	}

	/**
	 * Get a Write lock on a JVN object managed by a given JVN server
	 * 
	 * @param joi
	 *            : the JVN object identification
	 * @param js
	 *            : the remote reference of the server
	 * @return the current JVN object state
	 * @throws java.rmi.RemoteException,
	 *             JvnException
	 **/
	public synchronized Serializable jvnLockWrite(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		List<ServerState> ls = lockMap.get(joi);
		
		// If joi entry doesn't exists then the object have been created
		if (ls == null) {
			ArrayList<ServerState> listServerState = new ArrayList<ServerState>();
			ServerState st = new ServerState(js, StateLock.W);
			listServerState.add(st);
			lockMap.put(joi, listServerState);
			
			System.out.println("Object cr�� " + joi);
			
			return null;
		}
		
		System.out.println("Lock write sur " + joi);
		
		Serializable r = objectMap.get(joi);
		
		System.out.println(ls.size());
		// Check if there is no writers
		for(ServerState st : ls) {
			if (!js.equals(st.getServer())) {
				if (st.getState() == StateLock.W) {
					System.out.println("W " + st.getServer());
					r = st.getServer().jvnInvalidateWriter(joi);
					objectMap.put(joi, r);
					System.out.println("Invalidation d'un writer sur " + joi);
				} else {
					System.out.println("R " + st.getServer());
					st.getServer().jvnInvalidateReader(joi);
					System.out.println("Invalidation d'un reader sur " + joi);
				}
			}
			
		}
		ls.clear();
		
		// We add the server in write mode
		ServerState s = new ServerState(js, StateLock.W);
		ls.add(s);
		printStates();
		
		return r;
	}

	/**
	 * A JVN server terminates
	 * 
	 * @param js
	 *            : the remote reference of the server
	 * @throws java.rmi.RemoteException,
	 *             JvnException
	 **/
	public synchronized void jvnTerminate(JvnRemoteServer js) throws java.rmi.RemoteException, JvnException {
		// We look for each object if the server is using it
		for (Entry<Integer, List<ServerState>> e : lockMap.entrySet()) {
			List<ServerState> lst = e.getValue();

			// We also look in the list
			for (ServerState s : lst) {

				// if we find it, we remove it
				if (js == s.getServer()) {
					lst.remove(s);
				}
			}
		}
	}
	
	public void printNames() {
		System.out.println("Name Map :");
		for(Entry<String, Integer> e : nameMap.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue());
		}
	}
	
	public void printStates() {
		System.out.println("State Map :");
		for(Entry<Integer, List<ServerState>> e : lockMap.entrySet()) {
			System.out.println(e.getKey() + " :");
			for(ServerState s : e.getValue()) {
				System.out.println("\t" + s.getServer().toString() + "\t" + s.getState());
			}
		}
	}
}
