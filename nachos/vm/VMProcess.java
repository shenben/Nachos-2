package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
		super.saveState();
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		super.restoreState();
	}

	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
    System.out.println("numPages for process = " + numPages);
    pageTable = new TranslationEntry[numPages];
		for( int i = 0 ; i < numPages ; i++ ) {
      pageTable[i] = new TranslationEntry(i, -1, false, false, false, false);
		}
    return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		super.unloadSections();
	}

	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    System.out.println("in ReadVM, vaddr = " + vaddr);
    int vpn = vaddr / pageSize;
    if (!pageTable[vpn].valid) {
      return handlePageFault(vaddr);
    } else {
      return super.readVirtualMemory(vaddr, data, offset, length);
    }
  }

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    System.out.println("in WriteVM, vaddr = " + vaddr);
    int vpn = vaddr / pageSize;
    if (!pageTable[vpn].valid) {
      return handlePageFault(vaddr);
    } else {
      return super.writeVirtualMemory(vaddr, data, offset, length);
    }
  }

  /**
   * Handler for page faults. 
   */
  private int handlePageFault(int faddr) {
    if (faddr < 0 || faddr >= (numPages * pageSize)) return -1;

    System.out.println("faddr: " + faddr);

    int numCoff = coff.getNumSections();
    int vpn = faddr / pageSize;
    int ppn = -1;

    System.out.println("pageTable[vpn].valid = " + pageTable[vpn].valid);

    // Get a physical page
    if (UserKernel.getNumFreePages() > 0) {
      // If free page in list
      System.out.println("Getting page from freePages");
      ppn = UserKernel.giveOnePage();
      VMKernel.invTable[ppn].vpn = vpn;
      VMKernel.invTable[ppn].ppn = processID;
    } else {
      // If need to evict an existing page
      /*System.out.println("Getting page from evicted page");
      for (int i = 0; i < VMKernel.numPhysPages; i++) { //TODO replace w Clock Alg
        //if (pinned) continue;
        if (VMKernel.invTable[i].dirty == true) {
          byte buf[] = new byte[pageSize];
          // fill buf[] TODO read from page?
          VMKernel.swapFile.write(buf, 0, pageSize);
        }
        ppn = i;
        VMKernel.invTable[ppn].vpn = vpn;
        VMKernel.invTable[ppn].ppn = processID;
      }*/
      System.out.println("Not enough pages!");
    }
    /*ppn = VMKernel.evictPage();
    VMKernel.invTable[ppn].vpn = vpn;
    VMKernel.invTable[ppn].ppn = processID;*/

    if (ppn == -1) {
      System.out.println("No available pages to swap out!");
      return -1; //TODO request must be delayed - CV??
    }

    // Invalidate PTE for process w this evicted page TODO
    int pid = VMKernel.invTable[ppn].ppn;

    System.out.println("vpn = " + vpn);
    System.out.println("ppn = " + ppn);
    System.out.println("processID = " + processID);

    // Load page into memory
    boolean coffSec = false;
    pageTable[vpn].ppn = ppn;
    // Loop through COFF Sections to determine if load from here
    for (int s = 0; s < numCoff; s++) {
      CoffSection section = coff.getSection(s);
      for (int i = 0; i < section.getLength(); i++) {
        if (vpn == (section.getFirstVPN() + i)) {
          coffSec = true;
          // Load from SWAP if dirty
          if (pageTable[vpn].dirty) {
            System.out.println("Loading from SWAP (code/data)");
            // swapFile.read();//read(byte buf[], int offset, int length)
            break;
          // Load from COFF if not dirty
          } else {
            System.out.println("Loading from COFF");
            section.loadPage(i, pageTable[vpn].ppn);
            if (section.isReadOnly()) pageTable[vpn].readOnly = true;
            break;
          }
        }
      }
    }

    // Either stack or args page
    if (coffSec == false) {
      // Load from SWAP if dirty
      if (pageTable[vpn].dirty) {
        System.out.println("Loading from SWAP (stack/args)");
        // swapFile.read();//read(byte buf[], int offset, int length)
      // Load as 0-filled if not dirty
      } else {
        System.out.println("Loading as 0-filled");
        /*byte[] buf = new byte[pageSize];
        for (int i = 0; i < pageSize; i++) {
          buf[i] = 0;
        }
        writeVirtualMemory(vpn * pageSize, buf, 0, pageSize);*/
        byte[] zeroFill = new byte[pageSize];
			  System.arraycopy( zeroFill, 0, Machine.processor().getMemory(), 
			      ppn * pageSize, pageSize );
      }
    }
    // Mark page as valid
    pageTable[vpn].valid = true;
    //pageTable[vpn].used = false;
    //pageTable[vpn].dirty = false;
    Machine.processor().setPageTable(pageTable);
    return 1;
  }

	/**
	 * Handle a user exception. Called by <tt>UserKernel.exceptionHandler()</tt>
	 * . The <i>cause</i> argument identifies which exception occurred; see the
	 * <tt>Processor.exceptionZZZ</tt> constants.
	 * 
	 * @param cause the user exception that occurred.
	 */
	public void handleException(int cause) {
		Processor processor = Machine.processor();

		switch (cause) {
    case Processor.exceptionPageFault:
      handlePageFault(processor.readRegister(Processor.regBadVAddr));
      break;
		default:
			super.handleException(cause);
			break;
		}
	}

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

  /** How to get VMProcess from PID. */
  private VMProcess getProcess(int pid) { //TODO
    return this;
  }

  /** Given. */
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
