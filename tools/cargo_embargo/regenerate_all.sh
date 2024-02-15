#!/bin/bash
#
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

# Helper script to run cargo_embargo generate on all external Rust crates and generate an HTML
# report of the results, including any errors, warnings or changes to Android.bp files.
#
# Should be run from under external/rust/crates.

set -e

report="cargo_embargo_report.html"

cat > $report <<END
<html>
<head>
<title>cargo_embargo crate report</title>
<style type="text/css">
td { vertical-align: top; }
.success { color: green; }
.skipped { color: yellow; }
.warning { color: orange; }
.error { color: red; }
</style>
</head>
<body>
<h1>cargo_embargo crate report</h1>
<h2>Using existing cargo_embargo.json</h2>
<table>
<tr><th>Crate name</th><th>Generate</th><th>Details</th><th style="width: 25%;">Files</th></tr>
END

success_count=0
different_count=0
total_count=0
for config in */cargo_embargo.json; do
  ((total_count+=1))
  crate=$(dirname $config)
  echo "Trying $crate..."
  echo "<tr><td><code>$crate</code></td>" >> $report
  if (cd $crate && cargo_embargo generate cargo_embargo.json) 2> cargo_embargo.err; then
    (cd $crate && git diff Android.bp > Android.bp.diff)
    if grep "WARNING" cargo_embargo.err; then
      echo '<td class="error">Warning</td>' >> $report
      echo '<td><details><summary>' >> $report
      grep -m 1 "WARNING" cargo_embargo.err >> $report
      echo '</summary>' >> $report
      sed 's/$/<br\/>/g' < cargo_embargo.err >> $report
      echo '</details></td>' >> $report
    else
      # Compare the checked-in Android.bp to the generated one.
      (cd $crate && git show HEAD:Android.bp > Android.bp.orig)
      if diff $crate/Android.bp.orig $crate/Android.bp > /dev/null; then
        echo '<td class="success">Success</td>' >> $report
        ((success_count+=1))
      else
        echo '<td class="warning">Different</td>' >> $report
        ((different_count+=1))
      fi

      echo '<td>' >> $report
      if [[ -s "cargo_embargo.err" ]]; then
        echo '<details>' >> $report
        sed 's/$/<br\/>/g' < cargo_embargo.err >> $report
        echo '</details>' >> $report
      fi
      echo '</td>' >> $report
    fi
  else
    echo '<td class="error">Error</td>' >> $report
    echo '<td><details open>' >> $report
    sed 's/$/<br\/>/g' < cargo_embargo.err >> $report
    echo '</details></td>' >> $report
  fi

  rm cargo_embargo.err
  rm -rf "$crate/cargo.metadata" "$crate/cargo.out" "$crate/target.tmp" "$crate/Cargo.lock" "$crate/Android.bp.orig" "$crate/Android.bp.embargo" "$crate/Android.bp.embargo_nobuild"
  (cd $crate && git checkout Android.bp)

  echo '<td>' >> $report
  if [[ -s "$crate/Android.bp.diff" ]]; then
    echo '<details><summary>Android.bp.diff</summary><pre>' >> $report
    cat "$crate/Android.bp.diff" >> $report
    echo '</pre></details>' >> $report
    rm "$crate/Android.bp.diff"
  fi
  echo '</td></tr>' >> $report
done

echo '</table>' >> $report
echo "<p>$success_count success, $different_count different, $total_count total.</p>" >> $report
echo '</body>' >> $report
echo '</html>' >> $report

echo "Open file://$PWD/$report for details"
