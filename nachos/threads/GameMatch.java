package nachos.threads;

import nachos.machine.*;

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
    private Condition cond;
    private int numWaitingB = 0;
    private int numWaitingI = 0;
    private int numWaitingE = 0;
    private int numWaiting = 0; // First impl step
    private int matchNum = 0;

    private static int matchId = 0;

    /**
     * Allocate a new GameMatch specifying the number of player
     * threads of the same ability required to form a match.  Your
     * implementation may assume this number is always greater than zero.
     */
    public GameMatch (int numPlayersInMatch) {
      numPlayersNeeded = numPlayersInMatch;
      lock = new Lock();
      cond = new Condition(lock);
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
      lock.acquire();
      if (ability == abilityBeginner) numWaitingB++;
      else if (ability == abilityIntermediate) numWaitingI++;
      else if (ability == abilityExpert) numWaitingE++;
      else return -1;
      //numWaiting++;
      /*if (numWaiting < numPlayersNeeded) KThread.currentThread().sleep();
      else {
        numWaiting = 0;
        matchId++;
        this.matchNum = matchId;
        cond.wakeAll();
      }*/
      if (numWaitingB == numPlayersNeeded) {
        numWaitingB = 0;
        matchId++;
        this.matchNum = matchId;
        cond.wakeAll();
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
      lock.release();
	    return matchNum;
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
    }

    /** 
     * Test beginner levels with multiple matches
     */
    public static void matchTest2() {
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
      
      KThread beg3 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg3 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg3.setName("B3");

      KThread beg4 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityBeginner);
          System.out.println("beg4 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      beg4.setName("B4");

      beg2.fork();
      beg1.fork();
      beg4.fork();
      beg3.fork();
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
      
      KThread int1 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityIntermediate);
          System.out.println("int1 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      int1.setName("I1");

      KThread int2 = new KThread( new Runnable() {
        public void run() {
          int r = match.play(GameMatch.abilityIntermediate);
          System.out.println("int2 matched");
          Lib.assertTrue( r == 2, "expected a match number of 2" );
        }
      });
      int2.setName("I2");

      beg2.fork();
      int1.fork();
      int2.fork();
      beg1.fork();
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
		      // beginners should match with a match number of 1
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    beg1.setName("B1");

	    KThread beg2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg2 matched");
		      // beginners should match with a match number of 1
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

      // Assume join is not implemented, use yield to allow other
      // threads to run
      /*for (int i = 0; i < 10; i++) {
          KThread.currentThread().yield();
      }*/
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
		      // beginners should match with a match number of 1
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    beg1.setName("B1");

	    KThread beg2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg2 matched");
		      // beginners should match with a match number of 1
		      Lib.assertTrue(r == 1, "expected match number of 1");
		    }
	    });
	    beg2.setName("B2");

	    KThread beg3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityBeginner);
		      System.out.println ("beg3 matched");
		      // beginners should match with a match number of 1
		      Lib.assertTrue(r == 1, "expected match number of 1");
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
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    exp1.setName("E1");

	    KThread exp2 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 2, "expected match number of 2");
		    }
	    });
	    exp2.setName("E2");

	    KThread exp3 = new KThread( new Runnable () {
		    public void run() {
		      int r = match.play(GameMatch.abilityExpert);
		      Lib.assertTrue(r == 2, "expected match number of 2");
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
    }

    /**
     * Self Tests
     */
    public static void selfTest() {
      System.out.println("\nTesting GAMEMATCH\n");

      System.out.println("Testing 1 ability");
      matchTest1();

      System.out.println("Testing 1 ability multiple matches");
      matchTest2();

      System.out.println("Testing 2 ability multiple matches");
      matchTest3();

      System.out.println("Testing 3 ability 1 match");
      matchTest4();

      System.out.println("Testing 3 ability multiple match");
      matchTest5();

      System.out.println("\nGAMEMATCH tests done\n");
    }
}
