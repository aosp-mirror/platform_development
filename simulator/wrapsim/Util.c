
#include "Common.h"

int wsAtomicAdd(int *var, int val)
{
    int cc;
    int ret;
    cc = pthread_mutex_lock(&gWrapSim.atomicLock);
    ret = *var;
    *var = *var + val;
    cc = pthread_mutex_unlock(&gWrapSim.atomicLock);
    return ret;
}
