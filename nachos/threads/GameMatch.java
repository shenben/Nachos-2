package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * A <i>GameMatch</i> groups together player threads of the same
 * ability into fixed-sized groups to play matches with each other.
 * Implement the class <i>GameMatch</i> using <i>Lock</i> and
 * <i>Condition</i> to synchronize player threads into groups.
 */
public class GameMatch {
    
    /* Three levels of player ability. */
    public static final int abilityBeginner = 1,
	    abilityIntermediate = 2,
	    abilityExpert = 3;
    
    private int numPlayersNeeded = 0;
    private Lock lock;
    private Condition condBeginner;
		private Condition condInter;
		private Condition condExp;
    private int numWaitingB = 0;
    private int numWaitingI = 0;
    private int numWaitingE = 0;
    private int numWaiting = 0; // First impl step
    private int matchNum = 0;

    private static int matchId;

    private LinkedList<KThread> begQ;
    private LinkedList<KThread> intQ;
    private LinkedList<KThread> expQ;

    /**
     * Allocate a new GameMatch specifying the number of player
     * threads of the same ability required to form a match.  Your
     * implementation may assume this number is always greater than zero.
     */
    public GameMatch (int numPlayersInMatch) {
      numPlayersNeeded = numPlayersInMatch;
      matchId = 0;
      lock = new Lock();

      condBeginner = new Condition(lock);
			condInter = new Condition(lock);
			condExp = new Condition(lock);

      begQ = new LinkedList<KThread>();
      intQ = new LinkedList<KThread>();
      expQ = new LinkedList<KThread>();
    }

    /**
     * Wait for the required number of player threads of the same
     * ability to form a game match, and only return when a game match
     * is formed.  Many matches may be formed over time, but any one
     * player thread can be assigned to only one match.
     *
     * Returns the match number of the formed match.  The first match
     * returned has match number 1, and every subsequent match
     * increments the match number by one, independent of ability.  No
     * two matches should have the same match number, match numbers
     * should be strictly monotonically increasing, and there should
     * be no gaps between match numbers.
     * 
     * @param ability should be one of abilityBeginner, abilityIntermediate,
     * or abilityExpert; return -1 otherwise.
     */
    public int play (int ability) {
     // lock.acquire();
      if (ability == abilityBeginner) {
			  lock.acquire();
			  numWaitingB++;
				if( numWaitingB == numPlayersNeeded ) {
					numWaitingB = 0;
				  KThread.currentThread().setMatchNum(++matchId);
				  condBeginner.wakeAll();
          for (int i = 0; i < numPlayersNeeded - 1; i++) {
            begQ.poll().setMatchNum(KThread.currentThread().getMatchNum());
          }
				}
				else {
          begQ.add(KThread.currentThread());
          condBeginner.sleep();
        }
				lock.release();
			}
      else if (ability == abilityIntermediate) {
			  lock.acquire();
			  numWaitingI++;
				if( numWaitingI == numPlayersNeeded ) {
          numWaitingI = 0;
				  KThread.currentThread().setMatchNum(++matchId);
					condInter.wakeAll();
          for (int i = 0; i < numPlayersNeeded - 1; i++) {
            intQ.poll().setMatchNum(KThread.currentThread().getMatchNum());
          }
				}
				else {
          intQ.add(KThread.currentThread());
          condInter.sleep();
        }
				lock.release();
			}
      else if (ability == abilityExpert) {
			  lock.acquire();
			  numWaitingE++;
				if( numWaitingE == numPlayersNeeded ) {
          numWaitingE = 0;
				  KThread.currentThread().setMatchNum(++matchId);
					condExp.wakeAll();
          for (int i = 0; i < numPlayersNeeded - 1; i++) {
            expQ.poll().setMatchNum(KThread.currentThread().getMatchNum());
          }
				}
				else {
          expQ.add(KThread.currentThread());
				  condExp.sleep();
				}
				lock.release();
			}
      else return -1;
      //numWaiting++;
      /*if (numWaiting < numPlayersNeeded) KThread.currentThread().sleep();
      else {
        numWaiting = 0;
        matchId++;
        this.matchNum = matchId;
        cond.wakeAll();
      }*/
     /* if (numWaitingB == numPlayersNeeded) {
        numWaitingB = 0;
        matchId++;
        this.matchNum = matchId;
        condBeginner.wakeAll();
      } else if (numWaitingI == numPlayersNeeded) {
        numWaitingI = 0;
        matchId++;
        this.matchNum = matchId;
        cond.wakeAll();
      } else if (numWaitingE == numPlayersNeeded) {
        numWaitingE = 0;
        matchId++;
        this.matchNum = matchId;
        cond.wakeAll();
      } else {
        //KThread.currentThread().sleep();
				cond.sleep();
      }
      lock.release();*/
	    return KThread.currentThread().getMatchNum();
    }

    /************************** TESTS **************************/

    /**
     * Test 1 level 1 match
     */
    public static void matchTest1() {
      final GameMatch match = new GameMatch(2);

      KThread beg1 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg1 matched");
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      beg1.setName("B1");

      KThread beg2 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg2 matched");
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      beg2.setName("B2");
      
      beg1.fork();
      beg2.fork();

			beg1.join();
			beg2.join();
    }

    /** 
     * Test beginner levels with multiple matches
     */
    public static void matchTest2() {
      final GameMatch match = new GameMatch(2);

      KThread beg1 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("r: " + r);
          System.out.println("beg1 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg1.setName("B1");

      KThread beg2 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("r: " + r);
          System.out.println("beg2 matched");
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      beg2.setName("B2");
      
      KThread beg3 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg3 matched");
          System.out.println("r: " + r);
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg3.setName("B3");

      KThread beg4 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg4 matched");
          System.out.println("r: " + r);
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      beg4.setName("B4");

      beg2.fork();
      beg4.fork();
      beg1.fork();
      beg3.fork();

      beg2.join();
      beg4.join();
      beg1.join();
      beg3.join();
    }

    /**
     * Test beginner + intermediate player levels
     */
    public static void matchTest3() {
      final GameMatch match = new GameMatch(2);

      KThread beg1 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg1 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg1.setName("B1");

      KThread beg2 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg2 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg2.setName("B2");
      
      KThread int1 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityIntermediate);
          System.out.println("int1 matched");
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      int1.setName("I1");

      KThread int2 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityIntermediate);
          System.out.println("int2 matched");
          Lib.assertTrue( r == 1, "expected a match number of 1" );
        }
      });
      int2.setName("I2");

      beg2.fork();
      int1.fork();
      int2.fork();
      beg1.fork();

      beg2.join();
      int1.join();
      int2.join();
      beg1.join();
    }

    /**
     * Test all levels but only one match
     */
    public static void matchTest4 () {
	    final GameMatch match = new GameMatch(2);

	    // Instantiate the threads
	    KThread beg1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg1 matched");
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    beg1.setName("B1");

	    KThread beg2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg2 matched");
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    beg2.setName("B2");

	    KThread int1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
		      Lib.assertNotReached("int1 should not have matched!");
		    }
	    });
	    int1.setName("I1");

	    KThread exp1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertNotReached("exp1 should not have matched!");
		    }
	    });
	    exp1.setName("E1");

      // Run the threads.  The beginner threads should successfully
      // form a match, the other threads should not.  The outcome
      // should be the same independent of the order in which threads
      // are forked.
      beg1.fork();
      int1.fork();
      exp1.fork();
      beg2.fork();

      beg1.join();
      //int1.join();
      //exp1.join();
      beg2.join();
    }

    /**
     * Test all levels multiple matches
     */
    public static void matchTest5 () {
	    final GameMatch match = new GameMatch(3);

	    // Instantiate the threads
	    KThread beg1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg1 matched");
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg1.setName("B1");

	    KThread beg2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg2 matched");
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg2.setName("B2");

	    KThread beg3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg3 matched");
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg3.setName("B3");

	    KThread int1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int1.setName("I1");

	    KThread int2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int2.setName("I2");

	    KThread int3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int3.setName("I3");

	    KThread exp1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp1.setName("E1");

	    KThread exp2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp2.setName("E2");

	    KThread exp3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp3.setName("E3");

      // Run the threads.  The beginner threads should successfully
      // form a match, the other threads should not.  The outcome
      // should be the same independent of the order in which threads
      // are forked.
      beg1.fork();
      exp1.fork();
      exp2.fork();
      int1.fork();
      exp3.fork();
      beg3.fork();
      int2.fork();
      beg2.fork();
      int3.fork();

      beg1.join();
      exp1.join();
      exp2.join();
      int1.join();
      exp3.join();
      beg3.join();
      int2.join();
      beg2.join();
      int3.join();
    }

    /**
     * Test all levels multiple matches in different orders
     */
    public static void matchTest6 () {
	    final GameMatch match = new GameMatch(3);

	    // Instantiate the threads
	    KThread beg1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg1 matched with r = " + r);
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg1.setName("B1");

	    KThread beg2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg2 matched with r = " + r);
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg2.setName("B2");

	    KThread beg3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg3 matched");
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg3.setName("B3");

	    KThread int1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
          System.out.println ("int 1st  matched");

		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int1.setName("I1");

	    KThread int2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
  System.out.println ("int 2 matched");

		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int2.setName("I2");

	    KThread int3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityIntermediate);
	 System.out.println ("int 3 matched");

		      Lib.assertTrue(r == 3, "expected match number of 3");
		    }
	    });
	    int3.setName("I3");

	    KThread exp1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
					System.out.println( "exp1 match with r = "+ r);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp1.setName("E1");

	    KThread exp2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
					System.out.println( "exp2 matched with r = " + r);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp2.setName("E2");

	    KThread exp3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
					System.out.println( "exp3 matched with r = " + r);
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    exp3.setName("E3");

      KThread exp4 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 4, "expected match number of 4");
		    }
	    });
	    exp4.setName("E4");

			KThread exp5 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 4, "expected match number of 4");
		    }
	    });
	    exp5.setName("E5");

      KThread exp6 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 4, "expected match number of 4");
		    }
	    });
	    exp6.setName("E6");

      // Run the threads.  The beginner threads should successfully
      // form a match, the other threads should not.  The outcome
      // should be the same independent of the order in which threads
      // are forked.
      beg1.fork();
      exp1.fork();
      exp2.fork();
      int1.fork();
      exp3.fork();
      beg3.fork();
      int2.fork();
      beg2.fork();
      int3.fork();

			exp4.fork();
			exp5.fork();
			exp6.fork();

			beg1.join();
			beg2.join();
			beg3.join();
			int1.join();
			int2.join();
			int3.join();
			exp1.join();
			exp2.join();
			exp3.join();
			exp4.join();
			exp5.join();
			exp6.join();
    }

    /**
     * Test one uncompleted beginner game
     */
    public static void matchTest7() {
	    final GameMatch match = new GameMatch(3);

	    // Instantiate the threads
	    KThread beg1 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg1 matched with r = " + r);
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    beg1.setName("B1");

      beg1.fork();
      
      KThread.currentThread().yield();
      KThread.currentThread().yield();
      KThread.currentThread().yield();
    }

    /**
     * Self Tests
     */
    public static void selfTest() {
      System.out.println("\nTesting GAMEMATCH\n");

      System.out.println("---Test 1---");
      matchTest1();

      System.out.println("---Test 2---");
      matchTest2();

      System.out.println("---Test 3---");
      matchTest3();

      System.out.println("---Test 4---");
      matchTest4();

      System.out.println("---Test 5---");
      matchTest5();

      System.out.println("---Test 6---");
	    matchTest6();

      System.out.println("---Test 7---");
	    matchTest7();

      System.out.println("\nGAMEMATCH tests done\n");
    }
}
