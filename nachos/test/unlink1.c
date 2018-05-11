/*
 * unlink1.c
 *
 * Unlink existing file. 
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int ret = unlink("newfile.c");
  printf("retval: %d", ret);
  return 0;
}
