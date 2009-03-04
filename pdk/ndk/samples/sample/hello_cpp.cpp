#include <stdio.h>
#include "hello_cpp.h"

Hello::Hello()
{
}

Hello::~Hello()
{
}

void Hello::printMessage(char* msg)
{
  printf("C++ example printing message: %s", msg);
}

int main(void)
{
  Hello hello_obj;
  hello_obj.printMessage("Hello world!\n");
  return 0;
}
