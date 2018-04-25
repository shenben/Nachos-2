package nachos.threads;

import nachos.machine.*;
<<<<<<< HEAD
import java.util.*;

/*
 * Source of help:
 * How to iterate through a java hashmap:
 * stackoverflow.com/questions/1066589/iterate-through-a-hashmap
 */
=======
import java.util.concurrent.*;
>>>>>>> 7d438fcc97c599c66d9e08ea783744d08fe99c02

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 * 
	 * <p>
	 * <b>Note</b>: Nachos will not function correctly with more than one alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() {
				timerInterrupt();
			}
		});
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread that
	 * should be run.
	 */
	public void timerInterrupt() {

<<<<<<< HEAD
    if( threads.isEmpty() ) return;
    // Iterate through the hashmap to get all the threads that have a wake
		// time earlier than curren time
		Iterator it = threads.entrySet().iterator();
		while( it.hasNext() ){
      Map.Entry pair = (Map.Entry) it.next();
			if( ((long)pair.getKey()) <= Machine.timer().getTime() ) {
        intStatus = Machine.interrupt().disable();
				((KThread)pair.getValue()).ready();
				Machine.interrupt().restore(intStatus);

				it.remove();
			}
			else break;
		}
    
=======
	  // Check if it is the wake time yet
		if( wakeTime <= Machine.timer().getTime() && wakeTime != -1) { 
      Machine.interrupt().disable();
		  alarmCallerThread.ready();
      Machine.interrupt().enable();
			wakeTime = -1;      
	  }
>>>>>>> 7d438fcc97c599c66d9e08ea783744d08fe99c02
		KThread.currentThread().yield();
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks, waking it up
	 * in the timer interrupt handler. The thread must be woken up (placed in
	 * the scheduler ready set) during the first timer interrupt where
	 * 
	 * <p>
	 * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
	 * 
	 * @param x the minimum number of clock ticks to wait.
	 * :wq
	 *
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
	  
		// If wait time is negative or zero, return without waiting
		if( x <= 0 ) return;
    wakeTime = Machine.timer().getTime() + x; // Getting the waitup time
    
		// Put the current thread to sleep
		alarmCallerThread = KThread.currentThread();

	  // If the wake time arealdy exit, minus 1 from it
		if( threads.containsKey( wakeTime )) wakeTime--;
		threads.put( wakeTime, alarmCallerThread );
    
		intStatus = Machine.interrupt().disable();
		alarmCallerThread.sleep();
    Machine.interrupt().restore(intStatus);
  }
		
	/* Alarm testing code */

  public static void alarmTestNormal() {
   // int durations[] = {1000, 10*1000, 100*1000};
    int durations[] = { 1000, 100, 10 };
		long t0, t1;

    for ( int d: durations ) {
		  System.out.println( "alarmTestNormal: wait for " + d + " ticks" );
      t0 = Machine.timer().getTime();
      ThreadedKernel.alarm.waitUntil(d);
      t1 = Machine.timer().getTime();
      System.out.println("alarmTestNormal: waited for " + (t1 - t0) + " ticks");
    }
  }

	public static void alarmTestNegative() {
    int negativeDurs[] = { -1, -100, -1000 };
		long t0, t1;

		for( int d: negativeDurs ) {
      System.out.println( "alarmTestNegative: wait for " + d + " ticks" );
			t0 = Machine.timer().getTime();
			ThreadedKernel.alarm.waitUntil(d);
			t1 = Machine.timer().getTime();
			System.out.println( "alarmTestNegative: waited for " + ( t1- t0 )
			                     + " ticks" );
		}
	}

  /********* Ping Test class **************/
	private static class PingTest implements Runnable {
    private int which;
		long durations[] = {500, 1400, 3500 };
		long t0, t1;

    PingTest( int which ) {
      this.which = which;
		}

		public void run() {
      for( int i = 0 ; i < 3 ; i ++ ) {
			  System.out.println( "*** thread " + which + " is waiting  for " + 
				                    durations[i] + " ticks");
        t0 = Machine.timer().getTime();
				ThreadedKernel.alarm.waitUntil( durations[i] );
				t1 = Machine.timer().getTime();
				System.out.println( "*** thread " + which + " waited for " + (t1-t0)
				                    + " ticks" );
			  KThread.currentThread().yield();
			}
		}
	}

  // Two threads test
	public static void alarmTestTwoThreads() {
	System.out.println( "Into two threads test");
    new KThread(new PingTest(1)).fork();
		new PingTest(0).run();
	}

  // Invoked from ThreadedKernel.selfTest()
  public static void selfTest() {
    System.out.println("... Normal Tests ...");
    alarmTestNormal();
    System.out.println("... Negative Tests ...");
		alarmTestNegative();
		alarmTestTwoThreads();
  }

	long wakeTime = -1;
	KThread alarmCallerThread = null;
  boolean intStatus;
	HashMap<Long, KThread> threads = new HashMap<>();
}
