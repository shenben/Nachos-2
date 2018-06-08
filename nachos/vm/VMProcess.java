package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Arrays;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
	/**
	 * Allocate a new process.
	 */
	public VMProcess() {
		super();
		// Initialize the inverted page table if it has not been initialized yet
		if( invertedPageTable == null ) {
      invertedPageTable =
			   new TranslationEntry[Machine.processor().getNumPhysPages()];
			
			// Get all the phy pages in there
			for( int i = 0 ; i < Machine.processor().getNumPhysPages(); i++ ) {
        invertedPageTable[i] = new TranslationEntry(i, -1, false, false, false,
				                                             false );
			}
			clockHand = 0;
		}
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

    if( vPageBegin >= numPages || pageTable[vPageBegin] == null ) return -1;

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
			invertedPageTable[ pageTable[vPageBegin].ppn ].used = true;
System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );

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
				invertedPageTable[ pageTable[readPages].ppn ].used = true;
System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );


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
			invertedPageTable[ pageTable[readPages].ppn ].used = true;
System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );


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

      // Set all the flags
			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
      int p = pageTable[vPageBegin].ppn;
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );


			return length;
		}
		// When the bits ends beyong this page
		else {
		  // The first part
      System.arraycopy(data, offset, memory, paddr, firstPageLeft );

			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
			int p = pageTable[vPageBegin].ppn;
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
	System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );

		
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
        p = pageTable[pageRead].ppn;
			  invertedPageTable[p].used = true;
			  invertedPageTable[p].dirty = true;

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
      p = pageTable[pageRead].ppn;
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
System.out.println("Setting " + vPageBegin + " virtual to used and " + 
pageTable[vPageBegin].ppn + " to used too" );


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
      pageTable[i] = new TranslationEntry(-1, -1, false, false, false, false);
		}
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
		//super.unloadSections();
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

	private int clockHand;
	public final String swapFileName = "swapeanut";

	// Inverted page table for relating physical pages with processes
	protected static TranslationEntry[] invertedPageTable;

	protected int handlePageFault(int faultAddr) {

/*****************************/
/*System.out.println("page table");
for( int i = 0 ; i < numPages ; i++ ) {
TranslationEntry t = pageTable[i];
System.out.println( t.vpn + " " + t.ppn + " " + t.valid + " " + t.dirty + " "
+ t.used );
}
System.out.println( "Inverted page table" );
for( int i = 0 ; i < invertedPageTable.length ; i++ ) {
TranslationEntry t = invertedPageTable[i];
System.out.println( t.vpn + " " + t.ppn + " " + t.valid + " " + t.dirty + " "
+ t.used );

}*/
		// When we don't have more pages left
		int phyPage;
		if( VMKernel.getNumFreePages() <= 0 ) {
		  // TODO need to evict pages and get the page back in
			phyPage = swapOut();
		}
	  else phyPage = VMKernel.giveOnePage();

    // Get the page number where fault happened
		int faultPage = faultAddr / pageSize;
//System.out.println( "We are faulting on page " + faultPage + " that is mapped to "
  //                 + phyPage + " physical page." );
    // Map the virtual page to the actual physical page
    pageTable[faultPage].ppn = phyPage;
		// Map the prcess to the actual physcial page
		invertedPageTable[phyPage].ppn = this.processID;
		invertedPageTable[phyPage].vpn = faultPage;

		// when the page has not been loaded before
		if( !pageTable[faultPage].used || pageTable[faultPage].vpn < 0) {
		  int s = 0;
			boolean loaded = false;
		  // Get the appropriate data into the physical page
      for( ; s < coff.getNumSections() ; s++ ) {
        CoffSection section = coff.getSection(s);

			  for( int i = 0 ; i < section.getLength() ; i++ ) {
          int vpn = section.getFirstVPN() + i;
				  // If we found the page we are faulting on
				  if( faultPage == vpn ) {
            section.loadPage(i, pageTable[faultPage].ppn );
					  if( section.isReadOnly() ) pageTable[faultPage].readOnly = true;
						else pageTable[faultPage].readOnly = false;

						loaded = true;
					  break;
				  }
			  } // End of for section.getLength
		  } // End of for coff.getNumSections
    
		  // zero fill the page if we never got a page from segments
		 // if( s == coff.getNumSections() && pageTable[faultPage].ppn == -1 ) {
		  if( !loaded ) {
        byte [] zeroFill = new byte[pageSize];
				for( int i = 0 ; i < pageSize ; i++ ) {
          zeroFill[i] = 0;
				}
			  System.arraycopy( zeroFill, 0, Machine.processor().getMemory(), 
			                  phyPage * pageSize, pageSize );
				pageTable[faultPage].readOnly = false;
		  }  

		  pageTable[faultPage].valid = true;
			//pageTable[faultPage].vpn = faultPage;
		}
		else { // WHen the page has been swaped out before
		  // We read it back from the swap file
			swapIn( this, faultPage );

			// Wipe the plate clean
			pageTable[faultPage].valid = true;
			pageTable[faultPage].dirty = false;
			pageTable[faultPage].used = true;
			//pageTable[faultPage].readOnly = false;
		      
		}
    //invertedPageTable[phyPage].valid = true;
		invertedPageTable[phyPage].dirty = false;	

		return 1;
	}

  // Choose a page, write its content to swap file, and mark it clean to use
  public int swapOut(){
		while( true ){
//System.out.println("In swapping " + clockHand );
		  clockHand++;
		  // Advance the clock hand until we find one that has not been used
			if( clockHand >= Machine.processor().getNumPhysPages() ) clockHand = 0;

			// If we see a page that has been used
      if( invertedPageTable[clockHand].used )
			  invertedPageTable[clockHand].used = false;
			else { // Otherwise we have chosen our victim
//System.out.println("We are swapping out physical page: " + clockHand ); 
			  // Get the process that's using the physical page
			  VMProcess process = (VMProcess)VMKernel.allProcesses.get(
					                                    invertedPageTable[clockHand].ppn);
				// Updite its page table
			  int visPageNum = invertedPageTable[clockHand].vpn;	
        //for( int i = 0; i < process.pageTable.length ; i++ ) {
          //if( process.pageTable[i].ppn == clockHand ) {
            process.pageTable[visPageNum].valid = false;
//System.out.println( "setting page " + visPageNum + " to " + pageTable[visPageNum].valid );
            
/*for( int j = 0 ; j < pageTable.length ; j++ ) {
TranslationEntry temp = pageTable[j];
System.out.println( temp.vpn + " " + temp.ppn + " " + temp.valid + " " + 
temp.readOnly + " " + temp.used + " " + temp.dirty);
}*/
						//visPageNum = i;
					//	break;
					//}
				//}
        
				if( process.pageTable[visPageNum].dirty || 
				    !process.pageTable[visPageNum].readOnly ){
				  // We need to write the file out to swap
          OpenFile swapFile = ThreadedKernel.fileSystem.open( swapFileName, true);
					
					// See if the virtual page has a swap pos already
					int filePost = -1;
					if( process.pageTable[visPageNum].vpn < 0 ) 
					  filePost = VMKernel.giveSwapPage();
					else filePost = process.pageTable[visPageNum].vpn;
//System.out.println( "We are writing to file at " + filePost + " to write vpage " + visPageNum);
					int bitsWrote = swapFile.write( filePost * pageSize, 
					                Machine.processor().getMemory(),
					                clockHand*pageSize, pageSize );

//System.out.println( "We write " + bitsWrote + " bits" );
					// Record the spn in the virtual page number
					process.pageTable[visPageNum].vpn = filePost;
					
					swapFile.close();
				}

        invertedPageTable[clockHand].dirty = false;
				invertedPageTable[clockHand].used = true;
/*for( int j = 0 ; j < invertedPageTable.length ; j++ ) {
TranslationEntry temp = invertedPageTable[j];
System.out.println( "iverted page talbe " + temp.vpn + " " + temp.ppn + " " + temp.valid + " " + 
temp.readOnly + " " + temp.used + " " + temp.dirty);
}*/

//System.out.println( "reached return in swap out." );	
				return clockHand;
			}// end of else
		}
	}

	public int swapIn( VMProcess process, int faultPage ) {
    OpenFile swapFile = ThreadedKernel.fileSystem.open( swapFileName, false );
		int filePost = process.pageTable[faultPage].vpn; // The spn
//System.out.println( "We need file at position " + filePost + " to get vpage " + faultPage);
		// REad from the file and load it into the page
		swapFile.read( filePost*pageSize,
		               Machine.processor().getMemory(),
									 process.pageTable[faultPage].ppn * pageSize, pageSize );
		swapFile.close();
		return 1;
	}
}
