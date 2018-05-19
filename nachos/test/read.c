/* read.c
 * test handleRead and testOpen at the same time
 *
 */

#include "syscall.h"
#include "stdio.h"


int testReadOneLine() {
  char buf[30];
	int ret = read( open("thisIsNotDrill"), buf, 15);
  if ( ret == -1 ) printf ( "Error has occured, %d \n ", ret );
	else printf( "Success. Read: %s", buf );
}

int testReadLargeFile() {
  
}

int main(){
  testReadOneLine();
	halt();
}
