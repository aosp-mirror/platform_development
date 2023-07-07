struct Parameter {
  int member;
};

struct Return {
  int member;
  int extra_member;
};

void PassByValue(Parameter);
Return ReturnByValue();
