package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import java.util.*;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
	/**
	 * Allocate a new user kernel.
	 */
	public UserKernel() {
		super();
	}

	/**
	 * Initialize this kernel. Creates a synchronized console and sets the
	 * processor's exception handler.
	 */
	public void initialize(String[] args) {
		super.initialize(args);

		console = new SynchConsole(Machine.console());

		Machine.processor().setExceptionHandler(new Runnable() {
			public void run() {
				exceptionHandler();
			}
		});

		// Initialize freePhyPage if it has not been initialized
		boolean intStatus = Machine.interrupt().disable();
		if( freePhyPages == null ) {
		  freePhyPages = new LinkedList<Integer>();
      for( int i = 0 ; i < Machine.processor().getNumPhysPages(); i ++ ) {
        // Add each page to the table
				//freePhyPages.addFirst(i);
				freePhyPages.add(i);
			}
		}
		if( pageLock == null ) pageLock = new Lock();
		if( processLock == null ) processLock = new Lock();
		if( newProcLock == null ) newProcLock = new Lock();
		if( allProcesses == null ) 
		  allProcesses = new HashMap<Integer, UserProcess>();
		Machine.interrupt().restore(intStatus);
	}

	/**
	 * Test the console device.
	 */
	public void selfTest() {
		super.selfTest();

		System.out.println("Testing the console device. Typed characters");
		System.out.println("will be echoed until q is typed.");

		char c;

		do {
			c = (char) console.readByte(true);
			console.writeByte(c);
		} while (c != 'q');

		System.out.println("");
	}

	/**
	 * Returns the current process.
	 * 
	 * @return the current process, or <tt>null</tt> if no process is current.
	 */
	public static UserProcess currentProcess() {
		if (!(KThread.currentThread() instanceof UThread))
			return null;

		return ((UThread) KThread.currentThread()).process;
	}

	/**
	 * The exception handler. This handler is called by the processor whenever a
	 * user instruction causes a processor exception.
	 * 
	 * <p>
	 * When the exception handler is invoked, interrupts are enabled, and the
	 * processor's cause register contains an integer identifying the cause of
	 * the exception (see the <tt>exceptionZZZ</tt> constants in the
	 * <tt>Processor</tt> class). If the exception involves a bad virtual
	 * address (e.g. page fault, TLB miss, read-only, bus error, or address
	 * error), the processor's BadVAddr register identifies the virtual address
	 * that caused the exception.
	 */
	public void exceptionHandler() {
		Lib.assertTrue(KThread.currentThread() instanceof UThread);

		UserProcess process = ((UThread) KThread.currentThread()).process;
		int cause = Machine.processor().readRegister(Processor.regCause);
		process.handleException(cause);
	}

	/**
	 * Start running user programs, by creating a process and running a shell
	 * program in it. The name of the shell program it must run is returned by
	 * <tt>Machine.getShellProgramName()</tt>.
	 * 
	 * @see nachos.machine.Machine#getShellProgramName
	 */
	public void run() {
		super.run();

		UserProcess process = UserProcess.newUserProcess();
		processLock.acquire();
		process.setPID( numProcess );
		increaseProcess();
		ROOT = process;
		processLock.release();

		this.addProcess( process );

		String shellProgram = Machine.getShellProgramName();
		Lib.assertTrue(process.execute(shellProgram, new String[] {}));

		KThread.currentThread().finish();
	}

	/**
	 * Terminate this kernel. Never returns.
	 */
	public void terminate() {
		super.terminate();
	}

	/** Globally accessible reference to the synchronized console. */
	public static SynchConsole console;

	// dummy variables to make javac smarter
	private static Coff dummy1 = null;

  /** To keep track of how many free pages there are */
	protected static LinkedList<Integer> freePhyPages;
	protected static Lock pageLock;
	// Helper functions
  public static int getNumFreePages() {
    int pageNum = freePhyPages.size();
		return pageNum;
	}

	/**
	 * giveOnePage() 
	 * Gives out one more page
	 *
	 * returns the page number
	 * or -1 if error occurs
	 */
	public static int giveOnePage() {
	  pageLock.acquire();
    if( freePhyPages.size() <= 0 ){
		  pageLock.release();
		  return -1;
		}

		int availPage = freePhyPages.pop();
    pageLock.release();
		return availPage;
	}

	/**
	 * receiveOnePage()
	 * @param i - the number of the physical page
	 * Receives one page that the process gives back
	 * returns the number of pages returned
	 * or -1 if an error occured
	 */
	public static int receiveOnePage(int i ){
		if( i < 0 || i >= Machine.processor().getNumPhysPages() ) return -1;
		pageLock.acquire();
		freePhyPages.add(i);
		pageLock.release();
//System.out.println( "In kernel we have " + freePhyPages.size() + " left from "
  //                     + Machine.processor().getNumPhysPages() + " total pages");
		return i;
	}

  /** To handle multiprocess */
	public static Lock processLock;
	protected static int numProcess = 0;
	protected static int nextID = 0;
	public static UserProcess ROOT;
	public static int increaseProcess() {
	  //processLock.acquire();
    numProcess++;
		nextID ++;
		//processLock.release();
		return nextID;
	}
	public static int getNumProcess(){
    return numProcess;
	}
	public static int decreaseProcess(){
    processLock.acquire();
		numProcess--;
		processLock.release();
		return numProcess;
	}

	public static HashMap<Integer, UserProcess> allProcesses;
  public static Lock newProcLock;

	public static void addProcess( UserProcess process ){
    newProcLock.acquire();
		allProcesses.put( process.processID, process);
		newProcLock.release();
	}
}
