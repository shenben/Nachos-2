/*
 * exec1.c
 *
 * Simple program for testing exec.  It does not pass any arguments to
 * the child.
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"
int r,l,k;
int i = 0;
int testMultiple() {
 // int r, l, k;
	int childargc = 3;
	char *childargv[3] = {"matmult.coff", "cp.c", "3",};

  r = exec( childargv[0], 1, childargv );
	l = exec( childargv[0], 1, childargv );
	//k = exec( childargv[0], childargc, childargv );

  int rJoin = join( r, &i );
	//if( rJoin == 1 ) printf( "join r successfully\n" );
	int lJoin = join( l, &i );
//	if( lJoin == 1 ) printf( "join l successfully\n" );
	//int kJoin = join( k, &i );
  //if( kJoin == 1 ) printf( "join k successfully\n" );
	//printf( "r has pid: %d, l has pid: %d, and k has pid: %d\n", r,l,k);
	return 0;
}

int testError() {
  int join1 = join( 1, &i );
	if( join1 != -1 ) printf( "Join itself error not checked!" );
	int join2 = join( 0, &i );
	if( join2 != -1 ) printf( "Join root error not checked!");

	r = exec( "does.coff", 0, 0);
	if( r != -1 ) printf( "filename error not checked!" );
	r = exec( "does", 0, 0);
	if( r != -1 ) printf( "filename error not checked!" );
	return 0;
}


int
main (int argc, char *argv[])
{
  //  char *prog = "exit1.coff";
   // int pid;

    //pid = exec (prog, 0, 0);
    // the exit status of this process is the pid of the child process
    //exit (pid);
		//
		testMultiple();
	//	testError();
	//	halt();
		exit(9990);
}
