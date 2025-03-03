## Build

m CustomLocale

## Install

### New install

```shell
adb root && adb remount && adb reboot
adb wait-for-device && adb root && adb remount

APP_PATH="/system/priv-app/CustomLocale/"
adb shell mkdir ${APP_PATH} || true
adb push ${OUT}${APP_PATH} ${APP_PATH}
adb reboot
```

### Reinstall

```shell
APP_PATH="/system/priv-app/CustomLocale/"
adb install -r ${OUT}${APP_PATH} ${APP_PATH}
```


## How to use

Just launch the activity, or use following ADB command.

```shell
adb shell am broadcast -a com.android.intent.action.SET_LOCALE \
   --es com.android.intent.extra.LOCALE ${TARGET_LOCALE} com.android.customlocale2
```

`${TARGET_LOCALE}` can be either 2 chars (e.g. `en`, `iw`)
or 5 chars (e.g. `en_US`, `iw_IL`)
