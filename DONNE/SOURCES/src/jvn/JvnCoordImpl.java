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
	public synchronized void jvnRegisterObject(String jon, JvnObject jo, JvnRemoteServer js)
			throws java.rmi.RemoteException, jvn.JvnException {
		// We add the association of the name and id
		nameMap.put(jon, jo.jvnGetObjectId());

		// Then we add the object
		jo.setState(StateLock.NL);
		objectMap.put(jo.jvnGetObjectId(), jo.jvnGetObjectState());
		
		System.out.println("Object " + jon + "registered");
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
	public synchronized JvnObject jvnLookupObject(String jon, JvnRemoteServer js)
			throws java.rmi.RemoteException, jvn.JvnException {
		Integer idObject = nameMap.get(jon);
		
		if (idObject == null) {
			return null;
		}
		
		Serializable jo = objectMap.get(idObject);

		return new JvnObjectImpl(idObject, (Sentence)jo);
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
		for(ServerState st : ls) {
			if (st.getState() == StateLock.W) {
				writer = st;
				break;
			}
		}
		
		System.out.println("Lock read sur " + joi);
	
		// If there is a writer, we invalidate it
		if (writer != null) {
			r = writer.getServer().jvnInvalidateWriterForReader(joi);
			writer.setState(StateLock.R);
			
			System.out.println("Invalidation d'un writer sur " + joi);
		}
		
		// We add the server in read mode
		ServerState s = new ServerState(js, StateLock.R);
		ls.add(s);
		
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
			
			System.out.println("Object créé " + joi);
			
			return null;
		}
		
		System.out.println("Lock write sur " + joi);
		
		Serializable r = objectMap.get(joi);
		
		// Check if there is no writers
		ServerState s = null;
		for(ServerState st : ls) {
			if (js != st.getServer()) {
				if (st.getState() == StateLock.W) {
					r = st.getServer().jvnInvalidateWriter(joi);
					System.out.println("Invalidation d'un writer sur " + joi);
				} else {
					st.getServer().jvnInvalidateReader(joi);
					System.out.println("Invalidation d'un reader sur " + joi);
				}
				
				ls.remove(st);
			} else {
				s = st;
			}
		}
		
		// We add the server in read mode
		if (s == null) {
			s = new ServerState(js, StateLock.W);
			ls.add(s);
		} else {
			s.setState(StateLock.W);
		}
		
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
}
