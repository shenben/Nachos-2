/*
 * execarg1.c
 *
 * Test how handleExec handles args.
 */

#include "syscall.h"
#include "stdio.h"

int main (int argc, char * argv[]) {
  int r;
  int childargc = 5;
  char * childargv[5] = {
      "exit1.coff",
      "roses are red",
      "violets are blue",
      "I love Nachos",
      "and so do you",
  };

  printf("childargc: %d\n", childargc);
  printf("childargv: %d (0x%x)\n\n", childargv, childargv);

  printf("&childargv[0]: %d (0x%x)\n\n", &childargv[0], &childargv[0]);
  printf("&childargv[1]: %d (0x%x)\n\n", &childargv[1], &childargv[1]);
  printf("&childargv[2]: %d (0x%x)\n\n", &childargv[2], &childargv[2]);
  printf("&childargv[3]: %d (0x%x)\n\n", &childargv[3], &childargv[3]);
  printf("&childargv[4]: %d (0x%x)\n\n", &childargv[4], &childargv[4]);
  printf("childargv[0]: %d (0x%x)\n\n", childargv[0], childargv[0]);
  printf("childargv[1]: %d (0x%x)\n\n", childargv[1], childargv[1]);
  printf("childargv[2]: %d (0x%x)\n\n", childargv[2], childargv[2]);
  printf("childargv[3]: %d (0x%x)\n\n", childargv[3], childargv[3]);
  printf("childargv[4]: %d (0x%x)\n\n", childargv[4], childargv[4]);
  printf("childargv[0]: %s (0x%x)\n\n", childargv[0], childargv[0]);

  r = exec(childargv[0], childargc, childargv);
  if ( r < 0 ) {
    printf("exec return error: %d\n", r);
  } else {
    printf("exec succeeded, child pid: %d\n", r);
  }

  exit(r);
}
