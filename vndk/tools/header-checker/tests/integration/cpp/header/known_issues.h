#ifndef KNOWN_ISSUES_H_
#define KNOWN_ISSUES_H_

// header-abi-dumper is unable to output the following types correctly.

// template<int I> struct NonTypeTemplate;
extern NonTypeTemplate<1> non_type_template;

// namespace namespace1{
// template<typename T> class UsingTemplate;
// }
using namespace1::UsingTemplate;
extern UsingTemplate<float> *using_template;

// #define STDCALL __stdcall
STDCALL return_type function_with_calling_convention();

// class ClassInNameSpace;
template <typename T> class ClassTemplate;
extern ClassTemplate<::ClassInNameSpace> template_arg_in_namespace;

#endif  // KNOWN_ISSUES_H_
