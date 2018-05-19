/*
 * create4.c
 *
 * Create max num files + 1.
 */

#include "stdio.h"
#include "syscall.h"

int main(int argc, char * argv[]) {
  int fd[17];
  int i;
  for (i = 2; i < 17; i++) {
    fd[i] = creat("maxcreat.c");
    if ( i < 16 ) {
      if ( fd[i] < 0 ) {
        printf("... failed (fd = %d)\n", fd[i]);
      } else {
        printf("... passed\n");
      }
    } else {
      if ( fd[i] >= 0 ) {
        printf("... failed (fd = %d)\n", fd[i]);
      } else {
        printf("... passed\n");
      }
    }
  }
  return 0;
}
