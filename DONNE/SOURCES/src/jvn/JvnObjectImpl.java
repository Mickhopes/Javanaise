package jvn;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{
	
	private Serializable objectData;
	private int id;
	private StateLock state;
	
	public JvnObjectImpl(int id, Serializable objectData){
		this.id = id;
		this.objectData = objectData;
		this.state = StateLock.NL;
	}
	
	public StateLock getState() {
		return state;
	}
	
	public void setState(StateLock s) {
		this.state = s;
	}
	
	public synchronized void jvnLockRead() throws JvnException {
		switch(state) {
			case RC:
				state = StateLock.R;
				break;
			case WC:
				state = StateLock.RWC;
				break;
			case NL:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				objectData = (Serializable)js.jvnLockRead(id);
				state = StateLock.R;
				break;
		}
	}

	public synchronized void jvnLockWrite() throws JvnException {
		switch(state) {
			case WC:
			case RWC:
				state = StateLock.W;
				break;
			case NL:
			case RC:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				Serializable r = js.jvnLockWrite(id);
				if (r != null) {
					objectData = (Serializable)r;
				}
				state = StateLock.W;
				break;
		}
	}

	public synchronized void jvnUnLock() throws JvnException {
		switch(state) {
			case W:
			case RWC:
				state = StateLock.WC;
				break;
			case R:
				state = StateLock.RC;
				break;
		}
		System.out.println("Fin d'attente");
		notify();
	}

	public int jvnGetObjectId() throws JvnException {		
		return id;
	}

	public Serializable jvnGetObjectState() throws JvnException {	
		return objectData;
	}

	public synchronized void jvnInvalidateReader() throws JvnException {
		if (state == StateLock.R || state == StateLock.RWC) {
			try {
				System.out.println("Attente invalidateReader");
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state = StateLock.NL;
	}

	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		if (state == StateLock.W || state == StateLock.RWC) {
			try {
				System.out.println("Attente invalidateWriter");
				wait();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state = StateLock.NL;
		return jvnGetObjectState();
	}

	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {		
		switch(state) {
			case W:
				try {
					System.out.println("Attente invalidateWriterForReader");
					wait();
					state = StateLock.NL;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			case WC:
			case RWC:
				state = StateLock.RC;
				break;
		}
		
		return jvnGetObjectState();
	}
}
