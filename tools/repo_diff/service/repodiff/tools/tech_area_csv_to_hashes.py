import csv
import hashlib
import sys


DISPLAY_TO_CANONICAL_TECH_AREA = {
    "bluetooth": "Bluetooth",
    "audio": "Audio",
    "surfaceflinger": "SurfaceFlinger",
    "vold": "Vold",
    "telephony": "Telephony",
    "wifi": "WiFi",
    "sepolicy": "SEPolicy",
    "contacts": "Contacts",
    "clock": "Clock",
    "networking": "Networking",
    "settings": "Settings",
    "build": "Build",
    "camera": "Camera",
    "video": "Video",
    "email": "EMail",
    "systemui": "SystemUI",
    "nfc": "NFC",
    "music app": "MusicApp",
    "unknown": "Unknown",
}


def print_to_golang_code(fname):
  with open(fname, "rb") as csv_file:
    reader = csv.reader(csv_file, skipinitialspace=True, delimiter=",", quoting=csv.QUOTE_NONE)
    for row in list(reader)[1:]:
      email_address, tech_area = row
      print "\"%s\":" % hashlib.sha256(email_address).hexdigest(),
      print "%s," % DISPLAY_TO_CANONICAL_TECH_AREA[tech_area.lower()]


if __name__ == "__main__":
  csv_file = sys.argv[1]
  print_to_golang_code(csv_file)
