/**
 * write.c
 *
 * Testing the basic writing function
 */

int testWriteToOut(){
  char * str = "\nThis is the basic test of write().";
  write( 1, str, 10 );
}

int testWriteToFile() {
  char * str = "\nThis is the written to file test.";
	int file = creat( "fileForWrite" );
	write( file, str, 20 );
}

int testWriteEmptyString(){
  char * str = "";
	int file = open( "fileForWrite" );
	write( file, str, 10 );
}

int testWriteFromRead(){
  int file = open( "thisIsNotDrill" );
	int file2 = open( "thisIsNotADrill" );
	char buf[50];
	read( file, buf, 10 );
	write( file2, buf, 10 );
}

int testWriteLargeFile(){
  int file = open( "largeFile" );
	int file2 = creat( "largeFileDump" );
	char buf[1000];
	read( file, buf, 1000 );
	write(file2, buf, 1000 );
}

int main() {
  testWriteToOut();
  testWriteToFile();
	testWriteEmptyString();
	testWriteFromRead();
	testWriteLargeFile();
	halt();
}
