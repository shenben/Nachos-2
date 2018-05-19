int testCloseNormal() {
  int fileD = creat( "ThisIsNotDrill" );
	if( fileD == -1 ) {
    printf( "Fail to open file: ThisIsNotDrill\n" );
		return -1;
	}
  int retClose = close( fileD );
	if( retClose == 0 ) {
    printf( "Successfully closed file.\n" );
		return 0;
	}
	
	int fileH = creat( "Again" ); // Create the file again
	int fileR = creat( "again2" );
	close( fileH );
	int fileZ = creat( "Again3" );
	if( fileZ != 2 ) {
	
	  printf( "The file descripter is not freed" );
  }

  close( fileZ );
  char * string = "hello world";
	int retWrite = write( fileZ, string, 10);
  if( retWrite == -1 ) printf( "We are not supposed to write to a closed file.\n");
	else return -1;
}

int testCloseUnopened() {
  int retClose = close( 5 );
	if( retClose != -1 ) printf( "Error check fails.\n" );
	else printf( "Error checked!\n" );
	return 0;
}

int main(){
  printf( "\nTesting close() syscall...\n" );
  if( testCloseNormal() == -1 ) printf( "Normal case failed.\n" );
  //testCloseUnopened();
	halt();
}
