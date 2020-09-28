# Tool for visualizing window manager traces

## Developing WinScope
When the trace is enabled, Window Manager and Surface Flinger capture and
save current state to a file at each point of interest.
`frameworks/base/core/proto/android/server/windowmanagertrace.proto`
and `frameworks/native/services/surfaceflinger/layerproto/layerstrace.proto`
contain the proto definitions for their internal states.

### Checking out code and setting up environment
* Install [Yarn](https://yarnpkg.com), a JS package manager
* [Download Android source](https://source.android.com/setup/build/downloading)
* Navigate to `development/tools/winscope`
* Run `yarn install`

### Building & testing changes
* Navigate to `development/tools/winscope`
* Run `yarn run dev`

### Building with internal extensions
Internal paths in vendor/ which are not available in AOSP must be replaced by
stub files. See getWaylandSafePath for an example
