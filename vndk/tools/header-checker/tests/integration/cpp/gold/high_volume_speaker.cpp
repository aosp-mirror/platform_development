#include <high_volume_speaker.h>

void HighVolumeSpeaker::Speak() { }

HighVolumeSpeaker *HighVolumeSpeaker::BadPractice(BP_PARAMETER_TYPE id) {
  return nullptr;
}

LISTEN_RETURN_TYPE HighVolumeSpeaker::Listen() { LISTEN_RETURN_STATEMENT }

#if GOLDEN_ADD_FUNCTION
int HighVolumeSpeaker::AddedFunction() {
  return 0;
}
#endif

#if GOLDEN_ADD_GLOBVAR
int HighVolumeSpeaker::global_unprotected_id = 0;
#endif

