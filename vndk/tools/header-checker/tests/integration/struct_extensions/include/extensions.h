struct Struct1 {
 public:
  int member;
  int added_member;
};

struct Struct2 {
 public:
  union Nested {
    int nested_member;
    int added_member[2];
  } member;
};

Struct1 &PassByReference(Struct1 &, Struct2 &);
