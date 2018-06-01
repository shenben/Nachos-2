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
	 * Implement the readVirtualMemory method from UserProcess to throws
	 * page fault.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length){
    Lib.assertTrue( offset >= 0 && length >= 0 
		  && offset + length <= data.length);

    byte[] memory = Machine.processor().getMemory();

    // TODO return 0 or -1?
		if( vaddr < 0 || vaddr >= memory.length ) return -1;

    int bytesRead = 0;
    int vPageBegin = vaddr / pageSize;
		int pageLoc = vaddr % pageSize;

    if( pageTable[vPageBegin] == null ) return -1;

		// Check if the physcial page is allcoated
		if( !pageTable[vPageBegin].valid) {
		  handlePageFault( vaddr );
		}

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

		if( paddr < 0 || paddr >= memory.length ) return -1;

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within the page
		if( firstPageLeft >= length ){
      System.arraycopy( memory, paddr, data, offset, length);
			pageTable[vPageBegin].used = true;
			return length;
		}
    // When the bits ends beyond the first page
    else {
      // The first page part
			System.arraycopy( memory, paddr, data, offset, firstPageLeft );
			bytesRead += firstPageLeft;

			// Middle part
			int remainBytes = length - firstPageLeft;
			int readPages = vPageBegin + 1;
			offset += firstPageLeft;
			
			if( offset > length ) return bytesRead;
			while( remainBytes > pageSize ) {

        // If the page is not mapped to physical memory, handle it
				if( !pageTable[readPages].valid ) {
				  handlePageFault( readPages * pageSize );
				}

				pageTable[readPages].used = true;

				paddr = pageTable[readPages].ppn * pageSize;
				System.arraycopy( memory, paddr, data, offset, pageSize );

				bytesRead += pageSize;
				
				// Update the address and offset
				remainBytes -= pageSize;
				offset += pageSize;
				readPages++;
			}

			// The final part
			if( !pageTable[readPages].valid ){
			  handlePageFault( readPages * pageSize );
			}

			paddr = pageTable[readPages].ppn * pageSize;
			System.arraycopy( memory, paddr, data, offset, remainBytes );
			bytesRead += remainBytes;
			pageTable[readPages].used = true;
			remainBytes = 0;
			return bytesRead;
		} 
	}

	public int readVirtualMemory( int vaddr, byte[] data ) {
    return readVirtualMemory( vaddr, data, 0, data.length );
	}

	public String readVirtualMemoryString( int vaddr, int maxLength) {
    Lib.assertTrue( maxLength >= 0 );

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for( int length = 0 ; length < bytesRead ; length++ ) {
		  if( bytes[length] == 0)
			  return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * override writeVirtualMemory to handle page fault
	 */
	public int writeVirtualMemory(int vaddr, byte[]data, int offset, int length) {
    Lib.assertTrue(offset >= 0 && length >= 0 
		  && offset + length <= data.length );

		int bytesRead = 0;
    byte[] memory = Machine.processor().getMemory();

    // Break the length into pages
    int vPageBegin = vaddr / pageSize;
		int pageLoc = vaddr % pageSize;
    //if( vPageBegin >= numPages ) return 0;

    // Handle the page allocation
		if( !pageTable[vPageBegin].valid ){
		  handlePageFault( vaddr );
		}

		// Check the physical address makes sens
		if( pageTable[vPageBegin] == null ||
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
			if( /*pageRead >= numPages ||*/ offset > length ) return remainBytes;

			while( remainBytes > pageSize ) {
			  if( !pageTable[pageRead].valid ){
				  handlePageFault( pageRead * pageSize );
				}
				// Error
				if( pageTable[pageRead] == null || pageTable[pageRead].readOnly) 
				  return length - remainBytes;

				// Set the page's attribute
				pageTable[pageRead].used = true;
				pageTable[pageRead].dirty = true;	

				paddr = pageTable[pageRead].ppn * pageSize;
				System.arraycopy( data, offset, memory, paddr, pageSize );
				bytesRead += pageSize;
				// Update the address and offset
				remainBytes -= pageSize;
				offset += pageSize;
				pageRead++;
			}

			// The final part
			if( !pageTable[pageRead].valid ) {
				handlePageFault( pageRead * pageSize );
			}
			paddr = pageTable[pageRead].ppn * pageSize;
			System.arraycopy( data, offset, memory, paddr, remainBytes );
			pageTable[pageRead].used = true;
			pageTable[pageRead].dirty = true;

			bytesRead += remainBytes;
			remainBytes = 0;

			return bytesRead;
		}  
	}

	public int writeVirtualMemory( int vaddr, byte[] data ){
    return writeVirtualMemory( vaddr, data, 0, data.length );
	}
  
	/**
	 * Initializes page tables for this process so that the executable can be
	 * demand-paged.
	 * 
	 * @return <tt>true</tt> if successful.
	 */
	protected boolean loadSections() {
		//return super.loadSections();
		// When we run out of physical memory
/*System.err.println( "numpages is " + numPages + " and we have in total " + 
 UserKernel.getNumFreePages() );
		if( numPages > UserKernel.getNumFreePages() ) {
      coff.close();
			Lib.debug( dbgProcess, "\tinsufficient physical memory." );
			
			// Close all the files that opened
			for( int i = 0 ; i < maxOpenFiles ; i++ ) {
        if( fileTable[i] != null ) {
          fileTable[i].close();
					fileTable[i] = null;
				}
			}
			return false;
		}*/
    
		pageTable = new TranslationEntry[numPages];
		// For each page we need, we initialize it to invalid pages
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

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

	private static final char dbgVM = 'v';

	protected int handlePageFault(int faultAddr) {
	  int phyPage = VMKernel.giveOnePage();
		// When we don't have more pages left
		if( phyPage == -1 ) {
		  // TODO need to evict new pages
      return -1;
		}

    // Get the page number where fault happened
		int faultPage = faultAddr / pageSize;
    // Map the virtual page to the actual physical page
    pageTable[faultPage].ppn = phyPage;
		
		int s = 0;
		// Get the appropriate data into the physical page
    for( ; s < coff.getNumSections() ; s++ ) {
      CoffSection section = coff.getSection(s);

			for( int i = 0 ; i < section.getLength() ; i++ ) {
        int vpn = section.getFirstVPN() + i;
				// If we found the page we are faulting on
				if( faultPage == vpn ) {
          section.loadPage(i, pageTable[faultPage].ppn );
					if( section.isReadOnly() ) pageTable[faultPage].readOnly = true;
					break;
				}
			} // End of for section.getLength
		} // End of for coff.getNumSections
    
		// zero fill the page if we never got a page from segments
		if( s == coff.getNumSections() && pageTable[faultPage].ppn == -1 ) {
      int [] zeroFill = new int[pageSize];
			System.arraycopy( zeroFill, 0, Machine.processor().getMemory(), 
			                  phyPage * pageSize, pageSize );
		}

		pageTable[faultPage].valid = true;
		return 1;
	}
}
