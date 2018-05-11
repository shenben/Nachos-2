/*
 * create3.c
 *
 * Create new file (again). 
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  char * name = "reallynew.c";
  int fd = creat(name);
  printf("FD: %d\n", fd);
  close(fd);
  return 0;
}
