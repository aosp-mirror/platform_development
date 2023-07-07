struct Struct1 {
 protected:
  short offset_0;
  int offset_32;
};

struct Struct2 {
 protected:
  union Nested {
    int nested_member;
  } member;
};

struct Vtable1 {
  int member_1;

  virtual ~Vtable1();
  virtual void function_1() = 0;
};

struct Vtable2 {
  int member_2;

  virtual void function_2() = 0;
};

struct Vtable3 : virtual public Vtable1, virtual public Vtable2 {
  int member_3;

  virtual ~Vtable3();
  virtual void function_3();
};

Vtable3 &PassByReference(Struct1 &, Struct2 &);
