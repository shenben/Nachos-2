/*
 * read1.c
 *
 * Read from stdin, one byte at a time.
 */

#include "syscall.h"
#include "stdio.h"

int main(int argc, char * argv[]) {
  char buf[50];

  char c;
  while ( (c = getchar()) != 'q' ) {
    int r = read(0, buf, 1);
    if ( r != 1 ) {
      printf("failed to read character\n");
      exit(-1);
    }
  }
  printf("buf[0]: %c\n", buf[0]);
  return 0;
}