class PureVirtualBase {
 public:
  virtual ~PureVirtualBase() = 0;
  virtual void foo_pure() = 0;
  virtual void foo_virtual() {}
};

class DerivedBar : public PureVirtualBase {
 public:
  virtual ~DerivedBar() {}
  virtual void foo_pure() override {}
  virtual void foo_virtual() override = 0;
};

PureVirtualBase::~PureVirtualBase() {}
