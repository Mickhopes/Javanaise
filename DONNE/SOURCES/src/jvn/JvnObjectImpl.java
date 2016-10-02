package jvn;

import irc.Sentence;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{
	
	private Sentence sentence;
	private int id;
	private StateLock state;
	
	public JvnObjectImpl(int id, Sentence sentence){
		this.id = id;
		this.sentence = sentence;
		this.state = StateLock.NL;
	}
	
	public StateLock getState() {
		return state;
	}
	
	public void setState(StateLock s) {
		this.state = s;
	}
	
	public void jvnLockRead() throws JvnException {
		switch(state) {
			case RC:
				state = StateLock.R;
				break;
			case WC:
				state = StateLock.RWC;
				break;
			case NL:
				System.out.println("je lis");
				JvnServerImpl js = JvnServerImpl.jvnGetServer();
				sentence = (Sentence)js.jvnLockRead(id);
				System.out.println("sentence = " + sentence);
				state = StateLock.R;
				break;
		}
	}

	public void jvnLockWrite() throws JvnException {
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
					sentence = (Sentence)r;
				}
				state = StateLock.W;
				break;
		}
	}

	public void jvnUnLock() throws JvnException {
		switch(state) {
			case W:
				state = StateLock.WC;
				break;
			case R:
				state = StateLock.RC;
				break;
			case RWC:
				state = StateLock.WC;
				break;
		}
		synchronized(this) {
			notify();
		}
	}

	public int jvnGetObjectId() throws JvnException {		
		return id;
	}

	public Serializable jvnGetObjectState() throws JvnException {	
		return sentence;
	}

	public void jvnInvalidateReader() throws JvnException {
		if (state == StateLock.R) {
			try {
				synchronized(this) {
					wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state = StateLock.NL;
	}

	public Serializable jvnInvalidateWriter() throws JvnException {
		if (state == StateLock.W) {
			try {
				synchronized(this) {
					wait();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		state = StateLock.NL;
		return jvnGetObjectState();
	}

	public Serializable jvnInvalidateWriterForReader() throws JvnException {		
		switch(state) {
			case W:
				try {
					synchronized(this) {
						wait();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			case WC:
				state = StateLock.RC;
				break;
			case RWC:
				try {
					wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				state = StateLock.R;
				break;
		}
		
		return jvnGetObjectState();
	}
}
