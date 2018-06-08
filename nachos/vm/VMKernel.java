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
		boolean intStatus = Machine.interrupt().disable();
		if( freeSwapPages == null ) {
      freeSwapPages = new LinkedList<Integer>();
			swapLock = new Lock();
		}
		Machine.interrupt().restore(intStatus);
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
	}

	// dummy variables to make javac smarter
	private static VMProcess dummy1 = null;

	private static final char dbgVM = 'v';

	public static LinkedList<Integer> freeSwapPages;

	public static Lock swapLock;

	public static int giveSwapPage(){
    swapLock.acquire();
		int swapPage = freeSwapPages.size();
		freeSwapPages.add(swapPage);
		swapLock.release();
		return swapPage;
	}
}
