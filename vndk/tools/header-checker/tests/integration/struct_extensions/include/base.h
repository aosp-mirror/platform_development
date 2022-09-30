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

Struct1 &PassByReference(Struct1 &, Struct2 &);
