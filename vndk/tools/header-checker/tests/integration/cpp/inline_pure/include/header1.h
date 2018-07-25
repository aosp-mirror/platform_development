class PureVirtualBase {
  virtual void foo_pure() = 0;
  virtual void foo_pure2() = 0;

  virtual void foo_inline() {}
  virtual void foo_not_inline();
};

class DerivedBar : public PureVirtualBase {
  virtual void foo_pure() override;

  virtual void foo_inline() override;
  virtual void foo_not_inline() override {}

};

