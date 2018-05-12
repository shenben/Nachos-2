/* read.c
 * test handleRead and testOpen at the same time
 *
 */

#include "syscall.h"
#include "stdio.h"


int testReadOneLine() {
  char buf[30];
	read( open("thisIsNotDrill"), buf, 15);
  
}

int main(){
  testReadOneLine();
	halt();
}
