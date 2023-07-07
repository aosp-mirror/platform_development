struct Parameter {
  int member;
  int extra_member;
};

struct Return {
  int member;
};

void PassByValue(Parameter);
Return ReturnByValue();
