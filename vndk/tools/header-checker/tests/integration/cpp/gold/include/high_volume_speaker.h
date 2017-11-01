#include "abstract_class.h"

#if GOLDEN_PARAMETER_TYPE_DIFF
#define BP_PARAMETER_TYPE int
#else
#define BP_PARAMETER_TYPE float
#endif

#if GOLDEN_EQUAL_BUILTIN_POINTER_TYPE_DIFF
#define EQ_POINTER_RETURN_TYPE unsigned char *
#else
#define EQ_POINTER_RETURN_TYPE unsigned int *
#endif

class HighVolumeSpeaker : public SuperSpeaker {
 public:
  virtual void Speak() override;
  virtual LISTEN_RETURN_TYPE Listen() override;
  HighVolumeSpeaker *BadPractice(BP_PARAMETER_TYPE id);
#if GOLDEN_ADD_FUNCTION
  int AddedFunction();
#endif
#if GOLDEN_ADD_GLOBVAR
  static int global_unprotected_id;
#endif
};
