# Source Dr.

This is a collection of source tree analysis tools.

- [blueprint](blueprint) analyzes Android.bp and the dependencies between the
  modules.
- [ninja](ninja) analyzes `$(OUT)/combined-${target}.ninja`, which contains all
  file dependencies.
- [files/list_app_shared_uid.py](files/list_app_shared_uid.py) lists all
  pre-installed apps with `sharedUserId`.
