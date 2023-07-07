#!/usr/bin/python3
#
# Copyright (C) 2023 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

import xml.etree.ElementTree as ET
import json
import subprocess
import concurrent.futures
import requests
import os
import tempfile
import pprint
import sys


class BuildIdFinder:
    def __init__(self, branch="aosp-master", target="aosp_cf_x86_64_phone-userdebug", batch_size=100):
        local_branch = subprocess.getoutput(
            "cat .repo/manifests/default.xml | grep super | sed 's/.*revision=\\\"\(.*\)\\\".*/\\1/'").strip()
        text = subprocess.getoutput(
            "repo forall -c 'echo \\\"$REPO_PROJECT\\\": \\\"$(git log m/" + local_branch + " --format=format:%H -1)\\\",'")
        json_text = "{" + text[:-1] + "}"
        self.local = json.loads(json_text)
        self.branch = branch
        self.target = target
        self.batch_size = batch_size

    def __rating(self, bid, target):
        filename = "manifest_" + bid + ".xml"
        fetch_result = subprocess.run(["/google/data/ro/projects/android/fetch_artifact", "--bid", bid,
                                       "--target", target, filename], stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)
        if fetch_result.returncode != 0:
            raise Exception('no artifact yet')

        tree = ET.parse(filename)
        root = tree.getroot()

        remote = dict()
        for child in root:
            if child.tag == "project" and "revision" in child.attrib:
                remote[child.attrib["name"]] = child.attrib["revision"]

        common_key = self.local.keys() & remote.keys()
        os.remove(filename)

        return sum([self.local[key] != remote[key] for key in common_key])

    def batch(self, nextPageToken="", best_rating=None):
        result = dict()
        url = "https://androidbuildinternal.googleapis.com/android/internal/build/v3/buildIds/%s?buildIdSortingOrder=descending&buildType=submitted&maxResults=%d" % (
            self.branch, self.batch_size)
        if nextPageToken != "":
            url += "&pageToken=%s" % nextPageToken
        res = requests.get(url)
        bids = res.json()
        best_rating_in_batch = None
        with concurrent.futures.ThreadPoolExecutor(max_workers=20) as executor:
            futures = {executor.submit(
                self.__rating, bid_obj["buildId"], self.target): bid_obj["buildId"] for bid_obj in bids["buildIds"]}
            for future in concurrent.futures.as_completed(futures):
                try:
                    bid = futures[future]
                    different_prj_cnt = future.result()
                    if best_rating_in_batch is None:
                        best_rating_in_batch = different_prj_cnt
                    else:
                        best_rating_in_batch = min(
                            different_prj_cnt, best_rating_in_batch)

                except Exception as exc:
                    # Ignore..
                    pass
                else:
                    result[bid] = different_prj_cnt
                    if different_prj_cnt == 0:
                        return result
        if best_rating is not None:
            if best_rating < best_rating_in_batch:
                # We don't need to try it further.
                return result
        result.update(self.batch(
            nextPageToken=bids["nextPageToken"], best_rating=best_rating_in_batch))
        return result


def main():
    if len(sys.argv) == 1:
        bif = BuildIdFinder()
    elif len(sys.argv) == 3:
        bif = BuildIdFinder(branch=sys.argv[1], target=sys.argv[2])
    else:
        print("""
Run without arguments or two arguments(branch and target)
It uses aosp-master and aosp_cf_x86_64_phone-userdebug by default.

For example,
./development/multitree/find_build_id.py
./development/multitree/find_build_id.py aosp-master aosp_cf_x86_64_phone-userdebug
""")
        return

    result = bif.batch()
    best_rating = min(result.values())
    best_bids = {k for (k, v) in result.items() if v == best_rating}
    if best_rating == 0:
        print("%s is the bid to use %s in %s for your repository" %
              (best_bids, bif.target, bif.branch))
    else:
        print("""
Cannot find the perfect matched bid: There are 2 options
1. Choose a bid from the list below
  (bids: %s, count of different projects: %s)
2. repo sync
    """ % (best_bids, best_rating))


main()
