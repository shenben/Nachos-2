/*
 * write3.c
 *
 * Check invalid buffer cases.
 */

#include "stdio.h"
#include "stdlib.h"
#include "syscall.h"

int
do_creat (char *fname) {
  int fd;

  printf ("creating %s...\n", fname);
  fd = creat (fname);
  if (fd >= 0) {
    printf ("...passed (fd = %d)\n", fd);
  } else {
    printf ("...failed (%d)\n", fd);
    exit (-1001);
  }
  return fd;
}

int main() {
  char buffer[128], *file, *ptr;
  int buflen = 128;
  int fd, r, len, i;

  file = "bad.out";
  fd = do_creat (file);

  printf ("writing with an invalid buffer (should not crash, only return an error)...\n");
  r = write (fd, (char *) 0xBADFFF, 10);
  if (r < 0) {
    printf ("...passed (r = %d)\n", r);
  } else {
    printf ("...failed (r = %d)\n", r);
    exit (-6000);
  }
}
