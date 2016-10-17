/***
 * JAVANAISE Implementation
 * JvnServerImpl class
 * Contact: 
 *
 * Authors: 
 */

package jvn;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class JvnCoordImpl extends UnicastRemoteObject implements JvnRemoteCoord {

	/**
	 * Singleton variable
	 */
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
	 * Lock for the coordinator synchronization
	 */
	private Lock l = new ReentrantLock();

	/**
	 * Default constructor
	 * 
	 * @throws JvnException
	 **/
	private JvnCoordImpl() throws Exception {
		super();
		
		lockMap = new ConcurrentHashMap<Integer, List<ServerState>>();
		
		// We get the serialization maps if they exist
		// cf try-with-resources
		try (
			final FileInputStream f1 = new FileInputStream("nameMap.ser");
			ObjectInputStream ois1 = new ObjectInputStream(f1);
			final FileInputStream f2 = new FileInputStream("objectMap.ser");
			ObjectInputStream ois2 = new ObjectInputStream(f2);
		) {
			// We get the maps
			nameMap = (ConcurrentHashMap<String, Integer>) ois1.readObject();
			objectMap = (ConcurrentHashMap<Integer, Serializable>) ois2.readObject();
			
			// Then foreach object we have we recreate the lockmap
			// And we set the idCount
			Integer max = new Integer(-1);
			for(Entry<Integer, Serializable> e : objectMap.entrySet()) {
				lockMap.put(e.getKey(), new ArrayList<ServerState>());
				
				if (e.getKey().compareTo(max) == 1) {
					max = e.getKey();
				}
			}
			idCount = max;
		} catch (IOException | ClassNotFoundException e) {
			nameMap = new ConcurrentHashMap<String, Integer>();
			objectMap = new ConcurrentHashMap<Integer, Serializable>();
		}
	}
	
	/**
	 * 
	 * @return the unique instance of the server
	 */
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
		
		if (nameMap.containsKey(jon)) {
			throw new JvnException("'" + jon + "' is already registered");
		} else {
			l.lock();
			try {
				// We add the association of the name and id
				nameMap.put(jon, jo.jvnGetObjectId());
				
				// We add the object
				objectMap.put(jo.jvnGetObjectId(), jo.jvnGetObjectState());
				
				// We add the server state
				ArrayList<ServerState> listServerState = new ArrayList<ServerState>();
				ServerState st = new ServerState(js, StateLock.W);
				listServerState.add(st);
				lockMap.put(jo.jvnGetObjectId(), listServerState);
				
				// We save the object mapping
				saveNameMap();
				saveObjectMap();
			} finally {
				l.unlock();
			}
			
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
	public Serializable jvnLockRead(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		List<ServerState> ls = lockMap.get(joi);
		Serializable r = objectMap.get(joi);
		
		l.lock();
		try {
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
				try {
					r = writer.getServer().jvnInvalidateWriterForReader(joi);
					objectMap.put(joi, r);
					writer.setState(StateLock.R);
					System.out.println("InvalidateWriterForReader on " + joi);
					
					// We save the object map
					saveObjectMap();
				} catch (RemoteException e) {
					// If we have no answer from the server we terminate it
					System.out.println("Server in state '" + writer.getState() + "' terminated");
					removeServer(writer.getServer());
				}
			}
			
			// We add the server in read mode
			if (self == null) {
				ServerState s = new ServerState(js, StateLock.R);
				ls.add(s);
			}
		} finally {
			l.unlock();
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
	public Serializable jvnLockWrite(int joi, JvnRemoteServer js)
			throws java.rmi.RemoteException, JvnException {
		List<ServerState> ls = lockMap.get(joi);
		
		System.out.println("Lock write on " + joi);
		
		Serializable r = objectMap.get(joi);
		
		System.out.println(ls.size());
		
		l.lock();
		try {
			// Check if there is no writers
			List<ServerState> lsCopy = new ArrayList<>(ls);
			for(ServerState st : lsCopy) {
				if (!js.equals(st.getServer())) {
					try {
						if (st.getState() == StateLock.W) {
							System.out.println("W " + st.getServer());
							r = st.getServer().jvnInvalidateWriter(joi);
							objectMap.put(joi, r);
							System.out.println("InvalidateWriter on " + joi);
							
							// We save the object map
							saveObjectMap();
						} else {
							System.out.println("R " + st.getServer());
							st.getServer().jvnInvalidateReader(joi);
							System.out.println("InvalidateReader on " + joi);
						}
					} catch (RemoteException e) {
						// If we have no answer from the server we terminate it
						System.out.println("Server in state '" + st.getState() + "' terminated");
						removeServer(st.getServer());
					}
				}
				
			}
			ls.clear();
			
			// We add the server in write mode
			ServerState s = new ServerState(js, StateLock.W);
			ls.add(s);
		} finally {
			l.unlock();
		}

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
	public void jvnTerminate(JvnRemoteServer js) throws java.rmi.RemoteException, JvnException {
		l.lock();
		try {
			removeServer(js);
		} finally{
			l.unlock();
		}
	}
	
	/**
	 * Remove a server from the server list
	 * 
	 * @param js the server to remove
	 */
	private void removeServer(JvnRemoteServer js) {
		// We look for each object if the server is using it
		for (Entry<Integer, List<ServerState>> e : lockMap.entrySet()) {
			List<ServerState> lst = e.getValue();
			
			// We also look in the list
			ServerState rem = null;
			for (ServerState s : lst) {

				// if we find it, we remove it
				if (js.equals(s.getServer())) {
					rem = s;
				}
			}
		
			if (rem != null)
				lst.remove(rem);
		}
	}
	
	/**
	 * Print all object id with their respective names
	 */
	public void printNames() {
		System.out.println("Name Map :");
		for(Entry<String, Integer> e : nameMap.entrySet()) {
			System.out.println(e.getKey() + "\t" + e.getValue());
		}
	}
	
	/**
	 * Print the states of all server connected
	 */
	public void printStates() {
		System.out.println("State Map :");
		for(Entry<Integer, List<ServerState>> e : lockMap.entrySet()) {
			System.out.println(e.getKey() + " :");
			for(ServerState s : e.getValue()) {
				System.out.println("\t" + s.getServer().toString() + "\t" + s.getState());
			}
		}
	}
	
	/**
	 * Save the name map
	 */
	private void saveNameMap() {
		try (
			final FileOutputStream file = new FileOutputStream("nameMap.ser");
			ObjectOutputStream oos = new ObjectOutputStream(file);
		) {
			oos.writeObject(this.nameMap);
			oos.flush();
		} catch (IOException e) {
			System.err.println("Unable to save name map");
			e.printStackTrace();
		}
	}
	
	/**
	 * Save the object map
	 */
	private void saveObjectMap() {
		try (
			final FileOutputStream file = new FileOutputStream("objectMap.ser");
			ObjectOutputStream oos = new ObjectOutputStream(file);
		) {
			oos.writeObject(this.objectMap);
			oos.flush();
		} catch (IOException e) {
			System.err.println("Unable to save object map");
			e.printStackTrace();
		}
	}
}
