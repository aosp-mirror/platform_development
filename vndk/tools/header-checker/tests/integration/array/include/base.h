struct Struct {
  int array[0];
};

extern "C" {
void StructMember(Struct &);
void Pointer(int[]);
void DoublePointer(int **);
void PointerToArray(int (*)[]);
void PointerTo2DArray(int (*)[][10]);
void Reference(int (&)[][1]);
void Element(int (*)[2]);
}
