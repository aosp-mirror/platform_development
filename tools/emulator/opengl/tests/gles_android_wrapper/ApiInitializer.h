#ifndef _API_INITIALIZER_H_
#define _API_INITIALIZER_H_
#include <stdlib.h>
#include <dlfcn.h>

class ApiInitializer {
public:
    ApiInitializer(void *dso) :
        m_dso(dso) {
    }
    static void *s_getProc(const char *name, void *userData) {
        ApiInitializer *self = (ApiInitializer *)userData;
        return self->getProc(name);
    }
private:
    void *m_dso;
    void *getProc(const char *name) {
        void *symbol = NULL;
        if (m_dso) {
            symbol = dlsym(m_dso, name);
        }
        return symbol;
    }
};

#endif
