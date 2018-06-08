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
    swapFile = ThreadedKernel.fileSystem.open(swapName, true);
    numSwapPages = 20;
    if (numSwapPages == -1) {
      System.out.println("Find other way to get length");
    }
    System.out.println("numSwapPages = " + numSwapPages);
    freeSwapPages = new LinkedList<Integer>();
    for (int i = 0; i < numSwapPages; i++) {
      freeSwapPages.add((Integer) i);
    }
    pagesInMem = new LinkedList<Integer>();
    processMap = new HashMap();
    //lock = new Lock();
	}

	/**
	 * Test this kernel.
	 */
	public void selfTest() {
		//super.selfTest();
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
    ThreadedKernel.fileSystem.remove(swapName);
	}

  /**
   * Keep track of pages loaded into memory.
   */
  public void loadPage() {
  }

  /**
   * Clock Alg.
   */
  public static int evictPage() {
    if (UserKernel.getNumFreePages() > 0) {
      System.out.println("Getting page from freePages");
      return UserKernel.giveOnePage();
    }

    if (pagesInMem.size() != numPhysPages) {
      System.out.println("Memory should be filled, only has " + pagesInMem.size()
          + " out of " + numPhysPages);
      for (int i = 0; i < pagesInMem.size(); i++) {
        System.out.println("page @ " + i+ " = " + pagesInMem.get(i));
      }
      return -1;
    }

    System.out.println("***************** Getting page by evicting page ******************* %%%");

    processLock.acquire();

    System.out.println("Checking invTable...");
    for (int i = 0; i < numPhysPages; i++) {
      System.out.println("invTable[" + i + "].ppn = " + invTable[i].ppn);
    }
    
    //processLock.acquire();
    int index = pagesInMem.indexOf(lastPage);
    //processLock.release();
    if (index == -1) {
      System.out.println("Resetting index");
      index = 0;
    }
    int front = index;

    do {
      System.out.println("Clock alg INDEX ==== " + index);
      // if (pinned) continue; // update index
      //processLock.acquire();
      Integer ppn = pagesInMem.get(index);
      int pid = VMKernel.invTable[ppn].ppn;
      //processLock.release();

      System.out.println("###### BEFORE - Checking this pages ITE.......");
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].vpn        = " + invTable[ppn].vpn);
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].ppn (pid)  = " + invTable[ppn].ppn);
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].valid      = " + invTable[ppn].valid);
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].readOnly   = " + invTable[ppn].readOnly);
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].dirty      = " + invTable[ppn].dirty);
      System.out.println("-----------");
      System.out.println("iT["+ppn+"].used       = " + invTable[ppn].used);
      System.out.println("-----------");

      if (!invTable[ppn].used) {
        //processLock.acquire();
        VMProcess vmp = processMap.get(pid);
        if (vmp == null) {
          System.out.println("ppn = " + ppn);
          System.out.println("Process " + pid + " was not inserted into processMap");
          System.out.println("Size of processMap = " + processMap.size());
          System.out.println("item = " + processMap.get(0));
          return -1;
        }
        TranslationEntry[] pt = vmp.getPageTable();
        //processLock.release();
        if (!invTable[ppn].readOnly && invTable[ppn].dirty) {
          // Request swap page
          int spn = getSwapPage(); //locked
          if (spn == -1) {
            System.out.println("no more free swap pages :(");
            return -1;
          }
          swapFile.write(spn * pageSize, Machine.processor().getMemory(), 
              ppn * pageSize, pageSize);
          // Store spn in vpn entry of process pageTable
          pt[invTable[ppn].vpn].vpn = spn;
          System.out.println("Set spn of process " + pid + " at vpn " 
              + invTable[ppn].vpn + " to " + spn);
          // DEBUGGING STUFF
          byte exp[] = new byte[pageSize];
          for (int i = 0; i < pageSize; i++) {
            exp[i] = Machine.processor().getMemory()[(ppn * pageSize) + i];
          }
          vmp.expBytes.put(invTable[ppn].vpn, exp);
          System.out.println("exp hashmap size = " + vmp.expBytes.size() + " at vpn = " + invTable[ppn].vpn);
        }
        // Invalidate PTE + ITE for process w this evicted page
        invTable[ppn].valid = false;
        pt[invTable[ppn].vpn].valid = false;
        vmp.setPageTable(pt);
        untrackPhysPage(ppn); // reduces size of pagesInMem
        System.out.println("UNTRACKING PAGE " + ppn);

        System.out.println("Evicting page mapped to VP - " + invTable[ppn].vpn);
        System.out.println("###### AFTER - Checking this pages ITE.......");
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].vpn        = " + invTable[ppn].vpn);
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].ppn (pid)  = " + invTable[ppn].ppn);
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].valid      = " + invTable[ppn].valid);
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].readOnly   = " + invTable[ppn].readOnly);
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].dirty      = " + invTable[ppn].dirty);
        System.out.println("-----------");
        System.out.println("iT["+ppn+"].used       = " + invTable[ppn].used);
        System.out.println("-----------");
        
        index = (++index) % pagesInMem.size();//numPhysPages;
        lastPage = pagesInMem.get(index);
        processLock.release();
        return ppn;
      } else {
        invTable[ppn].used = false;
      }
      index = (++index) % numPhysPages;
    } while (index != front);

    // delay request CV

    processLock.release();

    return -1;
  }

  private static int getSwapPage() {
    //processLock.acquire();
    if (freeSwapPages.size() <= 0) {
      freeSwapPages.add(numSwapPages++);
    }
    int val = (int) freeSwapPages.remove();
    //processLock.release();
    return val;
  }

  public static void addProcess(Integer pid, VMProcess vmp) {
    processMap.put(pid, vmp);
  }

  public static void removeProcess(Integer pid) {
    processMap.remove(pid);
  }

  public static void trackPhysPage(Integer ppn) {
    pagesInMem.add(ppn);
  }

  public static boolean untrackPhysPage(Integer ppn) {
    return pagesInMem.remove(ppn);
  }

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

  public static TranslationEntry[] invTable;

  public static OpenFile swapFile;

  public static String swapName = "swapFile";

  private static LinkedList<Integer> freeSwapPages;

  private static int numSwapPages;

  public static int numPhysPages;

  public static LinkedList<Integer> pagesInMem;

  private static Integer lastPage = null;

	private static final int pageSize = Processor.pageSize;

  public static HashMap<Integer, VMProcess> processMap;

  //private static Lock lock;
  //private Lock macroLock;
}
