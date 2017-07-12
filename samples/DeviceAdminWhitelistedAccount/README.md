# DeviceAdmin Whitelisted Account

This application creates an account that will *not* prevent test-only DO/PO from being activated.

## Build and install:

```
croot
mmma -j development/samples/DeviceAdminWhitelistedAccount
adb install -r -g $OUT/data/app/DeviceAdminWhitelistedAccount/DeviceAdminWhitelistedAccount.apk
```


## Create a whitelisted account

- Launch the "DA Whitelisted Account" app from the launcher.

## Remove a whitelisted account

- Just uninstall the app. i.e.

```
adb uninstall com.example.android.app.admin.whitelistedaccount
```
