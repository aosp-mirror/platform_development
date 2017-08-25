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
#else
#define CLASS_MEMBERS \
    unsigned int speaker_uint_t; \
    float *speaker_float_star;
#endif

class LowVolumeSpeaker : public SuperSpeaker {
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
