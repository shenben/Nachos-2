#include "stdio.h"
#include "syscall.h"

int main() {
 int fd[32];
  int i;
  char buffer[128];
  char * str = "ROSES ARE RED\n";
  for (i = 2; i < 30; i += 2) {
    fd[i] = creat("somefile.c");
    if ( fd[i] < 0 ) {
      printf("... failed (fd[i] = %d)\n", fd[i]);
    } else {
      printf("... passed\n");
    }
    close(fd[i]);
    int invalidRead = read(fd[i], buffer, 0);
    int invalidWrite = write(fd[i], str, 20);
    if ( (invalidRead != -1) || (invalidWrite != -1) ) {
      printf("... failed - read/write to closed file should fail\n");
    } else {
      printf("... passed r/w to closed file\n");
    }
    fd[i+1] = creat("somefile2.c");
    if ( fd[i+1] != fd[i] ) {
      printf("... failed (fd[i+1] = %d)\n", fd[i+1]);
    } else {
      printf("... passed\n");
    }
  }
  return 0;
}
