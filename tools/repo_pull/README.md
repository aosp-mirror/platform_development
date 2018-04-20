Repo Pull
=========

`repo_pull.py` pulls multiple change lists from a Gerrit code review website.
A user may specify a query string and `repo_pull.py` will pull the matching
change lists.

For example, to pull the `repo-pull-initial-cl` topic from AOSP, run this
command:

    repo_pull.py pull 'topic:repo-pull-initial-cl' \
        -g https://android-review.googlesource.com

Read [Usage](#Usages) and [Examples](#Examples) for more details.


## Installation

`repo_pull.py` requires `.gitcookies` to access Gerrit APIs.  Please
check whether the Gerrit Code Review URL is in `~/.gitcookies`.

If you don't have an entry, follow these steps:

1. Visit the [Gerrit Code Review](https://android-review.googlesource.com).

2. Click [Settings -> HTTP Credentials](https://android-review.googlesource.com/settings/#HTTPCredentials)

3. Click **Obtain password**

4. Copy the highlighted shell commands and paste them in a terminal.

Note: You must repeat these for each Gerrit Code Review websites.


## Usages

Command line usages:

    $ repo_pull.py [sub-command] [query] \
                   [-g gerrit] \
                   [-b local-topic-branch] \
                   [-j num-threads] \
                   [--limits max-num-changes]


Three sub-commands are supported:

* `repo_pull.py json` prints the change lists in the JSON file format.

* `repo_pull.py bash` prints the *bash commands* that can pull the change lists.

* `repo_pull.py pull` *pulls the change lists* immediately.


### Query String

`[query]` is the query string that can be entered to the Gerrit search box.

These are common queries:

* `topic:name`
* `hashtag:name`
* `branch:name`
* `project:name`
* `owner:name`
* `is:open` | `is:merged` | `is:abandoned`
* `message:text`


### Options

* `-g` or `--gerrit` specifies the URL of the Gerrit Code Review website.
  *(required)*

* `-b` or `--branch` specifies the local branch name that will be passed to
  `repo start`.

* `-j` or `--parallel` specifies the number of parallel threads while pulling
  change lists.

* `-n` or `--limits` specifies the maximum number of change lists.  (default:
  1000)

* `-m` or `--merge` specifies the method to pick the merge commits.  (default:
  `merge-ff-only`)

* `-p` or `--pick` specifies the method to pick the non-merge commits.
  (default: `pick`)

  * `pick` maps to `git cherry-pick --allow-empty`
  * `merge` maps to `git merge --no-edit`
  * `merge-ff-only` maps to `git merge --no-edit --ff-only`
  * `merge-no-ff` maps to `git merge --no-edit --no-ff`
  * `reset` maps to `git reset --hard`
  * `checkout` maps to `git checkout`


## Examples

To print the change lists with the topic `repo-pull-initial-cl` in JSON file
format:

```
repo_pull.py json 'topic:repo-pull-initial-cl' \
    -g https://android-review.googlesource.com
```

To print the bash commands that can pull the change lists, use the `bash`
command:

```
repo_pull.py bash 'topic:repo-pull-initial-cl' \
    -g https://android-review.googlesource.com \
    -b my-local-topic-branch
```

To pull the change lists immediately, use the `pull` command:

```
repo_pull.py pull 'topic:repo-pull-initial-cl' \
    -g https://android-review.googlesource.com \
    -b my-local-topic-branch
```
