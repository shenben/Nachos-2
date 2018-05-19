/*
 * except1.c
 *
 * Causes page fault exception. 
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int *ptr = (int *) 0xBADFFFFF;
  return *ptr;
}
