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

  /**
   * readVirtualMemory implementations from UserProcess to 
   * accomodate local pageTable (and other vars).
   */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

    int vPageBegin = vaddr / pageSize;
    if (!pageTable[vPageBegin].valid) return handlePageFault(vaddr);

		byte[] memory = Machine.processor().getMemory();

		if ( vaddr < 0 || vaddr >= memory.length) return -1;

    int bytesRead = 0;
    // Break the length into pages
		int pageLoc = vaddr % pageSize;

		// Check the physical address makes sens
		if( vPageBegin >= numPages || 
		    pageTable[vPageBegin] == null || !pageTable[vPageBegin].valid )
		 return -1;

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

    if( paddr < 0 || paddr >= memory.length ) return -1;

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within this page
		if( firstPageLeft >= length ) {
      System.arraycopy(memory, paddr, data, offset, length);
			pageTable[vPageBegin].used = true;
			return length;
		}
		// When the bits ends beyong this page
		else {
		  // The first part
      System.arraycopy(memory, paddr, data, offset, firstPageLeft );
      bytesRead += firstPageLeft;
      
			// Middle part
			int remainBytes = length - firstPageLeft;
			int readPages = vPageBegin + 1;
			offset += (firstPageLeft );
			if( readPages >= numPages || offset > length ) return remainBytes;
			while( remainBytes > pageSize ) {
			  // Error
			  if( pageTable[readPages] == null || !pageTable[readPages].valid ) 
				  return (length - remainBytes);

				// Set the page's attributes
				pageTable[readPages].used = true;

			  paddr = pageTable[readPages].ppn * pageSize;
        System.arraycopy( memory, paddr, data, offset, pageSize );
				bytesRead += pageSize;
				// Update the address and offset
				remainBytes -= pageSize;
				offset += (pageSize);
				readPages++;
			}
			
			// The final part
			paddr = pageTable[readPages].ppn * pageSize;
			System.arraycopy( memory, paddr, data, offset, remainBytes );
			bytesRead += remainBytes;
			pageTable[readPages].used = true;
			remainBytes = 0;
			return bytesRead;
		}
	}

  /**
   * writeVirtualMemory implementations from UserProcess to 
   * accomodate local pageTable (and other vars).
   */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
  	Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

    int vPageBegin = vaddr / pageSize;
    if (!pageTable[vPageBegin].valid) return handlePageFault(vaddr);

    int bytesRead = 0;
		byte[] memory = Machine.processor().getMemory();

    // Break the length into pages
		int pageLoc = vaddr % pageSize;
    if( vPageBegin >= numPages ) return 0;

		// Check the physical address makes sens
		if( pageTable[vPageBegin] == null || !pageTable[vPageBegin].valid ||
				pageTable[vPageBegin].readOnly ) 
		  return -1;

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

    //System.out.println( "Now the paddr is at " + paddr );
		if( paddr < 0 || paddr >= memory.length ) return 0;

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within this page
		if( firstPageLeft >= length ) {
      System.arraycopy(data, offset, memory, paddr, length);
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
			return length;
		}
		// When the bits ends beyong this page
		else {
		  // The first part
      System.arraycopy(data, offset, memory, paddr, firstPageLeft );
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
			bytesRead += firstPageLeft;

			// Middle part
			int remainBytes = length - firstPageLeft;
			offset += (firstPageLeft);
			int pageRead = vPageBegin + 1;
			if( pageRead >= numPages || offset > length ) return remainBytes;
			while( remainBytes > pageSize ) {
				// Error
				if( pageTable[pageRead] == null || pageTable[pageRead].readOnly || 
				    !pageTable[pageRead].valid ) 
				  return length - remainBytes;

				// Set the page's attribute
				pageTable[pageRead].used = true;
				pageTable[pageRead].dirty = true;
				
			  paddr = pageTable[pageRead].ppn * pageSize;
        System.arraycopy( data, offset, memory, paddr, pageSize );
				bytesRead += pageSize;
				// Update the address and offset
				remainBytes -= pageSize;
				offset += (pageSize);
				pageRead++;
			}
			
			// The final part
			paddr = pageTable[pageRead].ppn * pageSize;
      //System.out.println( "after the first two parts the paddr is " + paddr );
			System.arraycopy( data, offset, memory, paddr, remainBytes );
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].used = false;
			bytesRead += remainBytes;
			remainBytes = 0;
			return bytesRead;
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
        byte[] zeroFill = new byte[pageSize];
			  System.arraycopy( zeroFill, 0, Machine.processor().getMemory(), 
			      ppn * pageSize, pageSize );
      }
    }
    // Mark page as valid
    pageTable[vpn].valid = true;
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
