#include "syscall.h"
#include "stdlib.h"
#include "stdio.h"

int main(){
  exec( "exec1.coff", 0, 0);
	exec( "exec1.coff", 0,0 );
	halt();
}
