/*
 * Copyright 2012, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <portability.h>
#include <pthread.h>
#include <time.h>
#include <signal.h>
#include <signal_portable.h>
#include <errno.h>
#include <errno_portable.h>

#define PORTABLE_TAG "pthread_portable"
#include <log_portable.h>

/*
 * Macros for STRIP_PARENS() which is used below in PTHREAD_WRAPPER(); cpp magic from:
 *      http://boost.2283326.n4.nabble.com/preprocessor-removing-parentheses-td2591973.html
 */
#define CAT(x, y) CAT_I(x, y)
#define CAT_I(x, y) x ## y

#define APPLY(macro, args) APPLY_I(macro, args)
#define APPLY_I(macro, args) macro args

#define STRIP_PARENS(x) EVAL((STRIP_PARENS_I x), x)
#define STRIP_PARENS_I(...) 1,1

#define EVAL(test, x) EVAL_I(test, x)
#define EVAL_I(test, x) MAYBE_STRIP_PARENS(TEST_ARITY test, x)

#define TEST_ARITY(...) APPLY(TEST_ARITY_I, (__VA_ARGS__, 2, 1))
#define TEST_ARITY_I(a,b,c,...) c

#define MAYBE_STRIP_PARENS(cond, x) MAYBE_STRIP_PARENS_I(cond, x)
#define MAYBE_STRIP_PARENS_I(cond, x) CAT(MAYBE_STRIP_PARENS_, cond)(x)

#define MAYBE_STRIP_PARENS_1(x) x
#define MAYBE_STRIP_PARENS_2(x) APPLY(MAYBE_STRIP_PARENS_2_I, x)
#define MAYBE_STRIP_PARENS_2_I(...) __VA_ARGS__

/*
 * Call pthread function and convert return value (a native errno) to portable error number.
 */
#define PTHREAD_WRAPPER(fn, DECLARGS, CALLARGS, fmt)            \
    int WRAP(fn) DECLARGS                                       \
    {                                                           \
        int rv, portable_rv;                                    \
                                                                \
        ALOGV(" ");                                             \
        ALOGV("%s" fmt, __func__, STRIP_PARENS(CALLARGS));      \
        rv = REAL(fn) CALLARGS;                                 \
        portable_rv = errno_ntop(rv);                           \
        ALOGV("%s: return(portable_rv:%d); rv:%d;", __func__,   \
                          portable_rv,     rv);                 \
        return portable_rv;                                     \
    }

PTHREAD_WRAPPER(pthread_attr_init, (pthread_attr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_attr_destroy, (pthread_attr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_attr_setdetachstate, (pthread_attr_t *attr, int state), (attr, state),
                "(attr:%p, state:%d)");

PTHREAD_WRAPPER(pthread_attr_getdetachstate, (pthread_attr_t const *attr, int *state),
                (attr, state), "(attr:%p, state:%p)");

PTHREAD_WRAPPER(pthread_attr_setschedpolicy, (pthread_attr_t *attr, int policy), (attr, policy),
                "(attr:%p, policy:%d)");

PTHREAD_WRAPPER(pthread_attr_getschedpolicy, (pthread_attr_t const *attr, int *policy),
                (attr, policy), "(attr:%p, policy:%p)");

PTHREAD_WRAPPER(pthread_attr_setschedparam,
                (pthread_attr_t *attr, struct sched_param const *param), (attr, param),
                "(attr:%p, param:%p)");

PTHREAD_WRAPPER(pthread_attr_getschedparam,
                (pthread_attr_t const *attr, struct sched_param *param), (attr, param),
                "(attr:%p, param:%p)");

PTHREAD_WRAPPER(pthread_attr_setstacksize, (pthread_attr_t *attr, size_t stack_size),
                (attr, stack_size), "(attr:%p, stack_size:%d)");

PTHREAD_WRAPPER(pthread_attr_getstacksize, (pthread_attr_t const *attr, size_t *stack_size),
                (attr, stack_size), "(attr:%p, stack_size:%p)");

PTHREAD_WRAPPER(pthread_attr_setstack, (pthread_attr_t *attr, void *stackaddr, size_t stack_size),
                (attr, stackaddr, stack_size), "(attr:%p, stackaddr:%p, stack_size:%d)");

PTHREAD_WRAPPER(pthread_attr_getstack, (pthread_attr_t const *attr, void **stackaddr,
                size_t *stack_size), (attr, stackaddr, stack_size),
                "(attr:%p, stackaddr:%p stack_size:%p)");

PTHREAD_WRAPPER(pthread_attr_setguardsize, (pthread_attr_t *attr, size_t guard_size),
                (attr, guard_size), "(attr:%p, guard_size:%d)");

PTHREAD_WRAPPER(pthread_attr_getguardsize, (pthread_attr_t const *attr, size_t *guard_size),
                (attr, guard_size), "(attr:%p, guard_size:%p)");

PTHREAD_WRAPPER(pthread_attr_setscope, (pthread_attr_t *attr, int scope), (attr, scope),
                "(attr:%p, scope:%d)");

PTHREAD_WRAPPER(pthread_attr_getscope, (pthread_attr_t const *attr, int* scope), (attr, scope), "(attr:%p, scope:%p)");

PTHREAD_WRAPPER(pthread_getattr_np, (pthread_t thid, pthread_attr_t *attr), (thid, attr),
                "(thid:%lx, attr:%p)");

PTHREAD_WRAPPER(pthread_create, (pthread_t *thread, const pthread_attr_t *attr,
                void *(*start_routine) (void *), void *arg),
                (thread, attr, start_routine, arg),
                "(thread:%p attr:%p, start_routine:%p, arg:%p)");

// void pthread_exit(void * retval);
PTHREAD_WRAPPER(pthread_join, (pthread_t thid, void **ret_val), (thid, ret_val),
                "(thid:%lx, ret_val:%p)");

PTHREAD_WRAPPER(pthread_detach, (pthread_t thid), (thid), "(thid:%lx)");

// pthread_t pthread_self(void);
// int pthread_equal(pthread_t one, pthread_t two);

PTHREAD_WRAPPER(pthread_getschedparam, (pthread_t thid, int *policy, struct sched_param *param),
                (thid, policy, param), "(thid:%lx, policy:%p, param:%p)");

PTHREAD_WRAPPER(pthread_setschedparam, (pthread_t thid, int policy,
                struct sched_param const *param), (thid, policy, param),
                "(thid:%lx, policy:%d, param:%p)");

PTHREAD_WRAPPER(pthread_mutexattr_init, (pthread_mutexattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_mutexattr_destroy, (pthread_mutexattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_mutexattr_gettype, (const pthread_mutexattr_t *attr, int *type),
                (attr, type), "(attr:%p, type:%p)");

PTHREAD_WRAPPER(pthread_mutexattr_settype, (pthread_mutexattr_t *attr, int type), (attr, type),
                "(attr:%p, type:%d)");

PTHREAD_WRAPPER(pthread_mutexattr_setpshared, (pthread_mutexattr_t *attr, int pshared),
                (attr, pshared), "(attr:%p, pshared:%d)");

PTHREAD_WRAPPER(pthread_mutexattr_getpshared, (pthread_mutexattr_t *attr, int *pshared),
                (attr, pshared), "(attr:%p, pshared:%p)");

PTHREAD_WRAPPER(pthread_mutex_init, (pthread_mutex_t *mutex, const pthread_mutexattr_t *attr),
                (mutex, attr), "(mutex:%p, attr:%p)");

PTHREAD_WRAPPER(pthread_mutex_destroy, (pthread_mutex_t *mutex), (mutex), "(mutex:%p)");

PTHREAD_WRAPPER(pthread_mutex_lock, (pthread_mutex_t *mutex), (mutex), "(mutex:%p)");

PTHREAD_WRAPPER(pthread_mutex_unlock, (pthread_mutex_t *mutex), (mutex), "(mutex:%p)");

PTHREAD_WRAPPER(pthread_mutex_trylock, (pthread_mutex_t *mutex), (mutex), "(mutex:%p)");

#if 0 /* MISSING FROM BIONIC */
PTHREAD_WRAPPER(pthread_mutex_timedlock, (pthread_mutex_t *mutex, struct timespec *ts),
                (mutex, ts), "(mutex:%p, ts:%p)");
#endif /* MISSING */

PTHREAD_WRAPPER(pthread_condattr_init, (pthread_condattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_condattr_getpshared, (pthread_condattr_t *attr, int *pshared),
                (attr, pshared), "(attr:%p, pshared:%p)");

PTHREAD_WRAPPER(pthread_condattr_setpshared, (pthread_condattr_t* attr, int pshared),
                (attr, pshared), "(attr:%p, pshared:%d)");

PTHREAD_WRAPPER(pthread_condattr_destroy, (pthread_condattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_cond_init, (pthread_cond_t *cond, const pthread_condattr_t *attr),
                (cond, attr), "(cond:%p, attr:%p)");

PTHREAD_WRAPPER(pthread_cond_destroy, (pthread_cond_t *cond), (cond), "(cond:%p)");

PTHREAD_WRAPPER(pthread_cond_broadcast, (pthread_cond_t *cond), (cond), "(cond:%p)");

PTHREAD_WRAPPER(pthread_cond_signal, (pthread_cond_t *cond), (cond), "(cond:%p)");

PTHREAD_WRAPPER(pthread_cond_wait, (pthread_cond_t *cond, pthread_mutex_t *mutex),
                (cond, mutex), "(cond:%p, mutex:%p)");

PTHREAD_WRAPPER(pthread_cond_timedwait, (pthread_cond_t *cond, pthread_mutex_t *mutex,
                const struct timespec *abstime), (cond, mutex, abstime),
                "(cond:%p, mutex:%p, abstime:%p)");

PTHREAD_WRAPPER(pthread_cond_timedwait_monotonic_np, (pthread_cond_t *cond,
                pthread_mutex_t *mutex, const struct timespec *abstime),
                (cond, mutex, abstime), "(cond:%p, mutex:%p, abstime:%p)");

PTHREAD_WRAPPER(pthread_cond_timedwait_monotonic, (pthread_cond_t *cond, pthread_mutex_t
                *mutex, const struct timespec *abstime),
                (cond, mutex, abstime), "(cond:%p, mutex:%p, abstime:%p)");

PTHREAD_WRAPPER(pthread_cond_timedwait_relative_np, (pthread_cond_t *cond, pthread_mutex_t *mutex,
                const struct timespec *reltime), (cond, mutex, reltime),
                "(cond:%p, mutex:%p, reltime:%p)");

PTHREAD_WRAPPER(pthread_cond_timeout_np, (pthread_cond_t *cond, pthread_mutex_t *mutex,
                unsigned msecs), (cond, mutex, msecs), "(cond:%p, mutex:%p, msecs:%u)");

PTHREAD_WRAPPER(pthread_mutex_lock_timeout_np, (pthread_mutex_t *mutex, unsigned msecs),
                (mutex, msecs), "(mutex:%p, msecs:%u)");

PTHREAD_WRAPPER(pthread_rwlockattr_init, (pthread_rwlockattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_rwlockattr_destroy, (pthread_rwlockattr_t *attr), (attr), "(attr:%p)");

PTHREAD_WRAPPER(pthread_rwlockattr_setpshared, (pthread_rwlockattr_t *attr, int  pshared),
                (attr, pshared), "(attr:%p, pshared:%d)");

PTHREAD_WRAPPER(pthread_rwlockattr_getpshared, (pthread_rwlockattr_t *attr, int *pshared),
                (attr, pshared), "(attr:%p, pshared:%p)");

PTHREAD_WRAPPER(pthread_rwlock_init, (pthread_rwlock_t *rwlock, const pthread_rwlockattr_t *attr),
                (rwlock, attr), "(rwlock:%p, attr:%p)");

PTHREAD_WRAPPER(pthread_rwlock_destroy, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_rwlock_rdlock, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_rwlock_tryrdlock, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_rwlock_timedrdlock, (pthread_rwlock_t *rwlock,
                const struct timespec *abs_timeout),
                (rwlock, abs_timeout), "(rwlock:%p, abs_timeout:%p)");

PTHREAD_WRAPPER(pthread_rwlock_wrlock, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_rwlock_trywrlock, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_rwlock_timedwrlock, (pthread_rwlock_t *rwlock,
                const struct timespec *abs_timeout), (rwlock, abs_timeout),
                "(rwlock:%p, abs_timeout:%p)");

PTHREAD_WRAPPER(pthread_rwlock_unlock, (pthread_rwlock_t *rwlock), (rwlock), "(rwlock:%p)");

PTHREAD_WRAPPER(pthread_key_create, (pthread_key_t *key, void (*destructor_function)(void *)),
                (key, destructor_function), "(key:%p, destructor_function:%p)");

PTHREAD_WRAPPER(pthread_key_delete , (pthread_key_t key), (key), "(key:%x)");

PTHREAD_WRAPPER(pthread_setspecific, (pthread_key_t key, const void *value), (key, value),
                "(key:%x, value:%p)");

// void *pthread_getspecific(pthread_key_t key);

int WRAP(pthread_kill)(pthread_t thread, int portable_signum)
{
    char *portable_signame = map_portable_signum_to_name(portable_signum);
    int mips_signum;
    int portable_ret, ret;

    ALOGV("%s(thread:%lx, portable_signum:%d)", __func__, thread, portable_signum);

    mips_signum = signum_pton(portable_signum);

    if ((portable_signum != 0) && (mips_signum == 0)) {
        /* A signal MIPS doesn't support; all we can do is ignore it. */
        ret = 0;
    } else {
        ALOGV("%s: calling pthread_kill(thread:%lx, mips_signum:%d);", __func__,
                                        thread,     mips_signum);
        ret = REAL(pthread_kill)(thread, mips_signum);
    }
    portable_ret = errno_ntop(ret);

    ALOGV("%s: return portable_ret:%d; ret:%d;", __func__,
                      portable_ret,    ret);

    return portable_ret;
}

int WRAP(pthread_sigmask)(int portable_how, const sigset_portable_t *portable_sigset,
                             sigset_portable_t *portable_oldset)
{
    extern int REAL(pthread_sigmask)(int how, const sigset_t *set, sigset_t *oset);
    int portable_ret, ret;

    ALOGV(" ");
    ALOGV("%s(portable_how:%d portable_sigset:%p, portable_oldset:%p)", __func__,
              portable_how,   portable_sigset,    portable_oldset);

    ret = do_sigmask(portable_how, portable_sigset, portable_oldset, REAL(pthread_sigmask), NULL);

    portable_ret = errno_ntop(ret);

    ALOGV("%s: return portable_ret:%d; ret:%d;", __func__,
                      portable_ret,    ret);

    return portable_ret;
}

PTHREAD_WRAPPER(pthread_getcpuclockid, (pthread_t tid, clockid_t *clockid), (tid, clockid),
                "(tid:%lx, clockid:%p)");

PTHREAD_WRAPPER(pthread_once, (pthread_once_t *once_control, void (*init_routine)(void)),
                (once_control, init_routine), "(once_control:%p, init_routine:%p)");

PTHREAD_WRAPPER(pthread_setname_np, (pthread_t thid, const char *thname), (thid, thname),
                "(thid:%lx, thname:\"%s\")");
