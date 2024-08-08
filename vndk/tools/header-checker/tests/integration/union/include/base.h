union ChangeType {
  char member_1;
  char member_2;
  int member_3;
};

union Rename {
  int member_1;
  char member_2;
};

union Swap {
  int member_1;
  char member_2;
};

struct ChangeTypeInStruct {
  int member_1;
  char member_2[0];
  char member_3[0];
  int member_4[0];
};

union ReorderAnonymousType {
  struct {
    int member_1;
  } member_1;
  struct {
    int member_2;
  };
};

extern "C" {
void function(ChangeType, Rename, Swap, ChangeTypeInStruct,
              ReorderAnonymousType);
}
