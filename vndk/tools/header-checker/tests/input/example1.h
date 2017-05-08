#ifndef EXAMPLE1_H_
#define EXAMPLE1_H_

#include "example2.h"

#if defined(__cplusplus)
extern "C" {
#endif

struct Hello {
  int foo;
  int bar;
};

#if defined(__cplusplus)
}  // extern "C"
#endif
using namespace test2;
using namespace test3;
typedef float float_type;
typedef const float_type cfloat_type;
struct CPPHello : private HelloAgain, public ByeAgain<float_type> {
  const int cpp_foo;
  cfloat_type cpp_bar;
  virtual int again() { return 0; }
  CPPHello() : cpp_foo(20), cpp_bar(1.234) { }
};

template<typename T>
struct StackNode {
public:
  T value_;
  StackNode<T>* next_;

public:
  StackNode(T t, StackNode* next = nullptr)
    : value_(static_cast<T&&>(t)),
      next_(next) { }
};

template<typename T>
class Stack {
private:
  StackNode<T>* head_;

public:
  Stack() : head_(nullptr) { }

  void push(T t) {
    head_ = new StackNode<T>(static_cast<T&&>(t), head_);
  }

  T pop() {
    StackNode<T>* cur = head_;
    head_ = cur->next_;
    T res = static_cast<T&&>(cur->value_);
    delete cur;
    return res;
  }
};

// Replicated from libsysutils.
template<typename T>
class List
{
protected:
    /*
     * One element in the list.
     */
    class _Node {
    public:
        explicit _Node(const T& val) : mVal(val) {}
        ~_Node() {}
        inline T& getRef() { return mVal; }
        inline const T& getRef() const { return mVal; }
    private:
        friend class List;
        friend class _ListIterator;
        T           mVal;
        _Node*      mpPrev;
        _Node*      mpNext;
    };
    _Node *middle;
};

typedef List<float> float_list;
float_list float_list_test;


typedef List<int> int_list;
int_list int_list_test;

#endif  // EXAMPLE1_H_
