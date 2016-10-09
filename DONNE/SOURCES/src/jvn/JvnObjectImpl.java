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
	
	public JvnObjectImpl(int id, Serializable objectData, StateLock state) {
		this(id, objectData);
		this.state = state;
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
			default:
				throw new JvnException("Read lock already taken");
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
			case R:
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				Serializable r = js.jvnLockWrite(id);
				if (r != null) {
					objectData = (Serializable)r;
				}
				state = StateLock.W;
				break;
			default:
				throw new JvnException("Write lock already taken");
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
			case RC:
				throw new JvnException("Read lock already unlocked");
			case WC:
				throw new JvnException("Write lock already unlocked");
			case NL:
				throw new JvnException("No lock taken");
		}
		System.out.println("Wait ended");
		notify();
	}

	public int jvnGetObjectId() throws JvnException {		
		return id;
	}

	public Serializable jvnGetObjectState() throws JvnException {	
		return objectData;
	}

	public synchronized void jvnInvalidateReader() throws JvnException {
		switch(state){
			case R:
			case RWC:
				try {
					System.out.println("Wait for invalidateReader");
					wait();
					state = StateLock.NL;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case RC:
				state = StateLock.NL;
				break;
			default:
				throw new JvnException("InvalidateReader when no readLock");
		}
	}

	public synchronized Serializable jvnInvalidateWriter() throws JvnException {
		switch(state){
			case W:
			case RWC:
				try {
					System.out.println("Wait for invalidateWriter");
					wait();
					state = StateLock.NL;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case WC:
				state = StateLock.NL;
				break;
			default:
				throw new JvnException("InvalidateWriter when no writeLock");
		}
		return jvnGetObjectState();
	}

	public synchronized Serializable jvnInvalidateWriterForReader() throws JvnException {		
		switch(state) {
			case W:
				try {
					System.out.println("Wait for invalidateWriterForReader : W");
					wait();
					state = StateLock.RC;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case RWC:
				try {
					System.out.println("Wait for invalidateWriterForReader : RWC");
					wait();
					state = StateLock.R;
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				break;
			case WC:		
				state = StateLock.RC;
				break;
			default:
				throw new JvnException("InvalidateWriterForReader called when no write lock");
		}
		
		return jvnGetObjectState();
	}
}
