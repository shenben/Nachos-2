package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.util.*;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
    expBytes = new HashMap<Integer, byte[]>();
    lock = new Lock();
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
    System.out.println("!!! NEW PROCESS !!! w PID = " + processID);
    lock.acquire();
    VMKernel.addProcess(processID, this);
    System.out.println("numproc = " + VMKernel.processMap.size());
    pageTable = new TranslationEntry[numPages];
		for( int i = 0 ; i < numPages ; i++ ) {
      pageTable[i] = new TranslationEntry(-1, -1, false, false, false, false);
		}
    lock.release();
    return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
    if (pageTable == null) return; 
    lock.acquire();
    for( int i = 0 ; i < numPages ; i++ ) {
		  if( pageTable[i] != null ) {
        UserKernel.receiveOnePage( pageTable[i].ppn );
				pageTable[i] = null;
      }
		}
    VMKernel.removeProcess(processID);
    lock.release();
		return;
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

    // TODO pin

    int vPageBegin = vaddr / pageSize;
    if (!pageTable[vPageBegin].valid) handlePageFault(vaddr);
    System.out.println("*******   Reading from VP " + vPageBegin);

    lock.acquire();

		byte[] memory = Machine.processor().getMemory();

		if ( vaddr < 0 ) {//|| vaddr >= memory.length) {
      lock.release();
      System.out.println("vaddr ==== " + vaddr);
      System.out.println("0");
      return -1;
    }

    int bytesRead = 0;
    // Break the length into pages
		int pageLoc = vaddr % pageSize;

		// Check the physical address makes sens
		if( vPageBegin >= numPages || 
		    pageTable[vPageBegin] == null || !pageTable[vPageBegin].valid ) {
     lock.release();
      System.out.println("1");
		 return -1;
    }

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

    if( paddr < 0 || paddr >= memory.length ) {
      lock.release();
      System.out.println("2");
      return -1;
    }

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within this page
		if( firstPageLeft >= length ) {
      System.arraycopy(memory, paddr, data, offset, length);
			pageTable[vPageBegin].used = true;
      // Update InvTable
      VMKernel.invTable[pPageBegin].used = true;
      lock.release();
      System.out.println("3");
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
			if( readPages >= numPages || offset > length ) {
        lock.release();
      System.out.println("4");
        return remainBytes;
      }
			while( remainBytes > pageSize ) {
			  // Error
			  if( pageTable[readPages] == null || !pageTable[readPages].valid ) {
          lock.release();
      System.out.println("5");
				  return (length - remainBytes);
        }

				// Set the page's attributes
				pageTable[readPages].used = true;
        // Update InvTable
        int pPageRead = pageTable[readPages].ppn;
        VMKernel.invTable[pPageRead].used = true;

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
      // Update InvTable
      int pPageRead = pageTable[readPages].ppn;
      VMKernel.invTable[pPageRead].used = true;
			remainBytes = 0;
      lock.release();
      System.out.println("6");
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

    // TODO pin

    int vPageBegin = vaddr / pageSize;
    if (!pageTable[vPageBegin].valid) handlePageFault(vaddr);

    System.out.println("*******   Writing to VP " + vPageBegin);
    lock.acquire();

    int bytesRead = 0;
		byte[] memory = Machine.processor().getMemory();

    // Break the length into pages
		int pageLoc = vaddr % pageSize;
    if( vPageBegin >= numPages ) {
      lock.release();
      System.out.println("invalid VA");
      return 0;
    }

		// Check the physical address makes sens
		if( pageTable[vPageBegin] == null || !pageTable[vPageBegin].valid ||
				pageTable[vPageBegin].readOnly ) {
      lock.release();
      System.out.println("bad PA");
		  return -1;
    }

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

    //System.out.println( "Now the paddr is at " + paddr );
		if( paddr < 0 || paddr >= memory.length ) {
      lock.release();
      System.out.println("invalid PA");
      return 0;
    }

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within this page
		if( firstPageLeft >= length ) {
      System.arraycopy(data, offset, memory, paddr, length);
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
      // Update InvTable
      VMKernel.invTable[pPageBegin].used = true;
      VMKernel.invTable[pPageBegin].dirty = true;
      lock.release();
      System.out.println("ONe page");
			return length;
		}
		// When the bits ends beyong this page
		else {
		  // The first part
      System.arraycopy(data, offset, memory, paddr, firstPageLeft );
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
      // Update InvTable
      VMKernel.invTable[pPageBegin].used = true;
      VMKernel.invTable[pPageBegin].dirty = true;
			bytesRead += firstPageLeft;

			// Middle part
			int remainBytes = length - firstPageLeft;
			offset += (firstPageLeft);
			int pageRead = vPageBegin + 1;
			if( pageRead >= numPages || offset > length ) {
        lock.release();
        System.out.println("MIDDLE");
        return remainBytes;
      }
			while( remainBytes > pageSize ) {
				// Error
				if( pageTable[pageRead] == null || pageTable[pageRead].readOnly || 
				    !pageTable[pageRead].valid ) {
          lock.release();
          System.out.println("error part");
				  return length - remainBytes;
        }

				// Set the page's attribute
				pageTable[pageRead].used = true;
				pageTable[pageRead].dirty = true;
        // Update InvTable
        int pPageRead = pageTable[pageRead].ppn;
        VMKernel.invTable[pPageRead].used = true;
        VMKernel.invTable[pPageRead].dirty = true;
				
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
			pageTable[pageRead].used = true;
			bytesRead += remainBytes;
			remainBytes = 0;
      lock.release();
      System.out.println("END OF WVM");
			return bytesRead;
		}
	}

  /**
   * Handler for page faults. 
   */
  private int handlePageFault(int faddr) {
    if (faddr < 0 || faddr >= (numPages * pageSize)) return -1;

    System.out.println("ENTERING HANDLER ***************");

    lock.acquire();

    System.out.println("faddr: " + faddr);

    int numCoff = coff.getNumSections();
    int vpn = faddr / pageSize;
    int ppn = -1;

    // Get a physical page
    ppn = VMKernel.evictPage();

    if (ppn == -1) {
      System.out.println("Swapping out failed");
      return -1; 
    }

    System.out.println("~~~~~~ PT for Process " + processID + " BEFORE ~~~~~~");
    System.out.println("pT["+vpn+"].spn                = " + pageTable[vpn].vpn);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].ppn                = " + pageTable[vpn].ppn);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].readOnly           = " + pageTable[vpn].readOnly);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].valid              = " + pageTable[vpn].valid);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].dirty              = " + pageTable[vpn].dirty);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].used               = " + pageTable[vpn].used);
    System.out.println("-----------");

    VMKernel.trackPhysPage(ppn);
    System.out.println("TRACKING PAGE " + ppn);

    // Load page into memory
    boolean loaded = false;
    pageTable[vpn].ppn = ppn;
    // Loop through COFF Sections to determine if load from here
    for (int s = 0; s < numCoff; s++) {
      CoffSection section = coff.getSection(s);
      for (int i = 0; i < section.getLength(); i++) {
        if (vpn == (section.getFirstVPN() + i)) {
          System.out.println("section NAME = " + section.getName());
          loaded = true;
          // Load from SWAP if dirty
          if ((!pageTable[vpn].readOnly) && pageTable[vpn].dirty) {
            System.out.println("Loading from SWAP (code/data)");
            int spn = pageTable[vpn].vpn;
            if (spn == -1) {
              System.out.println("Trying to read from swap area that this VP does not map to");
              return -1;
            }
            VMKernel.swapFile.read(spn * pageSize, Machine.processor().getMemory(), 
                ppn * pageSize, pageSize);
            byte[] act = new byte[pageSize];
            for (int j = 0; j < pageSize; j++) {
              act[j] = Machine.processor().getMemory()[(ppn * pageSize) + j];
            }
            byte[] exp = expBytes.get(vpn);
            if (exp == null) {
              System.out.println("trying to read in wrong context, vpn = " + vpn);
              return -1;
            }
            for (int j = 0; j < pageSize; j++) {
              if (act[j] != exp[j]) {
                System.out.println("act["+j+"] = " + act[j]);
                System.out.println("exp["+j+"] = " + exp[j]);
              }
            }
            break;
          // Load from COFF if not dirty
          } else {
            System.out.println("Loading from COFF");
            pageTable[vpn].dirty = true;
            if (section.isReadOnly()) {
              System.out.println("Setting to READONLY");
              pageTable[vpn].readOnly = true;
            }
            byte[] b = Machine.processor().getMemory();
            /*System.out.println("before loading ...");
            for (int k = 0; k < pageSize; k++) {
              System.out.print("" + b[(97* pageSize) + k] + ", ");
            }*/
            section.loadPage(i, pageTable[vpn].ppn);
            /*System.out.println("after loading ...");
            for (int k = 0; k < pageSize; k++) {
              System.out.print("" + b[(97* pageSize) + k] + ", ");
            }*/
            break;
          }
        }
      }
    }

    // Either stack or args page
    if (loaded == false) {
      // Load from SWAP if dirty
      if ((!pageTable[vpn].readOnly) && pageTable[vpn].dirty) {
        System.out.println("Loading from SWAP (stack/args)");
        int spn = pageTable[vpn].vpn;
        if (spn == -1) {
          System.out.println("Trying to read from swap area that this VP does not map to");
          return -1;
        }
        VMKernel.swapFile.read(spn * pageSize, Machine.processor().getMemory(), 
            ppn * pageSize, pageSize);
        byte[] act = new byte[pageSize];
        for (int j = 0; j < pageSize; j++) {
          act[j] = Machine.processor().getMemory()[(ppn * pageSize) + j];
        }
        byte[] exp = expBytes.get(vpn);
        if (exp == null) {
          System.out.println("trying to read in wrong context");
          return -1;
        }
        for (int j = 0; j < pageSize; j++) {
          if (act[j] != exp[j]) {
            System.out.println("act["+j+"] = " + act[j]);
            System.out.println("exp["+j+"] = " + exp[j]);
          }
        }
        System.out.println("Swap load succeeded");
      // Load as 0-filled if not dirty
      } else {
        System.out.println("Loading as 0-filled");
        pageTable[vpn].dirty = true;
        byte[] zeroFill = new byte[pageSize];
			  System.arraycopy( zeroFill, 0, Machine.processor().getMemory(), 
			      ppn * pageSize, pageSize );
      }
    }

    //VMKernel.trackPhysPage(ppn);
    //System.out.println("TRACKING PAGE " + ppn);

    // Mark page as valid
    pageTable[vpn].valid = true;
    pageTable[vpn].dirty = true;
    pageTable[vpn].used = false;
    Machine.processor().setPageTable(pageTable);

    // Update ITE for this physical page
    VMKernel.invTable[ppn].vpn = vpn;
    VMKernel.invTable[ppn].ppn = processID;
    VMKernel.invTable[ppn].valid = true;
    VMKernel.invTable[ppn].readOnly = pageTable[vpn].readOnly;
    VMKernel.invTable[ppn].dirty = true;
    VMKernel.invTable[ppn].used = false;
    System.out.println("~~~~~~ PT for Process " + processID + " AFTER ~~~~~~");
    System.out.println("pT["+vpn+"].spn                = " + pageTable[vpn].vpn);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].ppn                = " + pageTable[vpn].ppn);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].readOnly           = " + pageTable[vpn].readOnly);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].valid              = " + pageTable[vpn].valid);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].dirty              = " + pageTable[vpn].dirty);
    System.out.println("-----------");
    System.out.println("pT["+vpn+"].used               = " + pageTable[vpn].used);
    System.out.println("-----------");

    lock.release();

    System.out.println("EXITING HANDLER ***************");

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

  public TranslationEntry[] getPageTable() {
    return pageTable;
  }

  public void setPageTable(TranslationEntry[] pt) {
    pageTable = pt;
  }

  public static HashMap<Integer, byte[]> expBytes;

  private Lock lock;

  /** Given. */
	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';
}
