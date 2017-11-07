# Source Deps Reviewer

## Synopsis

This is a tool for labeling dependencies with a web interface.

Basically, it greps the specified directory for the given pattern,
and let human reviewers label their dependencies, even code dependencies,
which are code segments that are highly related to the specific pattern.

## Installation and Dependencies

This tool depends on [codesearch](https://github.com/google/codesearch)
to generate regular expression index, please install them with:

```
$ go get github.com/google/codesearch/cmd/cindex
$ go get github.com/google/codesearch/cmd/csearch
```

This tool depends on several Python packages,

```
$ pip install -e .
```

To run functional test, please do

```
$ pip install -e .[dev]
```

Prism, a code syntax highlighter is used.
It can be found at https://github.com/PrismJS/prism

## Usage

Initialize a project:

```
sourcedr init --source-dir [android-src] [project-dir]
```

Scan the codebase:

```
sourcedr scan
```

If there are occurrences that are not reviewed, then review the occurrences
with:

```
sourcedr review
```

Open browser and visit [http://localhost:5000](http://localhost:5000).


## Testing

```
$ python3 sourcedr/functional_tests.py
```
