#include <stddef.h>
#include "libtest1.h"

class Foo
{
public:
    Foo() { mAddress = NULL; }
    void setAddress(int *px);
    ~Foo();
private:
    int *mAddress;
};

void Foo::setAddress(int *px)
{
    mAddress = px;
    *mAddress = 1;
}

Foo::~Foo()
{
    if (mAddress)
        *mAddress = 2;
}

static Foo foo;

extern "C" void test1_set(int *px)
{
    foo.setAddress(px);
}
