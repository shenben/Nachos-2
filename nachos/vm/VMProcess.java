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
        invertedPageTable[i] = new TranslationEntry(-1, -1, false, false, false,
				                                             false );
			}
			clockHand = 0;
			//swapFile = ThreadedKernel.fileSystem.open( swapFileName, true );

			ivtLock = new Lock();
			//changeIvtTable = new Condition( ivtLock );
			pinLock = new Lock();
			allPinned = new Condition( pinLock );
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
//System.out.println( "We are reading " + length + " bytes." );
    Lib.assertTrue( offset >= 0 && length >= 0 
		  && offset + length <= data.length);

    byte[] memory = Machine.processor().getMemory();

    // TODO return 0 or -1?
		if( vaddr < 0 || vaddr >= numPages*pageSize ) {
		  System.out.println( "Virtual Address invalid*************************");
		  return -1;
		}

    int bytesRead = 0;
    int vPageBegin = vaddr / pageSize;
		int pageLoc = vaddr % pageSize;

    if( vPageBegin >= numPages || pageTable[vPageBegin] == null ) {
		  System.out.println( "Virtual page invalid******************************" );
		  return -1;
		}

   //this.PTLock.acquire();
//System.out.println( "Process " + processID + " needs virtual page " + vPageBegin );

 //   pinLock.acquire();
		this.pinPage(vPageBegin);
//		pinLock.release();
		// Check if the physcial page is allcoated
		//this.PTLock.acquire();
		if( !pageTable[vPageBegin].valid) {
//System.out.println( "Wnated page " + vPageBegin );
		  handlePageFault( vaddr );
		} 
		//this.PTLock.release();

   // pinLock.acquire();
		//this.pinPage(vPageBegin);
/*System.out.println( "Pinned page " + vPageBegin );
System.out.println( "Inverted page table" );
for( int i = 0 ; i < invertedPageTable.length ; i++ ) {
TranslationEntry t = invertedPageTable[i];
System.out.println( t.vpn + " " + t.ppn + " " + t.valid + " " + t.dirty + " "
+ t.used + " " +  t.readOnly );
}*/


	//	this.pinPage( vPageBegin );
	  toPin = false;

		int pPageBegin = pageTable[vPageBegin].ppn;
		int paddr = pPageBegin * pageSize + pageLoc;

		if( paddr < 0 || paddr >= numPages * pageSize ) {
//		  pinLock.acquire();
			this.unPin( vPageBegin );
//			pinLock.release();
//      System.out.println( "Physical memory address is not valid ************");
		  return -1;
		}

		// First part of the bytes
		int firstPageLeft = pageSize - pageLoc;
		// When the bits ends just within the page
		if( firstPageLeft >= length ){
      System.arraycopy( memory, paddr, data, offset, length);

			pageTable[vPageBegin].used = true;

			ivtLock.acquire();
			invertedPageTable[ pageTable[vPageBegin].ppn ].used = true;
			ivtLock.release();

//			pinLock.acquire();
			this.unPin( vPageBegin );
//			pinLock.release();

//      System.out.println( "Process " + this.processID + "Read the string " + new String( data ));
			return length;
		}
    // When the bits ends beyond the first page
    else {
      // The first page part
			System.arraycopy( memory, paddr, data, offset, firstPageLeft );

		//	pinLock.acquire();
			this.unPin( vPageBegin );
		//	pinLock.release();

			bytesRead += firstPageLeft;

			// Middle part
			int remainBytes = length - firstPageLeft;
			int readPages = vPageBegin + 1;
			offset += firstPageLeft;
			
			if( offset > length ) {
			  System.out.println( "Only read " + bytesRead + " in reading memory!" );
			  return bytesRead;
			}

			while( remainBytes > pageSize ) {

     //   pinLock.acquire();
				this.pinPage( readPages );
		//		pinLock.release();
        // If the page is not mapped to physical memory, handle it
				if( !pageTable[readPages].valid ) {
				  handlePageFault( readPages * pageSize );
				}
				toPin = false;

        /*pinLock.acquire();
				this.pinPage( readPages );
				pinLock.release();*/

				pageTable[readPages].used = true;

				ivtLock.acquire();
				invertedPageTable[ pageTable[readPages].ppn ].used = true;
				ivtLock.release();
//System.out.println("Setting " + vPageBegin + " virtual to used and " + 
//pageTable[vPageBegin].ppn + " to used too" );


				paddr = pageTable[readPages].ppn * pageSize;
				System.arraycopy( memory, paddr, data, offset, pageSize );

				//pinLock.acquire();
				this.unPin( readPages );
				//pinLock.release();

				bytesRead += pageSize;
				
				// Update the address and offset
				remainBytes -= pageSize;
				offset += pageSize;
				readPages++;
			}

			//pinLock.acquire();
			this.pinPage( readPages );
			//pinLock.release();
			// The final part
			if( !pageTable[readPages].valid ){
			  handlePageFault( readPages * pageSize );
			}
			toPin = false;

			/*pinLock.acquire();
			this.pinPage( readPages );
			pinLock.release();*/

			paddr = pageTable[readPages].ppn * pageSize;
			System.arraycopy( memory, paddr, data, offset, remainBytes );
			bytesRead += remainBytes;

			pageTable[readPages].used = true;

		//	pinLock.acquire();
			this.unPin( readPages );
		//	pinLock.release();

			ivtLock.acquire();
			invertedPageTable[ pageTable[readPages].ppn ].used = true;
			ivtLock.release();
//System.out.println("Setting " + vPageBegin + " virtual to used and " + 
//pageTable[vPageBegin].ppn + " to used too" );


			remainBytes = 0;
//System.out.println( "Returned**************************8 " + remainBytes );
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
		  && offset + length <= data.length  && vaddr >= 0);

		int bytesRead = 0;
    byte[] memory = Machine.processor().getMemory();

    // Break the length into pages
    int vPageBegin = vaddr / pageSize;
		int pageLoc = vaddr % pageSize;
    //if( vPageBegin >= numPages ) return 0;

	//	pinLock.acquire();
		this.pinPage( vPageBegin );
	//	pinLock.release();

    // Handle the page allocation
		if( !pageTable[vPageBegin].valid ){
		  handlePageFault( vaddr );
		}
    toPin = false;

		/*pinLock.acquire();
		this.pinPage( vPageBegin );
		pinLock.release();*/

		// Check the physical address makes sens
		if( pageTable[vPageBegin] == null ||
				pageTable[vPageBegin].readOnly ) {
	//	  pinLock.acquire();
			this.unPin( vPageBegin );
		//	pinLock.release();
//System.out.println( "returned here" );
			return -1;
		}

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

		//	pinLock.acquire();
			this.unPin(vPageBegin);
		//	pinLock.release();

			ivtLock.acquire();
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
			ivtLock.release();

//System.out.println("Setting " + vPageBegin + " virtual to used and " + 
//pageTable[vPageBegin].ppn + " to used too" );


			return length;
		}
		// When the bits ends beyong this page
		else {
		  // The first part
      System.arraycopy(data, offset, memory, paddr, firstPageLeft );

			pageTable[vPageBegin].used = true;
			pageTable[vPageBegin].dirty = true;
			int p = pageTable[vPageBegin].ppn;

	//		pinLock.acquire();
			this.unPin( vPageBegin );
		//	pinLock.release();

			ivtLock.acquire();
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
			ivtLock.release();

//	System.out.println("Setting " + vPageBegin + " virtual to used and " + 
//pageTable[vPageBegin].ppn + " to used too" );

		
			bytesRead += firstPageLeft;

			// Middle part
			int remainBytes = length - firstPageLeft;
			offset += (firstPageLeft);
			int pageRead = vPageBegin + 1;
			if( /*pageRead >= numPages ||*/ offset > length ) return remainBytes;

			while( remainBytes > pageSize ) {

		//	  pinLock.acquire();
		    this.pinPage( pageRead );
		//    pinLock.release();

			  if( !pageTable[pageRead].valid ){
				  handlePageFault( pageRead * pageSize );
				}
				toPin = false;

				/*pinLock.acquire();
				this.pinPage( pageRead );
				pinLock.release();*/
				// Error
				if( pageTable[pageRead] == null || pageTable[pageRead].readOnly) {
			//	  pinLock.acquire();
					this.unPin(pageRead );
			//		pinLock.release();
		      return length - remainBytes;
				}

				// Set the page's attribute
				pageTable[pageRead].used = true;
				pageTable[pageRead].dirty = true;	
        p = pageTable[pageRead].ppn;
        
				ivtLock.acquire();
			  invertedPageTable[p].used = true;
			  invertedPageTable[p].dirty = true;
				ivtLock.release();

				paddr = pageTable[pageRead].ppn * pageSize;
				System.arraycopy( data, offset, memory, paddr, pageSize );

	//			pinLock.acquire();
				this.unPin( pageRead );
	//			pinLock.release();

				bytesRead += pageSize;
				// Update the address and offset
				remainBytes -= pageSize;
				offset += pageSize;
				pageRead++;
			}

			// The final part
			
  //		pinLock.acquire();
	  	this.pinPage( pageRead );
	 // 	pinLock.release();

			if( !pageTable[pageRead].valid ) {
				handlePageFault( pageRead * pageSize );
			}
			toPin = false;

		/*	pinLock.acquire();
			this.pinPage( pageRead );
			pinLock.release();*/

			paddr = pageTable[pageRead].ppn * pageSize;
			System.arraycopy( data, offset, memory, paddr, remainBytes );

			pageTable[pageRead].used = true;
			pageTable[pageRead].dirty = true;
      p = pageTable[pageRead].ppn;

		//	pinLock.acquire();
			this.unPin( pageRead );
		//	pinLock.release();

			ivtLock.acquire();
			invertedPageTable[p].used = true;
			invertedPageTable[p].dirty = true;
			ivtLock.release();
//System.out.println("Setting " + vPageBegin + " virtual to used and " + 
//pageTable[vPageBegin].ppn + " to used too" );


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
		PTLock = new Lock();
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
	//public final String swapFileName = "swapeanut";
  //OpenFile swapFile;

  public static Lock ivtLock;
	//public static Condition changeIveTable;
	public static Lock pinLock;
	public static Condition allPinned;
	public boolean toPin = false;

	public static int pinPageCount = 0;
	//public Lock PTLock;

	// Inverted page table for relating physical pages with processes
	protected static TranslationEntry[] invertedPageTable;

	protected int handlePageFault(int faultAddr) {

//System.out.println( "Faulting on process " + this.processID + " "  + faultAddr/pageSize );
/*****************************/
/*System.out.println("page tablei for process " + this.processID);
for( int i = 0 ; i < numPages ; i++ ) {
TranslationEntry t = pageTable[i];
System.out.println( t.vpn + " " + t.ppn + " " + t.valid + " " + t.dirty + " "
+ t.used + " " + t.readOnly);
}
System.out.println( "Inverted page table" );
for( int i = 0 ; i < invertedPageTable.length ; i++ ) {
TranslationEntry t = invertedPageTable[i];
System.out.println( t.vpn + " " + t.ppn + " " + t.valid + " " + t.dirty + " "
+ t.used );

}*/
		// When we don't have more pages left
//System.out.println( "Handling page fault" );
		if( !ivtLock.isHeldByCurrentThread() ) ivtLock.acquire();
		int phyPage;
		if( VMKernel.getNumFreePages() <= 0 ) {
		  // TODO need to evict pages and get the page back in
//			System.out.println( "we need swaping" );
			phyPage = swapOut();
		}
	  else phyPage = VMKernel.giveOnePage();

    // Get the page number where fault happened
		int faultPage = faultAddr / pageSize;
//System.out.println( "We are faulting on page " + faultPage + " that is mapped to "
                 //  + phyPage + " physical page." );

		// Map the prcess to the actual physcial page
		//ivtLock.acquire();
		invertedPageTable[phyPage].ppn = this.processID;
		invertedPageTable[phyPage].vpn = faultPage;
		//ivtLock.release();

    //if( !this.PTLock.isHeldByCurrentThread()) this.PTLock.acquire();
    pageTable[faultPage].ppn = phyPage;
    //if( pageTable[faultPage].dirty && !pageTable[faultPage].readOnly) {
  	if( pageTable[faultPage].vpn >= 0 ) {
			swapIn( this, faultPage );

			// Wipe the plate clean
			//pageTable[faultPage].valid = true;
			pageTable[faultPage].dirty = false;
			pageTable[faultPage].used = false;
			pageTable[faultPage].readOnly = false;
			//pageTable[faultPage].readOnly = false;
		
		}
		// when the page has not been loaded before
		else if( !pageTable[faultPage].used || pageTable[faultPage].vpn < 0 ) {
		  int s = 0;
			boolean loaded = false;
		  // Get the appropriate data into the physical page
      for( ; s < coff.getNumSections() && !loaded; s++ ) {
        CoffSection section = coff.getSection(s);

			  for( int i = 0 ; i < section.getLength() ; i++ ) {
          int vpn = section.getFirstVPN() + i;
//System.out.println( "In section " + section.getName() + " with page " + vpn );
				  // If we found the page we are faulting on
				  if( faultPage == vpn ) {
            section.loadPage(i, pageTable[faultPage].ppn );
					  if( section.isReadOnly() ) pageTable[faultPage].readOnly = true;
						else pageTable[faultPage].readOnly = false;

//System.out.println( "loading page " + vpn + " at section " + section.getName());
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
				pageTable[faultPage].dirty = false;
		  }  

		  //pageTable[faultPage].valid = true;
		}
		      
    //invertedPageTable[phyPage].valid = true;
		//invertedPageTable[phyPage].dirty = false;	
		pageTable[faultPage].valid = true;
		//pageTable[faultPage].dirty = true;
		pageTable[faultPage].used = false;
/*********************************************************************
 */
		if( toPin ) {
      invertedPageTable[phyPage].readOnly = true;
			pinPageCount++;
			toPin = false;
		}

	//	if( this.PTLock.isHeldByCurrentThread()) this.PTLock.release();
    ivtLock.release();
		return 1;
	}

  // Choose a page, write its content to swap file, and mark it clean to use
  public int swapOut(){

		if( pinPageCount == Machine.processor().getNumPhysPages() ) {
      System.out.println( "All pages pinned");
      allPinned.sleep();
		}

		while( true ){
//System.out.println( "Couting pages " + clockHand );
      clockHand --;
			if( clockHand < 0 ) clockHand = Machine.processor().getNumPhysPages()-1;
			// If we see a page that has been used
      if( invertedPageTable[clockHand].used )
			  invertedPageTable[clockHand].used = false;
			else if( !invertedPageTable[clockHand].readOnly ){ 
//System.out.println( "We found the page" );
			  // Otherwise we have chosen our victim
			  // Get the process that's using the physical page
			  VMProcess process = (VMProcess)VMKernel.allProcesses.get(
					                                    invertedPageTable[clockHand].ppn);
				// Updite its page table
				int clockPos = clockHand;
			  int visPageNum = invertedPageTable[clockHand].vpn;	
				invertedPageTable[clockHand].used = true;
        //ivtLock.release();

        //process.PTLock.acquire();
//System.out.println( "Changing process " + process.processID + "'s page " + visPageNum );
        process.pageTable[visPageNum].valid = false;
				process.pageTable[visPageNum].dirty = false;

//System.out.println( "setting page " + visPageNum + " to " + pageTable[visPageNum].valid );        
				if( //process.pageTable[visPageNum].dirty && 
				    !process.pageTable[visPageNum].readOnly ){
				  // We need to write the file out to swap
          //OpenFile swapFile = ThreadedKernel.fileSystem.open( swapFileName, true);
					
					// See if the virtual page has a swap pos already
					int filePost = -1;
					if( process.pageTable[visPageNum].vpn < 0 ) {
					 // System.out.println( "Pgae " + visPageNum
						                 // + " at process " + process.processID + 
															//	" not swapped to file before!" );
						filePost = VMKernel.giveSwapPage();
						process.pageTable[visPageNum].vpn = filePost;
//System.out.println( "Assigning " + filePost + " to page " + visPageNum );
					}
					else filePost = process.pageTable[visPageNum].vpn;
//System.out.println( "We are writing to file at " + filePost + " to write vpage " + visPageNum);
					int bitsWrote = VMKernel.swapFile.write( filePost * pageSize, 
					                Machine.processor().getMemory(),
					                clockPos*pageSize, pageSize );

//System.out.println( "We write " + bitsWrote + " bits" );					
				//	swapFile.close();
				}
        //process.PTLock.release();

				return clockPos;
			}// end of else
			//else System.out.println( "All readonly" );
		}
	}

	public int swapIn( VMProcess process, int faultPage ) {
		int filePost = process.pageTable[faultPage].vpn; // The spn
		// REad from the file and load it into the page
		int bytesRead = VMKernel.swapFile.read( filePost*pageSize,
		               Machine.processor().getMemory(),
									 process.pageTable[faultPage].ppn * pageSize, pageSize );
		return 1;
	}

	public void pinPage( int virPage ){
	  //this.PTLock.acquire();
		ivtLock.acquire();
		if( this.pageTable[virPage].ppn >= 0 && this.pageTable[virPage].valid) {
		  pinPageCount++;
//System.out.println( "Have pinned " + pinPageCount );
      invertedPageTable[ pageTable[virPage].ppn ].readOnly = true;
			ivtLock.release();
			return;
		}
		ivtLock.release();

		/*ivtLock.acquire();
	  
    //else this.PTLock.release();
	  //ivtLock.acquire();
		//this.PTLock.acquire();
    int phyPage = this.pageTable[virPage].ppn;
		//this.PTLock.release();
		//ivtLock.acquire();
		invertedPageTable[phyPage].readOnly = true;
//System.out.println( "Process " + this.processID + " *******************__________Pinning page " + phyPage + " " + virPage + this.pageTable[virPage].valid );
//System.out.println( "readOnly: " + invertedPageTable[phyPage].readOnly );
		//ivtLock.release();*/
		toPin = true;
		//ivtLock.release();
		// If all physical pages are pinned
		/*if( pinPageCount == invertedPageTable.length ) {
      allPinned.sleep();
		}*/
	}

	public boolean isPined( int virPage ){
    int phyPage = this.pageTable[virPage].ppn;
		ivtLock.acquire();
    boolean isPinned = invertedPageTable[phyPage].readOnly;
		ivtLock.release();
		return isPinned;
	}

	public void unPin( int virPage ){
	  ivtLock.acquire();
    int phyPage = this.pageTable[virPage].ppn;
		//ivtLock.acquire();
		invertedPageTable[phyPage].readOnly = false;
		//ivtLock.release();
		pinPageCount--;
//System.out.print( "Unpinned " + pinPageCount );
		if( pinPageCount == (invertedPageTable.length-1)) allPinned.wake();
		ivtLock.release();
	}
}
