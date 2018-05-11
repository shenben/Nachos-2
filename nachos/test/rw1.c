/*
 * rw1.c
 *
 * Read from stdin and write to stdout.
 */

#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char * argv[]) {
  char buffer[80];
  char prompt[4];
  int i, n;

  prompt[0] = '-';
  prompt[1] = '>';
  prompt[2] = ' ';
  prompt[3] = '\0';

  while(1) {
    puts(prompt);
    
    i = 0;
    do {
      buffer[i] = getchar();
      //putchar(buffer[i]);
    } while(buffer[i++] != '\n');
    buffer[i] = '\0';

    if (buffer[0] == '.' &&
      buffer[1] == '\n') {
      return 0;
    }
    puts(buffer);
  }
}
