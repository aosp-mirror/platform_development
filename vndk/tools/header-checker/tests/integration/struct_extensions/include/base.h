struct Struct1 {
 protected:
  int member;
};

struct Struct2 {
 protected:
  union Nested {
    int nested_member;
  } member;
};

Struct1 &PassByReference(Struct1 &, Struct2 &);
