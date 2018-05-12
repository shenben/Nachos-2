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

int main() {
  //printf( "\nTesting create() syscall...\n");

	testCreateNewFile();
	testCreateOldFile();

  halt();
 // printf( "\nFinished testing create syscall.\n" );
}
