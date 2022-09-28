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

Struct1 &PassByReference(Struct1 &, Struct2 &);
