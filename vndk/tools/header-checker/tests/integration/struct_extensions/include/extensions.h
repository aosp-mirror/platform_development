struct Struct1 {
 public:
  short offset_0;
  short offset_16;
  int offset_32;
  int offset_64;
};

struct Struct2 {
 public:
  union Nested {
    int nested_member;
    int added_member[2];
  } member;
};

struct Vtable1 {
  int member_1;
  int added_member_1;

  virtual ~Vtable1();
  virtual void function_1() = 0;
  virtual void added_function_1() = 0;
};

struct Vtable2 {
  int member_2;
  int added_member_2;

  virtual void function_2();
  virtual void added_function_2() = 0;
  virtual ~Vtable2();
};

struct Vtable3 : virtual public Vtable1, virtual public Vtable2 {
  int member_3;
  int added_member_3;

  virtual ~Vtable3();
  virtual void function_3();
  virtual void added_function_3();
};

Vtable3 &PassByReference(Struct1 &, Struct2 &);
