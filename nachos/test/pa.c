/*
 * pa.c
 *
 * Used for execarg2.c.
 */

#include "syscall.h"
#include "stdio.h"

int main(int argc, char * argv[]) {
  if (strncmp(argv[0], "hello", 6) != 0 || strncmp(argv[1], "world", 6) != 0) {
    printf("\tPA FAILED\n");
    exit(-1);
  } else {
    printf("\tPA argv[0] = %s\n", argv[0]);
    printf("\tPA argv[1] = %s\n", argv[1]);
    exit(0);
  }
}
