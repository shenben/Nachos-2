/**
 * write.c
 *
 * Testing the basic writing function
 */


#include "stdio.h"
#include "stdlib.h"

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

int testWrite4() {
  char buffer[80];
	char prompt[4];
	int i,n;

	prompt[0] = '-';
	prompt[1] = '>';
	prompt[2] = ' ';
	prompt[3] = '\0';

	while( 1 ) {
    // Print the prompt.
		puts( prompt );

		// Read the input terminated by a newline
		i = 0 ; 
		do {
		  buffer[i] = getchar();
		} while( buffer[i++] != '\n' );
	  buffer[i] = '\0';

		// If the input is just a period, then exit
		if( buffer[0] == '.' && buffer[1] == '\n' ) {
      return 0;
		}
		puts(buffer);
	}
}


int main() {
  testWriteToOut();
  //testWriteToFile();
	//testWriteEmptyString();
	//testWriteFromRead();
	//testWriteLargeFile();
//	testWrite4();
	halt();
}
