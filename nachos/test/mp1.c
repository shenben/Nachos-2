/*
 * mp1.c
 *
 * Tests resource allocation for 2 processes. 
 */

#include "stdio.h"
#include "syscall.h"
#include "stdlib.h"

int main() {
  char * args[0];
  int pid;
  pid = exec("create1.coff", 0, args);
  exit (pid);
}
