#ifndef EXAMPLE1_H_
#define EXAMPLE1_H_

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

#endif  // EXAMPLE1_H_
