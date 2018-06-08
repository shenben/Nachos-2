package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;
import java.nio.ByteBuffer;

import java.io.EOFException;

import java.util.*;

/**
 * Encapsulates the state of a user process that is not contained in its user
 * thread (or threads). This includes its address translation state, a file
 * table, and information about the program being executed.
 * 
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 * 
 * @see nachos.vm.VMProcess
 * @see nachos.network.NetProcess
 */
public class UserProcess {
	/**
	 * Allocate a new process.
	 */
	public UserProcess() {

			fileTable = new OpenFile[maxOpenFiles];
			// 0 must for standard reading
			fileTable[0] = UserKernel.console.openForReading();
			fileCount++;
			fileTable[1] = UserKernel.console.openForWriting();
			fileCount++;

			childProcesses = new HashMap<Integer, UserProcess>();
			childExits = new HashMap<Integer, Integer>();
			childLock = new Lock();
	}

	/**
	 * Allocate and return a new process of the correct class. The class name is
	 * specified by the <tt>nachos.conf</tt> key
	 * <tt>Kernel.processClassName</tt>.
	 * 
	 * @return a new process of the correct class.
	 */
	public static UserProcess newUserProcess() {
	        String name = Machine.getProcessClassName ();

		// If Lib.constructObject is used, it quickly runs out
		// of file descriptors and throws an exception in
		// createClassLoader.  Hack around it by hard-coding
		// creating new processes of the appropriate type.

		if (name.equals ("nachos.userprog.UserProcess")) {
		    return new UserProcess ();
		} else if (name.equals ("nachos.vm.VMProcess")) {
		    return new VMProcess ();
		} else {
		    return (UserProcess) Lib.constructObject(Machine.getProcessClassName());
		}
	}

	/**
	 * Execute the specified program with the specified arguments. Attempts to
	 * load the program, and then forks a thread to run it.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the program was successfully executed.
	 */
	public boolean execute(String name, String[] args) {
		if (!load(name, args))
			return false;

   // currentThread = KThread.currentThread();
	  //new UThread(this).setName(name).fork();
		currentThread = new UThread(this).setName(name);
		currentThread.fork();

		return true;
	}

	/**
	 * Save the state of this process in preparation for a context switch.
	 * Called by <tt>UThread.saveState()</tt>.
	 */
	public void saveState() {
	}

	/**
	 * Restore the state of this process after a context switch. Called by
	 * <tt>UThread.restoreState()</tt>.
	 */
	public void restoreState() {
		Machine.processor().setPageTable(pageTable);
	}

	/**
	 * Read a null-terminated string from this process's virtual memory. Read at
	 * most <tt>maxLength + 1</tt> bytes from the specified address, search for
	 * the null terminator, and convert it to a <tt>java.lang.String</tt>,
	 * without including the null terminator. If no null terminator is found,
	 * returns <tt>null</tt>.
	 * 
	 * @param vaddr the starting virtual address of the null-terminated string.
	 * @param maxLength the maximum number of characters in the string, not
	 * including the null terminator.
	 * @return the string read, or <tt>null</tt> if no null terminator was
	 * found.
	 */
	public String readVirtualMemoryString(int vaddr, int maxLength) {
		Lib.assertTrue(maxLength >= 0);

		byte[] bytes = new byte[maxLength + 1];

		int bytesRead = readVirtualMemory(vaddr, bytes);

		for (int length = 0; length < bytesRead; length++) {
			if (bytes[length] == 0)
				return new String(bytes, 0, length);
		}

		return null;
	}

	/**
	 * Transfer data from this process's virtual memory to all of the specified
	 * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data) {
		return readVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from this process's virtual memory to the specified array.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to read.
	 * @param data the array where the data will be stored.
	 * @param offset the first byte to write in the array.
	 * @param length the number of bytes to transfer from virtual memory to the
	 * array.
	 * @return the number of bytes successfully transferred.
	 */
	public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		if ( vaddr < 0 || vaddr >= memory.length) return -1;
/*
		int amount = Math.min( length, memory.length - vaddr );
		System.arraycopy( memory, vaddr, data, offset, amount);
		return amount;*/

    int bytesRead = 0;
    // Break the length into pages
    int vPageBegin = vaddr / pageSize;
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

//		int amount = Math.min(length, memory.length - vaddr);
//		System.arraycopy(memory, vaddr, data, offset, amount);
	}

	/**
	 * Transfer all data from the specified array to this process's virtual
	 * memory. Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data) {
		return writeVirtualMemory(vaddr, data, 0, data.length);
	}

	/**
	 * Transfer data from the specified array to this process's virtual memory.
	 * This method handles address translation details. This method must
	 * <i>not</i> destroy the current process if an error occurs, but instead
	 * should return the number of bytes successfully copied (or zero if no data
	 * could be copied).
	 * 
	 * @param vaddr the first byte of virtual memory to write.
	 * @param data the array containing the data to transfer.
	 * @param offset the first byte to transfer from the array.
	 * @param length the number of bytes to transfer from the array to virtual
	 * memory.
	 * @return the number of bytes successfully transferred.
	 */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
		/*Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;*/
  	Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

    int bytesRead = 0;
		byte[] memory = Machine.processor().getMemory();

    // Break the length into pages
    int vPageBegin = vaddr / pageSize;
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
	 * Load the executable with the specified name into this process, and
	 * prepare to pass it the specified arguments. Opens the executable, reads
	 * its header information, and copies sections and arguments into this
	 * process's virtual memory.
	 * 
	 * @param name the name of the file containing the executable.
	 * @param args the arguments to pass to the executable.
	 * @return <tt>true</tt> if the executable was successfully loaded.
	 */
	private boolean load(String name, String[] args) {
		Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");

		OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
		if (executable == null) {
			Lib.debug(dbgProcess, "\topen failed");
      System.out.println("open failed");
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
      System.out.println("coff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
        System.out.println("fragmented exec");
				return false;
			}
			numPages += section.getLength();
		}

		// make sure the argv array will fit in one page
		byte[][] argv = new byte[args.length][];
		int argsSize = 0;
		for (int i = 0; i < args.length; i++) {
			argv[i] = args[i].getBytes();
			// 4 bytes for argv[] pointer; then string plus one for null byte
			argsSize += 4 + argv[i].length + 1;
		}
		if (argsSize > pageSize) {
			coff.close();
			Lib.debug(dbgProcess, "\targuments too long");
      System.out.println("args too long");
			return false;
		}

		// program counter initially points at the program entry point
		initialPC = coff.getEntryPoint();

		// next comes the stack; stack pointer initially points to top of it
		numPages += stackPages;
		initialSP = numPages * pageSize;

		// and finally reserve 1 page for arguments
		numPages++;

		if (!loadSections())
			return false;

    System.out.println("loadSections returns true");

		// store arguments in last page
		int entryOffset = (numPages - 1) * pageSize;
		int stringOffset = entryOffset + args.length * 4;

		this.argc = args.length;
		this.argv = entryOffset;

		for (int i = 0; i < argv.length; i++) {
			byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
			Lib.assertTrue(writeVirtualMemory(entryOffset, stringOffsetBytes) == 4);
			entryOffset += 4;
			Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) == argv[i].length);
			stringOffset += argv[i].length;
			Lib.assertTrue(writeVirtualMemory(stringOffset, new byte[] { 0 }) == 1);
			stringOffset += 1;
		}
    
    System.out.println("at end");

		return true;
	}

	/**
	 * Allocates memory for this process, and loads the COFF sections into
	 * memory. If this returns successfully, the process will definitely be run
	 * (this is the last step in process initialization that can fail).
	 * 
	 * @return <tt>true</tt> if the sections were successfully loaded.
	 */
	protected boolean loadSections() {
		/*if (numPages > Machine.processor().getNumPhysPages()) {
			coff.close();
			Lib.debug(dbgProcess, "\tinsufficient physical memory");
			return false;
		}

		// load sections
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);

			Lib.debug(dbgProcess, "\tinitializing " + section.getName()
					+ " section (" + section.getLength() + " pages)");

			for (int i = 0; i < section.getLength(); i++) {
				int vpn = section.getFirstVPN() + i;

				// for now, just assume virtual addresses=physical addresses
				section.loadPage(i, vpn);
			}
		}

		return true;*/
		if( numPages > UserKernel.getNumFreePages() ) {
      coff.close();
			Lib.debug( dbgProcess, "\tinsufficient physical memory." );
      System.out.println("unsufficient phys mem");
			return false;
		}

    pageTable = new TranslationEntry[numPages];
		// For each page we need, we request for one page
		for( int i = 0 ; i < numPages ; i++ ) {
      if( requestOnePage(i) == -1 ){
        returnPages();
				coff.close();
        System.out.println("not enough pages");
				return false;
			}
		}
		// Load sections
		for( int s = 0 ; s < coff.getNumSections() ; s++ ) {
      CoffSection section = coff.getSection(s);

			Lib.debug( dbgProcess, "\tinitializing " + section.getName() +
			  " section ( " + section.getLength() + " pages)" );

			for( int i = 0 ; i < section.getLength() ; i++ ) {
        int vpn = section.getFirstVPN() + i;
				section.loadPage(i, pageTable[vpn].ppn);
				if( section.isReadOnly()) pageTable[vpn].readOnly = true;
			}
		}

//System.out.println( "We have in total " + numPages + " for PID " + this.processID);
		return true;
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
    // Return all the pages
	  returnPages();
	}

	/**
	 * Initialize the processor's registers in preparation for running the
	 * program loaded into this process. Set the PC register to point at the
	 * start function, set the stack pointer register to point at the top of the
	 * stack, set the A0 and A1 registers to argc and argv, respectively, and
	 * initialize all other registers to 0.
	 */
	public void initRegisters() {
		Processor processor = Machine.processor();

		// by default, everything's 0
		for (int i = 0; i < processor.numUserRegisters; i++)
			processor.writeRegister(i, 0);

		// initialize PC and SP according
		processor.writeRegister(Processor.regPC, initialPC);
		processor.writeRegister(Processor.regSP, initialSP);

		// initialize the first two argument registers to argc and argv
		processor.writeRegister(Processor.regA0, argc);
		processor.writeRegister(Processor.regA1, argv);
	}

  /**
	 * Handle the creat() system call
	 *
	 * @param nameaddr - the vma of the string name of the 
	 *                   file user wants to create
	 *
	 * @return file descriptor if no error occurs
	 *         -1 if an error ocurrs
	 */
  private int handleCreat(int nameaddr) {
    // Check that we have enough space for opening new files
		if( fileCount >= maxOpenFiles ) return -1;

    String filename = readVirtualMemoryString( nameaddr, maxLen );
		if( filename == null ) return -1; // Error

//System.out.println( this.processID + " Create the file " + filename );
		// True to create the file if it does not exist yet
		OpenFile fileOpened = ThreadedKernel.fileSystem.open( filename, true );
		if( fileOpened == null ) return -1; // Error

		// Get a space in the fileTable for this file
		for( int i = 0 ; i < maxOpenFiles ; i++ ){
      if( fileTable[i] == null ){
        fileTable[i] = fileOpened;
				fileCount++;
//System.out.println( "Now  have " + fileCount + " files open" );
				return i;
			}
		}

		Lib.assertNotReached( "The file is not stored in the table!" );
		return -1; // Error
		
	}

	/**
	 * Handle the open() system call.
	 *
	 * @param nameaddr - the vma of the string name of the 
	 *                   file user wants to open
	 *
	 * @return file descriptor if no error occurs
	 *         -1 if an error ocurrs
	 */
  private int handleOpen(int nameaddr) {
    // Check that we have enough space for opening new files
		if( fileCount >= maxOpenFiles ) return -1;

    String filename = readVirtualMemoryString( nameaddr, maxLen );
//System.out.println( this.processID + " is the PId and Get the file " + filename );
		if( filename == null ) return -1; // Error

		// False so we don't create the file if it does not exist
		OpenFile fileOpened = ThreadedKernel.fileSystem.open( filename, false );
		if( fileOpened == null ) return -1; // Error

		// Get a space in the fileTable for this file
		for( int i = 0 ; i < maxOpenFiles ; i++ ){
      if( fileTable[i] == null ){
        fileTable[i] = fileOpened;
				fileCount++;
				return i;
			}
		}

		Lib.assertNotReached( "The file is not stored in the table!" );
		return -1; // Error
		
	}

	/**
	 * Handle the read() system call.
	 *
	 * @param fileDescriptor - the index of the file we want 
	 * @param bufaddr - pointer to where we want to transfer the data from the file
	 * @param count - how many bytes we want from the file
	 */
	private int handleRead(int fileDescriptor, int bufaddr, int count) {
	  if( fileDescriptor >= maxOpenFiles || fileDescriptor < 0 ) return -1;
    OpenFile file = fileTable[fileDescriptor];
		if( file == null ) return -1; // Error

//System.out.println( "reading from file " + file.getName() );
		if( count < 0 ) return -1; // Error
		if( count == 0 ) return 0; 

    byte[] buf = new byte[pageSize];
		int numBytesReadTotal = 0;
		int numBytesReadOnce = 0;

		while( numBytesReadTotal < count ) {
		  if( (count - numBytesReadTotal) >= pageSize ) numBytesReadOnce = pageSize;
			else numBytesReadOnce = count - numBytesReadTotal;

		  // Keep reading until we get all the bytes we want
		  int retVal = file.read(buf, 0, numBytesReadOnce );

      int wetVal = writeVirtualMemory( bufaddr + numBytesReadTotal,
			                                 buf, 0, retVal );
      numBytesReadTotal += retVal;

			// If we get an 0 from either, then an error occured
			if( retVal == 0 || wetVal == 0 ) return -1;

      // If we read less than what we want, then there is an error
			// or we don't have that many bytes
      if( retVal < numBytesReadOnce ) break;
		}
    return numBytesReadTotal;
	}

  /**
	 * Handle the write() system call.
	 *
	 * @param fileDescriptor - the index of the file we want to write it
	 * @param bufaddr - the address of the butter where we are writing from
	 * @param count - the number of bytes we want to write to file
	 */
	private int handleWrite(int fileDescriptor, int bufaddr, int count) {
    if( fileDescriptor >= maxOpenFiles || fileDescriptor < 0 ) return -1;
		OpenFile file = fileTable[fileDescriptor];
		if( file == null ) return -1;

		if( count < 0) return -1;
		if( count == 0 ) return 0;

    byte[] buf = new byte[pageSize];
		int numBytesWrittenTotal = 0;
		int numBytesWrittenOnce = 0;

		while( numBytesWrittenTotal < count ) {
		  // Calculate how many bytes to get from buffer
		  if( ( count - numBytesWrittenTotal ) >= pageSize ) 
			  numBytesWrittenOnce = pageSize;
			else numBytesWrittenOnce = count - numBytesWrittenTotal;

			// Get the bytes from bufaddr to buf
			int retValSucking = readVirtualMemory( bufaddr + numBytesWrittenTotal,
		                                  buf, 0, numBytesWrittenOnce );
      if( retValSucking <= 0 ) return -1;

			numBytesWrittenTotal += numBytesWrittenOnce;

			// Write buf into file
			int retValWriting = file.write( buf, 0, retValSucking );
			if( retValWriting == -1 ) return -1;
		}

		return numBytesWrittenTotal;
	}

  /**
	 * Hand the close() system call.
	 *
	 * @param fileDescriptor - the file index we want to close
	 */
	private int handleClose(int fileDescriptor){
    if( fileDescriptor < 0 || fileDescriptor >= maxOpenFiles ) return -1;
		OpenFile file = fileTable[fileDescriptor];
		if( file == null ) return -1;

		file.close();
		fileTable[fileDescriptor] = null;
		fileCount--;
//System.err.println( this.processID + " PID We have " + fileCount + " file left." );

		return 0;
	}

  private int handleUnlink(int nameaddr){
    if( fileCount <= 0 ) return -1;
		String fileName = readVirtualMemoryString( nameaddr, maxLen );

		if( fileName == null ) return -1;
		
		if( ThreadedKernel.fileSystem.remove(fileName) ) return 0;

		return -1; //error
	}

  /**
	 * Handle the exec() system call.
	 */
	private int handleExec(int fileNameAddr, int argc, int argvAddr ) {
    System.out.println("IN EXEC HANDLER");
	  String extCoff = ".coff";
    if( fileCount == maxOpenFiles ) {
      System.out.println("1");
      return -1;
    }
		if( argc < 0 ) return -1;
    System.out.println("2");
		if( argvAddr < 0 || argvAddr > numPages * pageSize ) return -1;

    // Load the file
    System.out.println("3");
		String fileName = readVirtualMemoryString( fileNameAddr, maxLen );
    System.out.println("name addr: " + fileNameAddr);
    System.out.println("fn: " + fileName);
    System.out.println("is null? " + (fileName == null));
		if( fileName == null || fileName.length() <= extCoff.length()) return -1;

		// Check the coff extension
    System.out.println("4");
		String extension = fileName.substring( fileName.length() - extCoff.length(),
		                                       fileName.length());
		if( !extension.equals( extCoff )) return -1;

    // Try open the file. If the file is not openable, then return -1

    //if( ThreadedKernel.fileSystem.open( fileName, false) == null ) return -1;
    System.out.println("MADE IT THIS FAR");


		// Get all the args
		byte ptr[] = new byte[sizeOfPtr];
		int argvPtr; // Pointer to string
		String argv[] = new String[argc]; // Strings
	//	int argv[] = new int[argc];
		for( int i = 0 ; i < argc ; i++ ) {
      int numBytesRead = readVirtualMemory( (i * sizeOfPtr + argvAddr), 
			                                       ptr, 0, sizeOfPtr );
			if( numBytesRead != sizeOfPtr ) return -1;
			// Got this way of converting 4 bytes to an int from
			// https://stackoverflow.com/questions/9581530/converting-from-byte-to-int-in-java
			argvPtr= (ptr[3] << 24) & 0xff000000 |
			         (ptr[2] << 16) & 0x00ff0000 |
							 (ptr[1] << 8 ) & 0x0000ff00|
							 (ptr[0] << 0 ) & 0x000000ff;
			if( argvPtr < 0 || argvPtr > numPages * pageSize ) return -1;

      argv[i] = readVirtualMemoryString( argvPtr, maxLen );
		 //argv[i] = argvPtr;
			if( argv[i] == null ) return -1;
		}

		UserProcess childProcess = UserProcess.newUserProcess();
		if( childProcess == null ) return -1;
		int id = UserKernel.increaseProcess();
		childProcess.setPID( id );
		childProcess.setParent( this );
		this.addChildProcess( id, childProcess );

    Lib.assertTrue( childProcess.execute( fileName, argv ) );
		return id;
	}

  /**
	 * Handle join() system call.
	 */
	private int handleJoin(int childPID, int statusAddr) {
    // First check if the PID of the child's PID is still a child
		if( !childProcesses.containsKey( childPID ) || statusAddr < 0 ) return -1;
		UserProcess childProcess = childProcesses.get( childPID );

    if( !childProcess.exited ){
      childProcess.currentThread.join();
		}

    // Resume and disown the child, check the exit status
		if( !childExits.containsKey( childPID ) ) return -1;

		int childExitStat = childExits.get( childPID );
System.out.println( "child exited with " + childExitStat );
    byte childExitBytes[] = new byte[sizeOfPtr];
		// Write that value to the address
		// Get the method from 
		// stackoverflow.com/questions/6374915/java-convert-int-to-byte-array-of-4-bytes
		childExitBytes[0] = (byte)(childExitStat);
		childExitBytes[1] = (byte)(childExitStat >>> 8);
		childExitBytes[2] = (byte)(childExitStat >>> 16);
		childExitBytes[3] = (byte)(childExitStat >>> 24);

		if( writeVirtualMemory( statusAddr, childExitBytes, 0, sizeOfPtr ) 
		    != sizeOfPtr ) return -1;
		else if( childProcess.exitAbnormal ) return 0;
		else return 1;
	//	if( childExitStat != 1 ) return 0;
    //else return 1;
	}

	/**
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

    if( processID != 0 ) return -1;
		Machine.halt();

		Lib.assertNotReached("Machine.halt() did not halt machine!");
		return 0;
	}

	/**
	 * Handle the exit() system call.
	 */
	private int handleExit(int status) {
	        // Do not remove this call to the autoGrader...
		Machine.autoGrader().finishingCurrentProcess(status);
		// ...and leave it as the top of handleExit so that we
		// can grade your implementation.
		
    System.out.println("exit status: " + status);

		// Free up memories
		returnPages();
		// Close all the files
		for( int i = 0 ; i < maxOpenFiles ; i ++ ) {
      if( fileTable[i] != null ) {
        fileTable[i].close();
				fileTable[i] = null;
			}
		}
    // All the children goes to have no parent process
		Iterator it = childProcesses.entrySet().iterator();
		while( it.hasNext() ) {
      Map.Entry pair = (Map.Entry)it.next();
			
			((UserProcess)pair.getValue()).setParent( null );
		}

		if( parentProcess != null ) {
		  parentProcess.addChildExitStatus( this.processID, status );
		 // parentProcess.deleteChildProcess( this.processID );
		}
    if( UserKernel.decreaseProcess() == 0 ) Kernel.kernel.terminate();
		exited = true;
		KThread.currentThread().finish();
		return 0;
	}

	private static final int syscallHalt = 0, syscallExit = 1, syscallExec = 2,
			syscallJoin = 3, syscallCreate = 4, syscallOpen = 5,
			syscallRead = 6, syscallWrite = 7, syscallClose = 8,
			syscallUnlink = 9;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td><tt>void halt();</tt></td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td><tt>void exit(int status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td><tt>int  exec(char *name, int argc, char **argv);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>3</td>
	 * <td><tt>int  join(int pid, int *status);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td><tt>int  creat(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>5</td>
	 * <td><tt>int  open(char *name);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>6</td>
	 * <td><tt>int  read(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>7</td>
	 * <td><tt>int  write(int fd, char *buffer, int size);
	 * 								</tt></td>
	 * </tr>
	 * <tr>
	 * <td>8</td>
	 * <td><tt>int  close(int fd);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>9</td>
	 * <td><tt>int  unlink(char *name);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall the syscall number.
	 * @param a0 the first syscall argument.
	 * @param a1 the second syscall argument.
	 * @param a2 the third syscall argument.
	 * @param a3 the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
//System.out.println( "syscall is : " + syscall);
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
		  return handleExec(a0, a1, a2 );
		case syscallJoin:
		  return handleJoin(a0, a1);
		case syscallCreate:
		  return handleCreat(a0);
		case syscallOpen:
		  return handleOpen(a0);
		case syscallRead:
		  return handleRead(a0, a1, a2);
		case syscallWrite:
		  return handleWrite(a0, a1, a2);
	  case syscallClose:
		  return handleClose(a0);
		case syscallUnlink:
		  return handleUnlink(a0);

		default:
			Lib.debug(dbgProcess, "Unknown syscall " + syscall);
			Lib.assertNotReached("Unknown system call!");
		}
		return 0;
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
		case Processor.exceptionSyscall:
			int result = handleSyscall(processor.readRegister(Processor.regV0),
					processor.readRegister(Processor.regA0),
					processor.readRegister(Processor.regA1),
					processor.readRegister(Processor.regA2),
					processor.readRegister(Processor.regA3));
			processor.writeRegister(Processor.regV0, result);

			processor.advancePC();
			break;

		default:
		  // Same here
			//if( parentProcess != null ) {
        //parentProcess.addChildExitStatus( this.processID, cause );
			//}
      System.out.println("cause = " + cause);
      exitAbnormal = true;
			Lib.debug(dbgProcess, "Unexpected exception: "
					+ Processor.exceptionNames[cause]);
			Lib.assertNotReached("Unexpected exception");
		}
	}

	/** The program being run by this process. */
	protected Coff coff;

	/** This process's page table. */
	protected TranslationEntry[] pageTable;

	/** The number of contiguous pages occupied by the program. */
	protected int numPages;

	/** The number of pages in the program's stack. */
	protected final int stackPages = 8;

	private int initialPC, initialSP;

	private int argc, argv;

	private static final int pageSize = Processor.pageSize;

	private static final char dbgProcess = 'a';

  /** For handling the access to file system */
	private static final int maxOpenFiles = 16;
	private int fileCount = 0;
	protected OpenFile[] fileTable;
	private int maxLen = 256; // Max bits for the name of a file
	private static final int sizeOfPtr = 4;

	//public int exitStatus;
	public KThread currentThread;

	/** For handling multiprogramming */
	/**
	 * request one page from kernel
	 */
	private int requestOnePage(int pagePos){
    int newPage = UserKernel.giveOnePage();
		if( newPage == -1 ) return -1; // Out of memory

		// Add the new physical page to the process's page table
		pageTable[pagePos] = new TranslationEntry(pagePos, newPage,
		                                           true, false, false, false );
		//numPages++;
    return newPage;
	}

	/**
	 * Return all the pages back to Kernel
	 */
  protected int returnPages(){
    if (pageTable == null) return -1; 
    for( int i = 0 ; i < numPages ; i++ ) {
		  if( pageTable[i] != null ) {
        UserKernel.receiveOnePage( pageTable[i].ppn );
				pageTable[i] = null;
      }
		}
		return 0;
	}

	/** For handling multiprocessing */
	public int processID;
	private UserProcess parentProcess;
	private HashMap<Integer, UserProcess> childProcesses;
	private Lock childLock;
	// Map to store child processes's exit statues
	private HashMap<Integer, Integer> childExits; 
	public boolean exited = false;
	public boolean exitAbnormal = false;
  public KThread parentThread = null;
	public int setPID( int itsID ) {
    processID = itsID;
		return processID;
	}

	public void setParent( UserProcess parent ) {
    parentProcess = parent;
	}
	public UserProcess getParent() {
    return parentProcess;
	}
	public int addChildProcess( int id, UserProcess child ) {
    childProcesses.put( id, child );
		return id;
	}
	public void deleteChildProcess( int id ) {
    childProcesses.remove(id);
	}
	public int addChildExitStatus( int id, int status ){
    childExits.put(id, status);
		return status;
	}

	public int getChildExitStatus( int id ) {
    if( childExits.containsKey(id) ) return childExits.get(id);
		else return Integer.MIN_VALUE;
	}
}
