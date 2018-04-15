package nachos.threads;

import nachos.machine.*;

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

	  // Check if it is the wake time yet
		if( wakeTime <= Machine.timer().getTime() && wakeTime != -1) { 
		  alarmCallerThread.ready();
			wakeTime = -1;      
	  }
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
	 * 
	 * @see nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
	  
		// If wait time is negative or zero, return without waiting
		if( x <= 0 ) return;
    wakeTime = Machine.timer().getTime() + x; // Getting the waitup time
    
		// Put the current thread to sleep
		alarmCallerThread = KThread.currentThread();
    
		Machine.interrupt().disable();
		alarmCallerThread.sleep();

  }
		
	/* Alarm testing code */

  public static void alarmTestNormal() {
    int durations[] = {1000, 10*1000, 100*1000};
    long t0, t1;

    for (int d: durations) {
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

  // Invoked from ThreadedKernel.selfTest()
  public static void selfTest() {
    alarmTestNormal();
		alarmTestNegative();
  }

	long wakeTime = -1;
	KThread alarmCallerThread = null;
}
