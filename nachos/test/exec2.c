/*
 * exec2.c
 *
 * Spawn two files.
 */

#include "syscall.h"
#include "stdio.h"

int main(int argc, char *argv[]) {
  char *p1 = "exit1.coff";
  char *p2 = "write1.coff";
  int pid1;
  int pid2;
  pid1 = exec(p1, 0, 0);
  pid2 = exec(p2, 0, 0);

  exit(0);
}
