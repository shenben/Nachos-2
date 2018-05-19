/*
 * open1.c
 *
 * Open an existing file.
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  char * n1 = "oldfile.c";
  char * n2 = "oldfile.c";
  int fd1 = open(n1);
  int fd2 = open(n2);
  close(fd1);
  close(fd2);
  return 0;
}
