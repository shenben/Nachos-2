/*
 * open1.c
 *
 * Open an existing file.
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int fd1 = open("oldfile.c");
  int fd2 = open("oldfile.c");
  close(fd1);
  close(fd2);
  return 0;
}
