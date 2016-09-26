package jvn;

import irc.Sentence;

import java.io.Serializable;

public class JvnObjectImpl implements JvnObject{

	private Sentence sentence;
	private int id;
	private StateLock state;
	
	public void jvnLockRead() throws JvnException {
		// TODO Auto-generated method stub
		
	}

	public void jvnLockWrite() throws JvnException {
		// TODO Auto-generated method stub
		
	}

	public void jvnUnLock() throws JvnException {
		// TODO Auto-generated method stub
		
	}

	public int jvnGetObjectId() throws JvnException {
		// TODO Auto-generated method stub
		return id;
	}

	public Serializable jvnGetObjectState() throws JvnException {
		// TODO Auto-generated method stub
		return null;
	}

	public void jvnInvalidateReader() throws JvnException {
		// TODO Auto-generated method stub
		
	}

	public Serializable jvnInvalidateWriter() throws JvnException {
		// TODO Auto-generated method stub
		return null;
	}

	public Serializable jvnInvalidateWriterForReader() throws JvnException {
		// TODO Auto-generated method stub
		return null;
	}

}
