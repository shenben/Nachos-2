/*
 * execarg2.c
 *
 * Use passed args
 */

#include "syscall.h"
#include "stdio.h"

int main(int argc, char * argv[]) {
  char *prog = "pa.coff";
  int pid, i;
  char *args[2] = {
    "this should print first",
    "this should print next",
  };
  pid = exec(prog, 2, args);
	join( pid, &i);
  if (pid < 0) {
    printf("exec failed: r = %d\n", pid);
  } else {
    printf("exec succeeded: pid = %d\n", pid);
  }
  exit(pid);
}
