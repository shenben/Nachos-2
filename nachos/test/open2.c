/*
 * open2.c
 *
 * Try to open file that doesn't exist. 
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int fd = open("nefile.c");
  close(fd);
  return 0;
}
