#if GOLDEN_RETURN_TYPE_DIFF
#define LISTEN_RETURN_TYPE \
    int
#define LISTEN_RETURN_STATEMENT \
    return 0;
#else
#define LISTEN_RETURN_TYPE \
    void
#define LISTEN_RETURN_STATEMENT \
    return;
#endif

#if GOLDEN_VTABLE_DIFF
#define VIRTUAL_FUNCTIONS \
    virtual LISTEN_RETURN_TYPE Listen() = 0; \
    virtual void Speak() = 0; \
    static void SpeakLouder();
#else
#define VIRTUAL_FUNCTIONS \
    virtual void Speak() = 0; \
    virtual LISTEN_RETURN_TYPE Listen() = 0; \
    void SpeakLouder();
#endif

#if GOLDEN_ENUM_EXTENSION
#define LOUD_SUPERLATIVES \
    Loudest = 3, \
    Lower = 0,\
    LouderThanLoudest = 5
#elif GOLDEN_ENUM_DIFF
#define LOUD_SUPERLATIVES \
    Loudest = -1,
#else
#define LOUD_SUPERLATIVES \
    Loudest = 3, \
    Lower = 0
#endif

class SuperSpeaker {
  enum Volume {
    Loud = 1,
    Louder = 2,
    LOUD_SUPERLATIVES
  };
#if GOLDEN_CHANGE_FUNCTION_ACCESS
 private:
#else
 public:
#endif
  static SuperSpeaker *CreateSuperSpeaker(int id);
 public:
  VIRTUAL_FUNCTIONS
  Volume SpeakLoud();
  void SpeakLoudest() {}
  virtual ~SuperSpeaker() {}
 private:
#if GOLDEN_CHANGE_MEMBER_NAME_SAME_OFFSET
  int mSpeakderId_;
#else
  int mSpeakderId;
#endif

#if GOLDEN_FUNCTION_POINTER
#if GOLDEN_FUNCTION_POINTER_ADD_PARAM
  SuperSpeaker * (*speaker_fp)(int, char, int);
#else
  SuperSpeaker * (*speaker_fp)(int, char);
#endif
#endif

#if GOLDEN_WITH_INTERNAL_STRUCT
#ifdef GOLDEN_WITH_PUBLIC_INTERNAL_STRUCT
 public:
#else
 private:
#endif
  struct InternalStruct {
    int internal;
  };
 private:
  InternalStruct a;
#endif  // GOLDEN_WITH_INTERNAL_STRUCT
};
