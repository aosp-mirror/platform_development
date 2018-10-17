#include <abstract_class.h>
#include <additional_odr.h>

SuperSpeaker *SuperSpeaker::CreateSuperSpeaker(int id) {
  // :)
  return nullptr;
}

void SuperSpeaker::SpeakLouder() {
}

SuperSpeaker::Volume SuperSpeaker::SpeakLoud() {
  return SuperSpeaker::Volume::Loud;
}

void test_virtual_function_call(SuperSpeaker *super_speaker) {
  super_speaker->Speak();
}
