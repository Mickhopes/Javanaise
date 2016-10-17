package irc;

import jvn.JvnObject;
import jvn.JvnServerImplBurst;

public class BurstThread implements Runnable {
	private int nbTest;
	private int numThread;
	
	public BurstThread(int _numThread, int _nbTest) {
		numThread = _numThread;
		nbTest = _nbTest;
	}
	
	@Override
	public void run() {
		try {
			// initialize JVN
			JvnServerImplBurst js = new JvnServerImplBurst();
			
			// look up the IRC object in the JVN server
			// if not found, create it, and register it in the JVN server
			JvnObject jo = js.jvnLookupObject("Burst");
			
			for(int i = 0; i < nbTest; i++) {
				int nb = (int)(Math.random()*10);
				if (nb > 5) {
					System.out.println("Thread " + numThread + " : WriteLock");
					jo.jvnLockWrite();
					
					Thread.sleep(200);
					
					System.out.println("Thread " + numThread + " : Unlock");
					jo.jvnUnLock();
				} else {
					System.out.println("Thread " + numThread + " : ReadLock");
					jo.jvnLockRead();
					
					Thread.sleep(200);
					
					System.out.println("Thread " + numThread + " : Unlock");
					jo.jvnUnLock();
				}
			}
			
			
		} catch (Exception e) {
			System.err.println("Error when bursting (Thread " + numThread + ") : " + e.getMessage());
			e.printStackTrace();
		}
		
	}

}
