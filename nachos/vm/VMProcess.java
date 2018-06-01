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
    int vPage = vaddr / pageSize;
    if (!pageTable[vPage].valid) {
      return handlePageFault(vaddr);
    } else {
      return super.readVirtualMemory(vaddr, data, offset, length);
    }
  }

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
    int vPage = vaddr / pageSize;
    if (!pageTable[vPage].valid) {
      return handlePageFault(vaddr);
    } else {
      return super.writeVirtualMemory(vaddr, data, offset, length);
    }
  }

  /**
   * Handler for page faults. 
   */
  private int handlePageFault(int faddr) {
    // code, data, or stack page?
    int numCoff = coff.getNumSections();
    int sec = faddr / pageSize;
    if (sec < numCoff) {
      // Load from coff or swap if not RO TODO how to deal w swap files?
      // use dirty bits to check if non-RO gotten from swap?
      CoffSection section = coff.getSection(sec);
      section.loadPage(section.getFirstVPN() + sec, pageTable[sec].ppn); //TODO why need to loop?
      if (section.isReadOnly()) pageTable[sec].readOnly = true;
    } else {
      // 0-fill
      byte[] buf = new byte[pageSize];
      for (int i = 0; i < pageSize; i++) {
        buf[i] = 0;
      }
      writeVirtualMemory(faddr, buf, 0, pageSize);
    }
    // Mark page as valid
    pageTable[sec].valid = true;
    return -1;
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

  /** Given. */
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
