package jvn;

public class ServerState {
	/**
	 * Reference of the application server
	 */
	private JvnRemoteServer server;
	
	/**
	 * State of the lock for the server
	 */
	private StateLock state;
	
	/**
	 * Constructor
	 * @param server Reference of the server
	 * @param state State of the lock
	 */
	public ServerState(JvnRemoteServer server, StateLock state) {
		this.server = server;
		this.state = state;
	}

	public JvnRemoteServer getServer() {
		return server;
	}

	public void setServer(JvnRemoteServer server) {
		this.server = server;
	}

	public StateLock getState() {
		return state;
	}

	public void setState(StateLock state) {
		this.state = state;
	}
}
