# Tool for visualizing window manager traces

## Developing WinScope
When the trace is enabled, Window Manager and Surface Flinger capture and
save current state to a file at each point of interest.
`frameworks/base/core/proto/android/server/windowmanagertrace.proto`
and `frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto`
contain the proto definitions for their internal states.

### Checking out code and setting up environment
* [Download Android source](https://source.android.com/setup/build/downloading)
* Navigate to `development/tools/winscope`
* Run `npm install`

### Build & test & deploy changes
* Navigate to `development/tools/winscope`
* Run `npm run` to get the list of available commands

### Update IntDefMapping
* Build `framework-minus-apex-intdefs` module and a preprocessor will
  generate the latest IntDefMapping. From the `ANDROID_ROOT` run:
```
. build/envsetup.sh
m framework-minus-apex-intdefs
```

* Copy the generated `intDefMapping.json` files to the `winscope`.
```
python3 -c 'import sys,json,collections; print(json.dumps(collections.OrderedDict(sorted(collections.ChainMap(*map(lambda x:json.load(open(x)), sys.argv[1:])).items())), indent=2))' $(find out/soong/.intermediates/frameworks/base -iname intDefMapping.json) > ./development/tools/winscope/src/common/intDefMapping.json
```

* Upload the changes.
```
repo start intdef-update
git commit -am "Update intdef mapping" "Test: N/A"
repo upload --cbr .
```

### Building with internal extensions
Internal paths in vendor/ which are not available in AOSP must be replaced by
stub files. See getWaylandSafePath for an example
