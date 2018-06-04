package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
	/**
	 * Allocate a new VM kernel.
	 */
	public VMKernel() {
		super();
	}

	/**
	 * Initialize this kernel.
	 */
	public void initialize(String[] args) {
		super.initialize(args);
    numPhysPages = Machine.processor().getNumPhysPages();
    System.out.println("numPhysPages: " + numPhysPages);
    invTable = new TranslationEntry[numPhysPages];
    for (int i = 0; i < numPhysPages; i++) {
      invTable[i] = new TranslationEntry(-1, -1, true, false, false, false);
    }
    swapFile = ThreadedKernel.fileSystem.open("swapFile", true);
    pagesInMem = new LinkedList<Integer>();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		super.selfTest();
	}

	/**
	 * Start running user programs.
	 */
	public void run() {
		super.run();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
    swapFile.close();
    ThreadedKernel.fileSystem.remove("swapFile");
	}

  /**
   * Keep track of pages loaded into memory.
   */
  public void loadPage() {
  }

  /**
   * Clock Alg.
   */
  public int evictPage() {
    if (UserKernel.getNumFreePages() > 0) {
      System.out.println("Getting page from freePages");
      return UserKernel.giveOnePage();
    }

    if (pagesInMem.size() != numPhysPages) {
      System.out.println("Memory should be filled");
      return -1;
    }

    System.out.println("Getting page from evicted page");
    
    int lp = lastPos;
    do {
      Integer page = pagesInMem.get(lastPos);
      lastPos = (++lastPos) % numPhysPages;
      return page;
    } while (lastPos != lp);
  }

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

  public static TranslationEntry[] invTable;

  public static OpenFile swapFile;

  public static int numPhysPages;

  public static LinkedList<Integer> pagesInMem;

  private int lastPos = 0;
}
