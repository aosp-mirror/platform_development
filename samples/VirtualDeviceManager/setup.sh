#!/bin/bash

# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

function die() {
  echo "${@}" >&2
  exit 1;
}

function run_cmd_or_die() {
  "${@}" > /dev/null || die "Command failed: ${*}"
}

function select_device() {
  while :; do
    read -r -p "Select a device to install the ${1} app (0-${DEVICE_COUNT}): " INDEX
    [[ "${INDEX}" =~ ^[0-9]+$ ]] && ((INDEX >= 0 && INDEX <= DEVICE_COUNT)) && return "${INDEX}"
  done
}

function install_app() {
  if ! adb -s "${1}" install -r -d -g "${2}" > /dev/null 2>&1; then
    adb -s "${1}" uninstall "com.example.android.vdmdemo.${3}" > /dev/null 2>&1
    run_cmd_or_die adb -s "${1}" install -r -d -g "${2}"
  fi
}

[[ -f build/make/envsetup.sh ]] || die "Run this script from the root of the tree."

DEVICE_COUNT=$(adb devices -l | tail -n +2 | head -n -1 | wc -l)
((DEVICE_COUNT > 0)) || die "No devices found"

DEVICE_SERIALS=( $(adb devices -l | tail -n +2 | head -n -1 | awk '{ print $1 }') )
DEVICE_NAMES=( $(adb devices -l | tail -n +2 | head -n -1 | awk '{ print $4 }') )
HOST_SERIAL=""
CLIENT_SERIAL=""

echo
echo "Available devices:"
for i in "${!DEVICE_SERIALS[@]}"; do
  echo -e "${i}: ${DEVICE_SERIALS[${i}]}\t${DEVICE_NAMES[${i}]}"
done
echo "${DEVICE_COUNT}: Do not install this app"
echo
select_device "VDM Host"
HOST_INDEX=$?
select_device "VDM Client"
CLIENT_INDEX=$?
echo

if ((HOST_INDEX == DEVICE_COUNT)); then
  echo "Not installing host app."
else
  HOST_SERIAL=${DEVICE_SERIALS[HOST_INDEX]}
  HOST_NAME="${HOST_SERIAL} ${DEVICE_NAMES[HOST_INDEX]}"
  echo "Using ${HOST_NAME} as host device."
fi
if ((CLIENT_INDEX == DEVICE_COUNT)); then
  echo "Not installing client app."
else
  CLIENT_SERIAL=${DEVICE_SERIALS[CLIENT_INDEX]}
  CLIENT_NAME="${CLIENT_SERIAL} ${DEVICE_NAMES[CLIENT_INDEX]}"
  echo "Using ${CLIENT_NAME} as client device."
fi

APKS_TO_BUILD=""
[[ -n "${HOST_SERIAL}" ]] && APKS_TO_BUILD="${APKS_TO_BUILD} VdmHost VdmDemos"
[[ -n "${CLIENT_SERIAL}" ]] && APKS_TO_BUILD="${APKS_TO_BUILD} VdmClient"
[[ -n "${APKS_TO_BUILD}" ]] || exit 0
echo
echo "Building APKs:${APKS_TO_BUILD}..."
echo

source ./build/envsetup.sh || die "Failed to set up environment"
[[ -n "${ANDROID_BUILD_TOP}" ]] || run_cmd_or_die tapas "${APKS_TO_BUILD}"
UNBUNDLED_BUILD_SDKS_FROM_SOURCE=true m -j "${APKS_TO_BUILD}" || die "Build failed"

if [[ -n "${CLIENT_SERIAL}" ]]; then
  echo
  echo "Installing VdmClient.apk to ${CLIENT_NAME}..."
  install_app "${CLIENT_SERIAL}" "${OUT}/system/app/VdmClient/VdmClient.apk" client
fi

if [[ -n "${HOST_SERIAL}" ]]; then
  echo
  echo "Installing VdmDemos.apk to ${HOST_NAME}..."
  install_app "${HOST_SERIAL}" "${OUT}/system/app/VdmDemos/VdmDemos.apk" demos
  echo

  readonly PERM_BASENAME=com.example.android.vdmdemo.host.xml
  readonly PERM_SRC="${ANDROID_BUILD_TOP}/development/samples/VirtualDeviceManager/host/${PERM_BASENAME}"
  readonly PERM_DST="/system/etc/permissions/${PERM_BASENAME}"
  readonly HOST_APK_DIR=/system/priv-app/VdmHost

  echo "Preparing ${HOST_NAME} for privileged VdmHost installation..."
  if adb -s "${HOST_SERIAL}" shell ls "${HOST_APK_DIR}/VdmHost.apk" > /dev/null 2>&1 \
      && adb -s "${HOST_SERIAL}" pull "${PERM_DST}" "/tmp/${PERM_BASENAME}" > /dev/null 2>&1 \
      && cmp --silent "/tmp/${PERM_BASENAME}" "${PERM_SRC}" \
      && (adb -s "${HOST_SERIAL}" uninstall com.example.android.vdmdemo.host > /dev/null 2>&1 || true) \
      && adb -s "${HOST_SERIAL}" install -r -d -g "${OUT}/${HOST_APK_DIR}/VdmHost.apk" > /dev/null 2>&1; then
    echo "A privileged installation already found, installed VdmHost.apk to ${HOST_NAME}"
  else
    run_cmd_or_die adb -s "${HOST_SERIAL}" root
    run_cmd_or_die adb -s "${HOST_SERIAL}" remount -R
    run_cmd_or_die adb -s "${HOST_SERIAL}" wait-for-device
    sleep 3  # Even after wait-for-device returns, the device may not be ready so give it some time.
    run_cmd_or_die adb -s "${HOST_SERIAL}" root
    run_cmd_or_die adb -s "${HOST_SERIAL}" remount
    echo "Installing VdmHost.apk as a privileged app to ${HOST_NAME}..."
    run_cmd_or_die adb -s "${HOST_SERIAL}" shell mkdir -p "${HOST_APK_DIR}"
    run_cmd_or_die adb -s "${HOST_SERIAL}" push "${OUT}/${HOST_APK_DIR}/VdmHost.apk" "${HOST_APK_DIR}"
    echo 'Copying privileged permissions...'
    run_cmd_or_die adb -s "${HOST_SERIAL}" push "${PERM_SRC}" "${PERM_DST}"
    echo 'Rebooting device...'
    run_cmd_or_die adb -s "${HOST_SERIAL}" reboot
    run_cmd_or_die adb -s "${HOST_SERIAL}" wait-for-device
  fi
fi

echo
echo 'Success!'
echo
