union ChangeType {
  char member_1;
  int member_2;
  int member_3;
};

union Rename {
  int rename_1;
  char rename_2;
};

union Swap {
  char member_2;
  int rename_1;
};

struct ChangeTypeInStruct {
  int member_1;
  char member_2[0];
  int member_3[0];
  int member_4[0];
};

extern "C" {
void function(ChangeType, Rename, Swap, ChangeTypeInStruct);
}
