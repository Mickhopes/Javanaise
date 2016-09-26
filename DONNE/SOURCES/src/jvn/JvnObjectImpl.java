package jvn;

import irc.Sentence;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{

	private Sentence sentence;
	private int id;
	private StateLock state;
	
	public void jvnLockRead() throws JvnException {
		state = StateLock.R;
	}

	public void jvnLockWrite() throws JvnException {		
		state = StateLock.W;
	}

	public void jvnUnLock() throws JvnException {	
		state = StateLock.NL;
	}

	public int jvnGetObjectId() throws JvnException {		
		return id;
	}

	public Serializable jvnGetObjectState() throws JvnException {	
		return sentence;
	}

	public void jvnInvalidateReader() throws JvnException {	
		
	}

	public Serializable jvnInvalidateWriter() throws JvnException {
		return null;
	}

	public Serializable jvnInvalidateWriterForReader() throws JvnException {		
		return null;
	}

}
