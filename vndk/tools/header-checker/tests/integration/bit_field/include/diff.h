struct Struct {
  struct {
    char unnamed_struct;
  };
  char : 2;
  char a : 1;
  char : 0;
  char b : 2;
};

extern "C" {
void function(Struct*);
}
