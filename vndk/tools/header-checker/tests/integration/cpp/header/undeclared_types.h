#ifndef UNDECLARED_TYPES_H_
#define UNDECLARED_TYPES_H_

using ::namespace_a::A;
typedef const namespace_b::B *B;
using C = namespace_c::C[];

extern A a;
extern namespace_b::template_b<B> b;
extern const decltype(b) c;

inline A &inline_function(template_c<template_d<C>> d) {
  LocalVar e;
  return FunctionCall(d, e);
}

class InvalidClass {
  A member;

  D member_function(E);
  virtual void virtual_function(float);
};

#define DECLARE_VARIABLE extern TemplateInMacro<F> *template_in_macro
DECLARE_VARIABLE;

#endif  // UNDECLARED_TYPES_H_
