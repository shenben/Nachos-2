/**
 * unlink
 * test unlink system call
 */

#include "stdio.h"
#include "stdlib.h"

int testValidFile() {
  unlink( "toDelete" );
}

int testInvalidFile() {
  unlink( "badname" );
}
int main() {
  testValidFile();
	int r = testInvalidFile();
	if( r == -1 ) printf( "Yes, returned %d.", r );
	else printf( "no, returned %d.", r);
	halt();
}
