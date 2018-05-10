/*
 * open1.c
 *
 * Open an existing file.
 */

#include "syscall.h"

int main(int argc, char * argv[]) {
  int fd = open("../oldfile.c");
  return 0;
}
