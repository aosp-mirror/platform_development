# Test data for `cargo_embargo`

The files here are used for `cargo_embargo` integration tests. Run the tests with

```shell
atest --host cargo_embargo.test
```

## Handling changes in Cargo output

When the output of `cargo metadata` changes, you need to update the
`cargo.metadata` files found in the subdirectories here. Do this with:

```
for crate in aho-corasick async-trait either plotters rustc-demangle-capi; do
    pushd $ANDROID_BUILD_TOP/external/rust/crates/$crate
    cargo metadata --format-version 1 | jq --sort-keys \
      > $ANDROID_BUILD_TOP/development/tools/cargo_embargo/testdata/$crate/cargo.metadata
    popd
done
```

Run the integration tests again after updating the crate metadata.

Some tests will likely fail because of outdated information in the other test
files:

- `expected_Android.bp`: Adjust the version numbers to match the current version
  from `cargo.metadata`.
- `crates.json`: Adjust version numbers and `package_dir` as necessary.
- `cargo_embargo.json`: Adjust the list of Cargo features if this has changed
  since the file was last touched.
