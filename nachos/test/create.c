/* create.c
 * Test program to open an existing file, or create a new one
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int testCreateNewFile() {
 // printf( "\nTesting open new file called thisIsNotADrill...\n");
  if( creat( "thisIsNotADrill" ) != 3) {
	 
  }
	//else printf( "Success.\n");
}

int testCreateOldFile() {
  creat( "thisIsNotADrill");
}

int testTooManyFiles() {
  int i;
	for( i = 0 ; i < 15 ; i++ ) {
	  int ret = creat( "tset" );
    if( ret != -1 ) 
		  printf( "Created file names %d", ret );
		else 
		  printf( "Too many files at i = %d", ret );
	}
}

int main() {
  printf( "\nTesting create() syscall...\n");

	//testCreateNewFile();
	//testCreateOldFile();
  testTooManyFiles();
  halt();
}
