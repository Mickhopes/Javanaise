package irc;

import java.io.Serializable;

import jvn.JvnObject;
import jvn.JvnServerImplBurst;

public class Burst {
	static final int NB_THREADS = 10;
	
	public static void main(String[] args) {
		try {
			if (args.length == 0 || args.length > 2) {
				System.out.println("Usage : java -jar Burst.jar <nb_threads> <nb_test_per_thread>");
				System.exit(-1);
			}
			
			// We get the args
			int nbThreads = 0;
			int nbTest = 0;
			try {
				nbThreads = Integer.parseInt(args[0]);
				nbTest = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Arguments must be integer\n");
				System.out.println("Usage : java -jar Burst.jar <nb_threads> <nb_test_per_thread>");
				System.exit(-1);
			}
			
			// initialize JVN
			JvnServerImplBurst js = new JvnServerImplBurst();
			
			// look up the IRC object in the JVN server
			// if not found, create it, and register it in the JVN server
			JvnObject jo = js.jvnLookupObject("Burst");
			   
			if (jo == null) {
				jo = js.jvnCreateObject((Serializable) new String());
				// after creation, I have a write lock on the object
				jo.jvnUnLock();
				js.jvnRegisterObject("Burst", jo);
			}
			
			// We create a certain number of threads
			Thread[] threads = new Thread[nbThreads];
			for(int i = 0; i < nbThreads; i++) {
				threads[i] = new Thread(new BurstThread(i, nbTest));
			}
			
			// We launch our threads
			for(int i = 0; i < nbThreads; i++) {
				threads[i].start();
			}
			
			// We wait for them to finish
			for(int i = 0; i < nbThreads; i++) {
				threads[i].join();
			}
			
			// We end the program
			System.exit(1);
		   
		   } catch (Exception e) {
			   System.out.println("IRC problem : " + e.getMessage());
			   e.printStackTrace();
		   }
	}
}
