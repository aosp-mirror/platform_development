class Alpha {
public:
    virtual void getData(int *src, int *dst, int data);
    virtual ~Alpha() {};
private:
    int mPdata = 0;
};

class Beta : public Alpha {
public:
    Beta(int data) : mCdata(data) {}
    virtual void getData(int *src, int *dst, int data);
    virtual ~Beta() {};
private:
    int mCdata = 1;
};

class Gamma : public Beta {
public:
    Gamma(int data) : mGCdata(data), Beta(data) {}
    virtual void getData(int *src, int *dst, int data);
    virtual ~Gamma() {};
private:
    int mGCdata = 2;
};

void Alpha::getData(int *src, int *dst, int data) {}

void Beta::getData(int *src, int *dst, int data) {}

void Gamma::getData(int *src, int *dst, int data) {}

