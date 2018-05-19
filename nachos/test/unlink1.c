/*
 * unlink1.c
 *
 * Unlink existing file. 
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int ret = unlink("newfile.c");
  printf("retval: %d\n", ret);
  return 0;
}
