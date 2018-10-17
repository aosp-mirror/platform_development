#include <low_volume_speaker.h>

void LowVolumeSpeaker::Speak() {}

LISTEN_RETURN_TYPE LowVolumeSpeaker::Listen() { LISTEN_RETURN_STATEMENT }

#ifdef ADD_UNEXPORTED_ELF_SYMBOL
void UnexportedSymbol(int *a) {
  if (a) {
    a++;
  }
  a--;
}
#endif
