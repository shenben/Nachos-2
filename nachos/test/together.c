#include "stdio.h"
#include "stdlib.h"

int testClose(){
  int i;
	for( i = 0 ; i < 13 ; i++ ) {
    int creatRet = creat( "ToBeClosed" );
		if( creatRet == -1 ) {
      printf( "Creating file failed.\n");
			return -1;
		}
    
		char * string = "Bulalalalalalala";
		write( creatRet, string, 50 );

		close( creatRet );
		int writeRet = write( creatRet, string, 10 );
		if( writeRet != -1 ) {
       printf( "Closing file failed.\n" );
		}

		int openRet = open( "ToBeClosed" );
		if( creatRet != openRet ) {
      printf( "Opening file failed.\n" );
			return -1;
		}
    
		char stringRead[30];
		read( openRet, stringRead, 30 );
		write( 1, stringRead, 30 );
		close( openRet );
		return 0;
	}
}

int main() {
  printf( "\nTesting closing files...\n" );
	testClose();
}
