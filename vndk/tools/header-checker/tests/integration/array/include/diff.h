struct Struct {
  int array[];
};

extern "C" {
void StructMember(Struct &);
void Pointer(int *);
void DoublePointer(int *[]);
void PointerToArray(int (*)[10]);
void PointerTo2DArray(int (*)[10][10]);
void Reference(int (&)[][11]);
void Element(short (*)[2]);
}
