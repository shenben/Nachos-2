/*
 * create2.c
 *
 * Create a previously non-existant file.
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  char * name1 = "newfile1.c";
  char * name2 = "newfile2.c";
  int fd1 = creat(name1);
  int fd2 = creat(name2);
  close(fd1);
  close(fd2);
  return 0;
}
