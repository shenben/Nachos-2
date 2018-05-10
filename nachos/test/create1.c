/*
 * create1.c
 *
 * Create a previously non-existant file.
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  char * name = "../newfile.c";
  int fd = creat(name);
  return 0;
}
