#include "abstract_class.h"

#if GOLDEN_MEMBER_DIFF
#define CLASS_MEMBERS \
    long long speaker_long_long; \
    long long * speaker_long_long_star;
#elif GOLDEN_MEMBER_FAKE_DIFF
#define CLASS_MEMBERS \
    char32_t  speaker_uint_t;\
    float *speaker_float_star;
#elif GOLDEN_MEMBER_INTEGRAL_TYPE_DIFF
#define CLASS_MEMBERS \
    float speaker_float;\
    float *speaker_float_star;
#elif GOLDEN_MEMBER_CV_DIFF
#define CLASS_MEMBERS \
    unsigned int speaker_uint_t;\
    const float *const_speaker_float_star;
#elif GOLDEN_CHANGE_MEMBER_NAME_SAME_OFFSET
#define CLASS_MEMBERS \
    unsigned int speaker_uint_t_;\
    float *speaker_float_star_;
#else
#define CLASS_MEMBERS \
    unsigned int speaker_uint_t; \
    float *speaker_float_star;
#endif

#if GOLDEN_CHANGE_INHERITANCE_TYPE
class LowVolumeSpeaker : public virtual SuperSpeaker {
#else
class LowVolumeSpeaker : public SuperSpeaker {
#endif
 public:
  virtual void Speak() override;
  virtual LISTEN_RETURN_TYPE Listen() override;
#if GOLDEN_CHANGE_MEMBER_ACCESS
 private:
#else
 public:
#endif
  CLASS_MEMBERS
};
