import { getSimplifiedLayerName } from "../src/utils/names";

const simplifications = {
  "WindowToken{38eae45 android.os.BinderProxy@398bebc}#0": "WindowToken",
  "7d8c460 NavigationBar0#0": "NavigationBar0#0",
  "Surface(name=d2965b1 NavigationBar0)/@0xe4380b2 - animation-leash#2": "Surface - animation-leash#2",
  "com.breel.wallpapers19.doodle.wallpaper.variations.DoodleWallpaperV1#0": "DoodleWallpaperV1#0",
  "ActivityRecord{825ebe6 u0 com.google.android.apps.nexuslauncher/.NexusLauncherActivity#0": "ActivityRecord",
  "com.google.android.apps.nexuslauncher/com.google.android.apps.nexuslauncher.NexusLauncherActivity#0": "NexusLauncherActivity#0",
  "com.android.settings/com.android.settings.Settings$UsbDetailsActivity#0": "Settings$UsbDetailsActivity#0",
  "7d8c460 com.google.android.calendar/com.google.android.calendar.AllInOneCalendarActivity#0": "AllInOneCalendarActivity#0",
  "WallpaperWindowToken{ad25afe token=android.os.Binder@8ab6b9}#0": "WallpaperWindowToken",
};

describe("getSimplifiedLayerName", () => {
  it("simplifies traces as expected", () => {
    for (const longName in simplifications) {
      const expectedSimplifiedName = simplifications[longName];
      const actualSimplifiedName = getSimplifiedLayerName(longName);

      expect(actualSimplifiedName).toBe(expectedSimplifiedName);
    }
  });
});