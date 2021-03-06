package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 * 
 * <p>
 * You must implement this.
 * 
 * @see nachos.threads.Condition
 */
public class Condition2 {
	/**
	 * Allocate a new condition variable.
	 * 
	 * @param conditionLock the lock associated with this condition variable.
	 * The current thread must hold this lock whenever it uses <tt>sleep()</tt>,
	 * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
	 */
	public Condition2(Lock conditionLock) {
		this.conditionLock = conditionLock;

		waitQueue = new LinkedList<KThread>();
	}

	/**
	 * Atomically release the associated lock and go to sleep on this condition
	 * variable until another thread wakes it using <tt>wake()</tt>. The current
	 * thread must hold the associated lock. The thread will automatically
	 * reacquire the lock before <tt>sleep()</tt> returns.
	 */
	public void sleep() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());
 
		conditionLock.release();
		boolean intStatus = Machine.interrupt().disable();

    // Add current thread to the waitQueue, and make it sleep
		waitQueue.addFirst( KThread.currentThread() );
		KThread.currentThread().sleep();

    Machine.interrupt().restore( intStatus );
		conditionLock.acquire();
	}

	/**
	 * Wake up at most one thread sleeping on this condition variable. The
	 * current thread must hold the associated lock.
	 */
	public void wake() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

    boolean intState = Machine.interrupt().disable();

		if( waitQueue.size() != 0 ) {
      KThread sleepingThread = (KThread)waitQueue.removeFirst();
			sleepingThread.ready();
		}

		Machine.interrupt().restore( intState );
	}

	/**
	 * Wake up all threads sleeping on this condition variable. The current
	 * thread must hold the associated lock.
	 */
	public void wakeAll() {
		Lib.assertTrue(conditionLock.isHeldByCurrentThread());

		while( waitQueue.size() > 0 ) {
      wake();
		}
	}

	private Lock conditionLock;

	private  LinkedList<KThread> waitQueue;// The wait queue for threads


	/*************************** test **********************************/
	/**
	 * Simple test for this class 
	 */
	private static class InterlockTest {
    private static Lock lock;
		private static Condition2 cv;
		private static class Interlocker implements Runnable {
      public void run() {
        lock.acquire();
				for( int i = 0 ; i < 10 ; i ++ ) {
          System.out.println( KThread.currentThread().getName());
					cv.wake(); // Signal
					cv.sleep(); // wait
				}
				lock.release();
			}
		}// End of InterLocker class

    public InterlockTest() {
      lock = new Lock();
			cv = new Condition2(lock);
			KThread ping = new KThread( new Interlocker() );
			ping.setName( "ping" );
			
			KThread pong = new KThread( new Interlocker() );
			pong.setName( "pong" );
			
			ping.fork();
			pong.fork();

			ping.join();
			//for(int i = 0 ; i < 50 ; i++ ) { KThread.currentThread().yield(); }
		}
	}

  /**
	 * A more complicated test for this class 
	 */
	public static void cvTest5() {
    final Lock lock = new Lock();
	//	 final Condition empty = new Condition( lock );
		final Condition2 empty = new Condition2( lock );
		final LinkedList<Integer> list = new LinkedList<>();

		KThread consumer = new KThread( new Runnable () {
        public void run() {
          lock.acquire();
					while( list.isEmpty() ) {
            empty.sleep();
					}
					Lib.assertTrue( list.size() == 5, "List should have 5 values." );
					while( !list.isEmpty() ) {
            // Context switch for the fun of it.
						KThread.currentThread().yield();
						System.out.println( "Removed " + list.removeFirst());
					}
					lock.release();
				}
		  });

		KThread producer = new KThread( new Runnable () {
        public void run() {
          lock.acquire();
					for( int i = 0 ; i < 5 ; i ++ ) {
            list.add(i);
						System.out.println( "Added " + i );

						KThread.currentThread().yield();
					}
					empty.wake();
					lock.release();
				}
		  });

		consumer.setName( "Consumer" );
		producer.setName( "Producer" );
		consumer.fork();
		producer.fork();

		consumer.join();
		producer.join();

		//for(int i=0;i<50;i++) {KThread.currentThread().yield();}
	}

	/**
	 * Test to see if sleep blocks the thread
	 */
	public static void sleepBlockingTest() {
    final Lock lock = new Lock();
	//	final Condition empty = new Condition( lock );
		final Condition2 empty = new Condition2( lock );

		KThread consumer = new KThread( new Runnable() {
        public void run() {
          lock.acquire();
					for( int i = 5 ; i >= 0 ; i-- ) {
					  if( i == 3 ) empty.sleep();
            System.out.println( "Self destruction ... count down " + i );
					  KThread.currentThread().yield();
				  }
					lock.release();
			  }
		  });

	 consumer.setName( "Consumer" ).fork();
	 
	 for( int i = 0 ; i < 5; i++ ) {
	  // if( i ==  )
     System.out.println( "Trying to stop death..." );
		 KThread.currentThread().yield();
	 }	
	}

	/**
	 * Test to see if wake only wakes up on thread
	 */
	public static void wakeTest() {
	  final Lock lock = new Lock();
	//	 final Condition empty = new Condition( lock );
		final Condition2 empty = new Condition2( lock );
		final LinkedList<Integer> list = new LinkedList<>();
		final LinkedList<Integer> list2 = new LinkedList<>();

		KThread consumer = new KThread( new Runnable () {
        public void run() {
          lock.acquire();
					while( list.isEmpty() ) {
					  System.out.println("Consumer1 sleeping.");
            empty.sleep();
					}
					Lib.assertTrue( list.size() == 5, "List should have 5 values." );
					while( !list.isEmpty() ) {
            // Context switch for the fun of it.
						KThread.currentThread().yield();
						System.out.println( "Removed from list " + list.removeFirst());
					}
				//	empty.wake();
					lock.release();
				}
		  });

    KThread consumer2 = new KThread( new Runnable () {
        public void run() {
          lock.acquire();
					while( list2.isEmpty() ) {
					  System.out.println( "Consumer2 sleeping.");
            empty.sleep();
					}
					Lib.assertTrue( list2.size() == 5, "List 2 should have 5 values." );
					while( !list2.isEmpty() ) {
            // Context switch for the fun of it.
						KThread.currentThread().yield();
						System.out.println( "Removed from list2 " + list2.removeFirst());
					}
				//	empty.wake();
					lock.release();
				}
		  });


		KThread producer = new KThread( new Runnable () {
        public void run() {
          lock.acquire();
					for( int i = 0 ; i < 5 ; i ++ ) {
            list.add(i);
						System.out.println( "Added to list " + i );
						list2.add(5-i);
						System.out.println( "Added to list2 " + (5-i));

						KThread.currentThread().yield();
					}
					empty.wakeAll();
				 // empty.wake();
					lock.release();
				}
		  });

		consumer.setName( "Consumer" );
		consumer2.setName( "Consumer2" );
		producer.setName( "Producer" );
		consumer.fork();
		consumer2.fork();
		producer.fork();

		consumer.join();
		consumer2.join();
		producer.join();

			
	}

	public static void selfTest() {
	  System.out.println( "\nTesting Condition2." );
		System.out.println( "\nSimple test." );
    new InterlockTest();

		System.out.println( "\nVERY complicated test." );
		cvTest5();

		System.out.println( "\nTest if conditoin blocks the current thread in sleep" );
		sleepBlockingTest();

		System.out.println( "\nTest if wake() only wakes up one thread at a time." );
    wakeTest();

		System.out.println( "\nFinished testing condition2.\n");
	}
}
