package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.io.EOFException;

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
		int numPhysPages = Machine.processor().getNumPhysPages();
		numPages = 0;
		pageTable = new TranslationEntry[numPhysPages];

    // Only gets a page when you need one
		/*for (int i = 0; i < numPhysPages; i++)
			pageTable[i] = new TranslationEntry(i, i, true, false, false, false);

			fileTable = new OpenFile[maxOpenFiles];
			// 0 must for standard reading
			fileTable[0] = UserKernel.console.openForReading();
			fileCount++;
			fileTable[1] = UserKernel.console.openForWriting();
			fileCount++;*/
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

		new UThread(this).setName(name).fork();

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

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(memory, vaddr, data, offset, amount);

		return amount;
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
		Lib.assertTrue(offset >= 0 && length >= 0
				&& offset + length <= data.length);

		byte[] memory = Machine.processor().getMemory();

		// for now, just assume that virtual addresses equal physical addresses
		if (vaddr < 0 || vaddr >= memory.length)
			return 0;

		int amount = Math.min(length, memory.length - vaddr);
		System.arraycopy(data, offset, memory, vaddr, amount);

		return amount;
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
			return false;
		}

		try {
			coff = new Coff(executable);
		}
		catch (EOFException e) {
			executable.close();
			Lib.debug(dbgProcess, "\tcoff load failed");
			return false;
		}

		// make sure the sections are contiguous and start at page 0
		numPages = 0;
		for (int s = 0; s < coff.getNumSections(); s++) {
			CoffSection section = coff.getSection(s);
			if (section.getFirstVPN() != numPages) {
				coff.close();
				Lib.debug(dbgProcess, "\tfragmented executable");
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
			return false;
		}

		// For each page we need, we request for one page
		for( int i = 0 ; i < numPages ; i++ ) {
      requestOnePage();
		}
		// Load sections
		for( int s = 0 ; s < coff.getNumSections() ; s++ ) {
      CoffSection section = coff.getSection(s);

			Lib.debug( dbgProcess, "\tinitializing " + section.getName() +
			  " section ( " + section.getLength() + " pages)" );

			for( int i = 0 ; i < section.getLength() ; i++ ) {
        int vpn = section.getFirstVPN() + i;
				section.loadPage(pageTable[vpn].ppn, vpn);
			}
		}
	}

	/**
	 * Release any resources allocated by <tt>loadSections()</tt>.
	 */
	protected void unloadSections() {
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
		fileCount++;
		processor.writeRegister(Processor.regA1, argv);
		fileCount++;
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

		// True to create the file if it does not exist yet
		OpenFile fileOpened = ThreadedKernel.fileSystem.open( filename, true );
		if( fileOpened == null ) return -1; // Error

		// Get a space in the fileTable for this file
		for( int i = 0 ; i < maxOpenFiles ; i++ ){
      if( fileTable[i] == null ){
        fileTable[i] = fileOpened;
				fileCount++;
//System.out.println( "now we created file " + filename + " with decriptor " + i);
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

		if( count < 0 ) return -1; // Error
		if( count == 0 ) return 0; 

    byte[] buf = new byte[pageSize];
		int numBytesReadTotal = 0;
		int numBytesReadOnce = 0;

		while( numBytesReadTotal < count ) {
		  if( (count - numBytesReadTotal) >= pageSize ) numBytesReadOnce = pageSize;
			else numBytesReadOnce = count - numBytesReadTotal;

		  // Keep reading until we get all the bytes we want
		  numBytesReadOnce = file.read(buf, 0, numBytesReadOnce );
//System.out.println( "printing the bytes: " +  new String(buf));

			// Write the read bytes off to the pointer
      int retVal =  writeVirtualMemory( bufaddr + numBytesReadTotal, 
			                                  buf, 0, numBytesReadOnce);

      if( retVal <= 0 ) return -1; // Out of memory for pointer
			
			numBytesReadTotal += numBytesReadOnce;
		}
//System.out.println( "We read bytes : " + numBytesReadTotal );
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

		if( count < 0 ) return -1;
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

//System.out.println( "in write we are writing: " + new String(buf));
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
	 * Handle the halt() system call.
	 */
	private int handleHalt() {

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
System.out.println( "syscall is : " + syscall);
		switch (syscall) {
		case syscallHalt:
			return handleHalt();
		case syscallExit:
			return handleExit(a0);
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
	private static int fileCount = 0;
	protected static OpenFile[] fileTable;
	private int maxLen = 256; // Max bits for the name of a file

	/** For handling multiprogramming */
	/**
	 * request one page from kernel
	 */
	private int requestOnePage(){
    int newPage = UserKernel.giveOnePage();
		if( newPage == -1 ) return -1; // Out of memory

		// Add the new physical page to the process's page table
		pageTable[numPages] = new TranslationEntry(numPages, newPage,
		                                           true, false, false );
		numPages++;
    return newPage;
	}
}
