/* 
 * exit1.c
 *
 * It does not get simpler than this...
 */
   
#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int testExit(){
  int i = 0;
	for( i = 0 ; i < 10 ; i++ ) {
    printf( "%d's loop\n", i );
		if( i == 8 ){
      exit( 0 );
		}
	}
	printf( "You should not see this\n" );
}


int
main (int argc, char *argv[])
{
   testExit();
	 exit(0);
		//halt();
}
