# Repo Diff Trees

repo_diff_trees.py compares two repo source trees and outputs reports on the
findings.

The ouput is in CSV and is easily consumable in a spreadsheet.

In addition to importing to a spreadsheet, you can also create your own
Data Studio dashboard like [this one](https://datastudio.google.com/open/0Bz6OwjyDcWYDbDJoQWtmRl8telU).

If you wish to create your own dashboard follow the instructions below:

1. Sync the two repo workspaces you wish to compare. Example:

```
mkdir android-8.0.0_r1
cd android-8.0.0_r1
repo init \
  --manifest-url=https://android.googlesource.com/platform/manifest \
  --manifest-branch=android-8.0.0_r1
# Adjust the number of parallel jobs to your needs
repo sync --current-branch --no-clone-bundle --no-tags --jobs=8
cd ..
mkdir android-8.0.0_r11
cd android-8.0.0_r11
repo init \
  --manifest-url=https://android.googlesource.com/platform/manifest \
  --manifest-branch=android-8.0.0_r11
# Adjust the number of parallel jobs to your needs
repo sync --current-branch --no-clone-bundle --no-tags --jobs=8
cd ..
```

2. Run repo_diff_trees.py. Example:

```
python repo_diff_trees.py --exclusions_file=android_exclusions.txt \
  android-8.0.0_r1 android-8.0.0_r11
```

3. Create a [new Google spreadsheet](https://docs.google.com/spreadsheets/create).
4. Import projects.csv to a new sheet.
5. Create a [new data source in Data Studio](https://datastudio.google.com/datasources/create).
6. Connect your new data source to the project.csv sheet in the Google spreadsheet.
7. Add a "Count Diff Status" field by selecting the menu next to the "Diff
   Status" field and selecting "Count".
8. Copy the [Data Studio dashboard sample](https://datastudio.google.com/open/0Bz6OwjyDcWYDbDJoQWtmRl8telU).
    Make sure you are logged into your Google account and you have agreed to Data Studio's terms of service. Once
    this is done you should get a link to "Make a copy of this report".
9. Select your own data source for your copy of the dashboard when prompted.
10. You may see a "Configuration Incomplete" message under
    the "Modified Projects" pie chart. To address this select the pie chart,
    then replace the "Invalid Metric" field for "Count Diff Status".

## Analysis method

repo_diff_trees.py goes through several stages when comparing two repo
source trees:

1. Match projects in source tree A with projects in source tree B.
2. Diff projects that have a match.
3. Find commits in source tree B that are not in source tree A.

The first two steps are self explanatory. The method
of finding commits only in B is explaned below.

## Finding commits not upstream

After matching up projects in both source tree
and diffing, the last stage is to iterate
through each project matching pair and find
the commits that exist in the downstream project (B) but not the
upstream project (A).

'git cherry' is a useful tool that finds changes
which exist in one branch but not another. It does so by
not only by finding which commits that were merged
to both branches, but also by matching cherry picked
commits.

However, there are many instances where a change in one branch
can have an equivalent in another branch without being a merge
or a cherry pick. Some examples are:

* Commits that were squashed with other commits
* Commits that were reauthored

Cherry pick will not recognize these commits as having an equivalent
yet they clearly do.

This is addressed in two steps:

1. First listing the "git cherry" commits that will give us the
   list of changes for which "git cherry" could not find an equivalent.
2. Then we "git blame" the entire project's source tree and compile
   a list of changes that actually have lines of code in the tree.
3. Finally we find the intersection: 'git cherry' changes
   that have lines of code in the final source tree.


## Caveats

The method described above has proven effective on Android
source trees. It does have shortcomings.

* It does not find commits that only delete lines of code.
* It does take into accounts merge conflict resolutions.
